package teaselib.core.ui;

import static java.util.stream.Collectors.toList;
import static teaselib.core.speechrecognition.srgs.PhraseString.words;
import static teaselib.core.util.ExceptionUtil.asRuntimeException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiFunction;
import java.util.function.IntUnaryOperator;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactoryConfigurationError;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import teaselib.core.events.Event;
import teaselib.core.speechrecognition.AudioSignalProblems;
import teaselib.core.speechrecognition.Confidence;
import teaselib.core.speechrecognition.Rule;
import teaselib.core.speechrecognition.RuleIndicesList;
import teaselib.core.speechrecognition.SpeechRecognition;
import teaselib.core.speechrecognition.SpeechRecognitionChoices;
import teaselib.core.speechrecognition.SpeechRecognitionEvents;
import teaselib.core.speechrecognition.SpeechRecognitionImplementation;
import teaselib.core.speechrecognition.SpeechRecognitionParameters;
import teaselib.core.speechrecognition.SpeechRecognitionSRGS;
import teaselib.core.speechrecognition.SpeechRecognizer;
import teaselib.core.speechrecognition.events.AudioLevelUpdatedEventArgs;
import teaselib.core.speechrecognition.events.AudioSignalProblemOccuredEventArgs;
import teaselib.core.speechrecognition.events.SpeechRecognitionStartedEventArgs;
import teaselib.core.speechrecognition.events.SpeechRecognizedEventArgs;
import teaselib.core.speechrecognition.implementation.TeaseLibSRGS;
import teaselib.core.speechrecognition.implementation.Unsupported;
import teaselib.core.speechrecognition.srgs.PhraseString;
import teaselib.core.speechrecognition.srgs.SRGSPhraseBuilder;
import teaselib.core.speechrecognition.srgs.SlicedPhrases;
import teaselib.core.ui.Prompt.Result;

/**
 * @author Citizen-Cane
 *
 */
public class SpeechRecognitionInputMethod implements InputMethod, teaselib.core.Closeable {
    private static final Logger logger = LoggerFactory.getLogger(SpeechRecognitionInputMethod.class);

    private static final double AUDIO_PROBLEM_PENALTY_WEIGHT = 0.005;

    public enum Notification implements InputMethod.Notification {
        RecognitionRejected
    }

    private final SpeechRecognizer speechRecognizer;
    private final Map<Locale, SpeechRecognition> usedRecognitionInstances = new HashMap<>();
    private final AudioSignalProblems audioSignalProblems;
    public final SpeechRecognitionEvents events;

    private final Event<SpeechRecognitionStartedEventArgs> speechRecognitionStartedEventHandler;
    private final Event<AudioLevelUpdatedEventArgs> audioLevelUpdatedEventHandler;
    private final Event<AudioSignalProblemOccuredEventArgs> audioSignalProblemEventHandler;
    private final Event<SpeechRecognizedEventArgs> speechDetectedEventHandler;
    private final Event<SpeechRecognizedEventArgs> recognitionRejected;
    private final Event<SpeechRecognizedEventArgs> recognitionCompleted;

    private final AtomicReference<Prompt> active = new AtomicReference<>();
    private Rule hypothesis = null;;
    private float awarenessBonus = 0.0f;

    public SpeechRecognitionInputMethod(SpeechRecognizer speechRecognizers) {
        this.speechRecognizer = speechRecognizers;
        this.audioSignalProblems = new AudioSignalProblems();
        this.events = new SpeechRecognitionEvents();

        this.speechRecognitionStartedEventHandler = this::handleRecognitionStarted;
        this.audioLevelUpdatedEventHandler = this::handleAudioLevelUpdated;
        this.audioSignalProblemEventHandler = this::handleAudioSignalProblemDetected;
        this.speechDetectedEventHandler = this::handleSpeechDetected;
        this.recognitionRejected = this::handleRecognitionRejected;
        this.recognitionCompleted = this::handleRecogntionCompleted;
    }

    private void handleRecognitionStarted(SpeechRecognitionStartedEventArgs eventArgs) {
        active.updateAndGet(prompt -> {
            clearDetectedSpeech();
            events.recognitionStarted.fire(eventArgs);
            return prompt;
        });
    }

    private void clearDetectedSpeech() {
        audioSignalProblems.clear();
        hypothesis = null;
    }

    private void handleAudioLevelUpdated(AudioLevelUpdatedEventArgs audioLevelUpdatedEventArgs) {
        active.updateAndGet(prompt -> {
            events.audioLevelUpdated.fire(audioLevelUpdatedEventArgs);
            return prompt;
        });
    }

    private void handleAudioSignalProblemDetected(
            AudioSignalProblemOccuredEventArgs audioSignalProblemOccuredEventArgs) {
        active.updateAndGet(prompt -> {
            audioSignalProblems.add(audioSignalProblemOccuredEventArgs.problem);
            events.audioSignalProblemOccured.fire(audioSignalProblemOccuredEventArgs);
            return prompt;
        });
    }

    private void handleSpeechDetected(SpeechRecognizedEventArgs eventArgs) {
        active.updateAndGet(prompt -> {
            SpeechRecognition recognizer = getRecognizer(prompt);
            if (audioSignalProblems.exceedLimits() && recognizer.audioSync.speechRecognitionInProgress()) {
                logTooManyAudioSignalProblems(eventArgs.result);
                recognizer.restartRecognition();
            } else if (recognizer.implementation instanceof TeaseLibSRGS) {
                if (prompt.acceptedResult == Result.Accept.Distinct) {
                    buildDistinctHypothesis(eventArgs, prompt);
                } else if (prompt.acceptedResult == Result.Accept.Multiple) {
                    // Ignore
                } else {
                    throw new UnsupportedOperationException(prompt.acceptedResult.toString());
                }
            }
            return prompt;
        });
    }

    private void buildDistinctHypothesis(SpeechRecognizedEventArgs eventArgs, Prompt prompt) {
        // Caveats:

        // DONE
        // - trailing null rules make rule undefined (distinct [1], common [0,1], distinct NULL [0]
        // -> remove trailing null rules because the available rules already define a distinct result
        // + trailing null rules are at the end of the text (fromElement = size)

        // DONE
        // For single rules or long rules, confidence decreases with length
        // -> forward earlier accepted hypothesis - this is already handled by the existing code
        // + for single confirmations, confidence is normal anyway
        // - longer rules within multiple choices may cause recognition problems
        // -> remember earlier hypothesis

        // DONE
        // words are omitted in the main rule, instead there are NULL child rules
        // Example: "Ja Mit <NULL> dicken Dildo Frau Streng" -> "Ja Mit "DEM" dicken Dildo Frau Streng" ->
        // matches
        // phrase
        // -> for each NULL rule try to reconstruct the phrase by replacing the NULL rule with a valid phrase
        // + if it matches a phrase, use that one (works for speechDetected as well as for
        // recognitionCompleted/Rejected
        // - the phrase should match the beginning of a single phrase - can rate incomplete phrases -> not needed

        // DONE
        // in the logs aborting recognition on audio problems looks suspicious in the log - multiple recognitions?
        // -> only check for audio problems when rejecting speech - should be okay if recognized
        // + make it as optional as possible

        // RESOLVED by confidence function
        // first child rule may have very low confidence value, second very high, how to measure minimal length
        // -> number of distinct rules, words, or vowels -> words or vowels

        // RESOLVED by confidence function
        // - confidence decreases at the end of the hypothesis (distinct [1] = 0.79, common [0,1] = 0.76,
        // distinct
        // NULL [0]= 0.58;
        // -> cut off hypothesis when probability falls below accepted threshold, or when average falls below
        // threshold
        //
        // unrealized NULL rules seem to have confidence == 1.0 -> all NULL rules have C=1.0

        // DONE
        // map phrase to choice indices before evaluating best phrase
        // -> better match on large phrase sets, supports hypothesis as well

        // TODO only keep hypothesis that extends
        // + keep all hypothesis results
        // + keep those who continue in the next event
        // + copy over confidence from previous hypothesis

        List<Rule> candidates = new ArrayList<>(eventArgs.result.length);
        for (Rule rule : eventArgs.result) {
            rule.removeTrailingNullRules();
            if (!rule.text.isBlank()) {
                // TODO attempt to match beginning of existing phrase
                // TODO matching rule indices during repair must map to choices
                // -> avoids wrong repair in
                // teaselib.core.speechrecognition.SpeechRecognitionComplexTest.testSRGSBuilderMultipleChoicesAlternativePhrases()
                List<Rule> repaired = rule.repair(getRecognizer(prompt).getChoices().slicedPhrases);
                if (repaired.isEmpty()) {
                    candidates.add(rule);
                } else {
                    candidates.addAll(repaired);
                }
            }
        }

        validate(candidates);

        Optional<Rule> result = bestSingleResult(candidates.stream(), toChoices(prompt));
        if (result.isPresent()) {
            Rule rule = result.get();
            double expectedConfidence = expectedConfidence(prompt, rule, awarenessBonus);
            if (weightedProbability(prompt, rule) >= expectedConfidence) {
                if (hypothesis != null && hypothesis.indices.containsAll(rule.indices)) {
                    float hypothesisProbability = weightedProbability(prompt, hypothesis);
                    float h = hypothesis.children.size();
                    float r = rule.children.size();
                    float average = (hypothesisProbability * h + rule.probability * r) / (h + r);
                    setHypothesis(rule, Math.max(average, rule.probability));
                } else {
                    setHypothesis(rule, weightedProbability(prompt, rule));
                }
                logger.info("Considering as hypothesis");
                events.speechDetected.fire(new SpeechRecognizedEventArgs(hypothesis));
            }
        }
    }

    private float weightedProbability(Prompt prompt, Rule rule) {
        SlicedPhrases<PhraseString> slicedPhrases = getRecognizer(prompt).getChoices().slicedPhrases;
        if (slicedPhrases != null) {
            // TODO use sequence length of detected speech, 0 if no match, ignore trailing null rules
            return rule.probability * rule.indices().size() / slicedPhrases.size();
        } else {
            return 1.0f;
        }
    }

    private void setHypothesis(Rule rule, float probability) {
        hypothesis = new Rule(rule, probability, rule.confidence);
        hypothesis.children.addAll(rule.children);
    }

    private static void validate(List<Rule> candidates) {
        candidates.stream().forEach(Rule::isValid);
    }

    private static double expectedConfidence(Prompt prompt, Rule rule, float awarenessBonus) {
        float weighted = confidence(prompt.choices.intention).weighted(words(rule.text).length, 2.0f);
        float expectedCOnfidence = weighted - awarenessBonus;
        if (awarenessBonus > 0.0f) {
            logger.info("Weighted confidence {} - Awareness bonus {} =  expected confidence {}", weighted,
                    awarenessBonus, expectedCOnfidence);
        }
        return expectedCOnfidence;
    }

    private void handleRecognitionRejected(SpeechRecognizedEventArgs eventArgs) {
        active.updateAndGet(prompt -> {
            try {
                if (audioSignalProblems.exceedLimits()) {
                    logTooManyAudioSignalProblems(eventArgs.result);
                    events.recognitionRejected.fire(eventArgs);
                    return prompt;
                } else {
                    if (hypothesis != null
                            && hypothesis.probability >= expectedConfidence(prompt, hypothesis, awarenessBonus)) {
                        logger.info("Considering hypothesis");
                        // rejectedResult may contain better result than hypothesis
                        // TODO Are last speech detection and recognitionRejected result the same?
                        // TODO accept only if hypothesis and recognitionRejected result have the same indices
                        return handle(prompt, this::singleResult, hypothesis);
                    } else {
                        events.recognitionRejected.fire(eventArgs);
                        signalHandlerInvocation(Notification.RecognitionRejected, eventArgs);
                        return prompt;
                    }
                }
            } finally {
                clearDetectedSpeech();
            }
        });
    }

    private void handleRecogntionCompleted(SpeechRecognizedEventArgs eventArgs) {
        active.updateAndGet(prompt -> {
            try {
                if (audioSignalProblems.exceedLimits()) {
                    logTooManyAudioSignalProblems(eventArgs.result);
                    return prompt;
                } else {
                    try {
                        if (prompt.acceptedResult == Result.Accept.Distinct) {
                            return handle(eventArgs, prompt, SpeechRecognitionInputMethod::bestSingleResult,
                                    this::singleResult);
                        } else if (prompt.acceptedResult == Result.Accept.Multiple) {
                            return handle(eventArgs, prompt, SpeechRecognitionInputMethod::bestMultipleChoices,
                                    this::multipleChoiceResults);
                        } else {
                            throw new UnsupportedOperationException(prompt.acceptedResult.toString());
                        }
                    } catch (Exception e) {
                        prompt.setException(e);
                        getRecognizer(prompt.choices.locale).endRecognition();
                        return null;
                    }
                }
            } finally {
                clearDetectedSpeech();
            }
        });
    }

    private Prompt.Result singleResult(Rule rule, IntUnaryOperator toChoices) {
        Set<Integer> choices = choices(rule, toChoices);
        if (choices.isEmpty()) {
            throw new NoSuchElementException("No choice indices: " + rule);
        } else if (choices.size() > 1) {
            throw new NoSuchElementException("No distinct choice indices: " + rule);
        }
        Integer distinctChoice = choices.iterator().next();
        return new Prompt.Result(distinctChoice);
    }

    private Prompt handle(SpeechRecognizedEventArgs eventArgs, Prompt prompt,
            BiFunction<Stream<Rule>, IntUnaryOperator, Optional<Rule>> matcher,
            BiFunction<Rule, IntUnaryOperator, Prompt.Result> resultor) {
        validate(Arrays.asList(eventArgs.result));

        Optional<Rule> result = matcher.apply(Arrays.stream(eventArgs.result), toChoices(prompt));
        if (result.isPresent() && hypothesis != null && hypothesis.probability > result.get().probability) {
            return handle(prompt, resultor, hypothesis);
        } else if (result.isPresent()) {
            return handle(prompt, resultor, result.get());
        } else {
            reject(eventArgs, "No matching choice");
            return prompt;
        }
    }

    private Prompt handle(Prompt prompt, BiFunction<Rule, IntUnaryOperator, Prompt.Result> resultor, Rule rule) {
        double expectedConfidence = expectedConfidence(prompt, rule, awarenessBonus);
        double audioProblemPenalty = audioSignalProblems.penalty() * AUDIO_PROBLEM_PENALTY_WEIGHT;
        // TODO Weighted probability should be calculated in bestSingleResult
        float ruleProbability = weightedProbability(prompt, rule);
        if (ruleProbability - audioProblemPenalty >= expectedConfidence) {
            Prompt.Result promptResult = resultor.apply(rule, toChoices(prompt));
            if (promptResult.elements.isEmpty()) {
                reject(prompt, rule, "Empty result");
                return prompt;
            } else if (promptResult.valid(prompt.choices)) {
                events.recognitionCompleted.fire(new SpeechRecognizedEventArgs(rule));
                accept(prompt, promptResult);
                return null;
            } else {
                reject(prompt, rule, "Undefined result index");
                return prompt;
            }
        } else if (ruleProbability >= expectedConfidence) {
            logAudioSignalProblemPenalty(expectedConfidence, audioProblemPenalty);
            reject(rule);
            return prompt;
        } else {
            logLackOfConfidence(expectedConfidence);
            reject(rule);
            return prompt;
        }
    }

    private void reject(Prompt prompt, Rule rule, String reason) {
        logger.info("{} in {} : {} - rejecting", reason, prompt.choices, rule);
        reject(rule);
    }

    private void reject(SpeechRecognizedEventArgs eventArgs, String reason) {
        logger.info("{} in {} - rejecting", reason, eventArgs);
        events.recognitionRejected.fire(eventArgs);
    }

    private void reject(Rule rule) {
        events.recognitionRejected.fire(new SpeechRecognizedEventArgs(rule));
    }

    private IntUnaryOperator toChoices(Prompt prompt) {
        return getRecognizer(prompt).getChoices().mapper;
    }

    private Prompt.Result multipleChoiceResults(Rule rule, @SuppressWarnings("unused") IntUnaryOperator toChoices) {
        RuleIndicesList choices = rule.indices();
        List<Integer> multipleChoices = choices.stream().map(indices -> indices.size() == 1 ? indices.iterator().next()
                : Prompt.Result.UNDEFINED.elements.iterator().next()).collect(toList());
        return new Prompt.Result(multipleChoices);
    }

    public static Optional<Rule> bestSingleResult(Stream<Rule> stream, IntUnaryOperator toChoices) {
        return stream.filter(rule -> {
            Set<Integer> choices = choices(rule.indices(), toChoices);
            return choices.size() == 1;
        }).reduce(Rule::maxProbability);
    }

    public static Set<Integer> choices(Rule rule, IntUnaryOperator toChoices) {
        return choices(rule.indices(), toChoices);
    }

    public static Set<Integer> choices(RuleIndicesList indices, IntUnaryOperator toChoices) {
        return indices.intersection().stream().map(toChoices::applyAsInt).collect(Collectors.toSet());
    }

    public static Optional<Rule> bestMultipleChoices(Stream<Rule> stream,
            @SuppressWarnings("unused") IntUnaryOperator toChoices) {
        return stream.reduce(Rule::maxProbability);
    }

    private SpeechRecognition getRecognizer(Prompt prompt) {
        return getRecognizer(prompt.choices.locale);
    }

    private SpeechRecognition getRecognizer(Locale locale) {
        SpeechRecognition recognizer = speechRecognizer.get(locale);
        if (usedRecognitionInstances.put(recognizer.locale, recognizer) == null) {
            addEvents(recognizer);
        }
        return recognizer;
    }

    private Prompt accept(Prompt prompt, Prompt.Result promptResult) {
        getRecognizer(prompt.choices.locale).endRecognition();
        signal(prompt, promptResult);
        return null;
    }

    private static void logLackOfConfidence(double expectedConfidence) {
        logger.info("Lack of confidence (expected {})", expectedConfidence);
    }

    private void logTooManyAudioSignalProblems(Rule[] result) {
        logger.info("Audio signal problems: {}", audioSignalProblems);
    }

    private static void logAudioSignalProblemPenalty(double confidence, double penalty) {
        logger.info("Audio signal problem penalty (required {} + penalty {} + = {})", confidence, penalty,
                confidence + penalty);
    }

    private void signal(Prompt prompt, Prompt.Result result) {
        prompt.lock.lock();
        try {
            prompt.signalResult(this, result);
        } finally {
            prompt.lock.unlock();
        }
    }

    private void signalHandlerInvocation(InputMethod.Notification eventType, SpeechRecognizedEventArgs eventArgs) {
        Prompt prompt = active.get();
        if (prompt != null) {
            prompt.lock.lock();
            try {
                prompt.signalHandlerInvocation(new SpeechRecognitionInputMethodEventArgs(eventType, eventArgs));
            } finally {
                prompt.lock.unlock();
            }
        }
    }

    @Override
    public Setup getSetup(Choices choices) {
        SpeechRecognition recognizer = getRecognizer(choices.locale);
        SpeechRecognitionImplementation implementation = recognizer.implementation;
        if (implementation instanceof SpeechRecognitionSRGS) {
            return srgsPhraseBuilder(choices, recognizer);
        } else if (implementation instanceof SpeechRecognitionChoices) {
            return simplePhraseBuilder(choices, recognizer);
        } else if (implementation instanceof Unsupported) {
            return () -> { //
            };
        } else {
            throw new UnsupportedOperationException();
        }
    }

    private static Setup srgsPhraseBuilder(Choices choices, SpeechRecognition recognizer)
            throws TransformerFactoryConfigurationError {
        try {
            SRGSPhraseBuilder builder = new SRGSPhraseBuilder(choices, recognizer.implementation.getLanguageCode());
            if (logger.isInfoEnabled()) {
                logger.info("{}", builder.slices);
                logger.info("{}", builder.toXML());
            }
            SpeechRecognitionParameters speechRecognitionParameters = new SpeechRecognitionParameters(builder);
            return () -> recognizer.setChoices(speechRecognitionParameters);
        } catch (ParserConfigurationException | TransformerException e) {
            throw asRuntimeException(e);
        }
    }

    private static Setup simplePhraseBuilder(Choices choices, SpeechRecognition recognizer) {
        SpeechRecognitionParameters parameters = new SpeechRecognitionParameters(choices);
        return () -> recognizer.setChoices(parameters);
    }

    @Override
    public void show(Prompt prompt) {
        Objects.requireNonNull(prompt);
        Prompt previousPrompt = active.getAndSet(prompt);
        if (previousPrompt != null) {
            throw new IllegalStateException("Trying to show prompt when already showing another");
        }
        prompt.inputMethodInitializers.setup(this);
        startSpeechRecognition(prompt);
    }

    @Override
    public boolean dismiss(Prompt prompt) throws InterruptedException {
        Prompt activePrompt = active.getAndSet(null);
        if (activePrompt == null) {
            return false;
        } else if (activePrompt != prompt) {
            throw new IllegalStateException("Trying to dismiss wrong prompt");
        } else {
            getRecognizer(prompt.choices.locale).endRecognition();
            return true;
        }
    }

    private void startSpeechRecognition(Prompt prompt) {
        SpeechRecognition recognizer = getRecognizer(prompt.choices.locale);
        if (recognizer.isActive()) {
            throw new IllegalStateException("Speech recognizer already active");
        }

        recognizer.startRecognition();
    }

    public static final class ResumeRecognition implements teaselib.core.Closeable {
        private final Runnable runnable;

        public ResumeRecognition(Runnable resumeRecognition) {
            this.runnable = resumeRecognition;
        }

        @Override
        public void close() {
            runnable.run();
        }

    }

    public ResumeRecognition pauseRecognition() {
        return new ResumeRecognition(speechRecognizer.pauseRecognition());
    }

    public void emulateRecogntion(String phrase) {
        Prompt activePrompt = active.get();
        if (activePrompt == null) {
            throw new NoSuchElementException("Active prompt");
        } else {
            getRecognizer(activePrompt).emulateRecogntion(phrase);
        }
    }

    private static Confidence confidence(Intention intention) {
        switch (intention) {
        case Chat:
            return Confidence.Low;
        case Confirm:
            return Confidence.Normal;
        case Decide:
            return Confidence.High;
        default:
            throw new IllegalArgumentException(intention.toString());
        }
    }

    private void addEvents(SpeechRecognition recognizer) {
        SpeechRecognitionEvents events = recognizer.events;
        events.recognitionStarted.add(speechRecognitionStartedEventHandler);
        events.audioLevelUpdated.add(audioLevelUpdatedEventHandler);
        events.audioSignalProblemOccured.add(audioSignalProblemEventHandler);
        events.speechDetected.add(speechDetectedEventHandler);
        events.recognitionRejected.add(recognitionRejected);
        events.recognitionCompleted.add(recognitionCompleted);
    }

    private void removeEvents(SpeechRecognition speechRecognizer) {
        SpeechRecognitionEvents events = speechRecognizer.events;
        events.recognitionStarted.remove(speechRecognitionStartedEventHandler);
        events.audioLevelUpdated.remove(audioLevelUpdatedEventHandler);
        events.audioSignalProblemOccured.remove(audioSignalProblemEventHandler);
        events.speechDetected.remove(speechDetectedEventHandler);
        events.recognitionRejected.remove(recognitionRejected);
        events.recognitionCompleted.remove(recognitionCompleted);
    }

    @Override
    public String toString() {
        Prompt prompt = active.get();
        String object = prompt != null ? getRecognizer(prompt.choices.locale).toString() : "<inactive>";
        String expectedConfidence = prompt != null ? confidence(prompt.choices.intention).toString() : "<?>";
        return "SpeechRecognizer=" + object + " confidence=" + expectedConfidence;
    }

    @Override
    public void close() {
        usedRecognitionInstances.values().stream().forEach(this::removeEvents);
    }

    public void setAwareness(boolean aware) {
        awarenessBonus = aware ? Confidence.High.probability - Confidence.Normal.reducedProbability() : 0.0f;
    }

}
