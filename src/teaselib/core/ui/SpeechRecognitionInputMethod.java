package teaselib.core.ui;

import static java.util.stream.Collectors.toList;
import static teaselib.core.util.ExceptionUtil.asRuntimeException;

import java.util.ArrayList;
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

import teaselib.core.AudioSync;
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

    private static final float AUDIO_PROBLEM_PENALTY_WEIGHT = 0.005f;

    public enum Notification implements InputMethod.Notification {
        RecognitionRejected
    }

    private final SpeechRecognizer speechRecognizer;
    private final Map<Locale, SpeechRecognition> usedRecognitionInstances = new HashMap<>();
    private final AudioSignalProblems audioSignalProblems;
    public final SpeechRecognitionEvents events;
    private final AudioSync audioSync;

    private final Event<SpeechRecognitionStartedEventArgs> speechRecognitionStartedEventHandler;
    private final Event<AudioLevelUpdatedEventArgs> audioLevelUpdatedEventHandler;
    private final Event<AudioSignalProblemOccuredEventArgs> audioSignalProblemEventHandler;
    private final Event<SpeechRecognizedEventArgs> speechDetectedEventHandler;
    private final Event<SpeechRecognizedEventArgs> recognitionRejected;
    private final Event<SpeechRecognizedEventArgs> recognitionCompleted;

    private final AtomicReference<Prompt> active = new AtomicReference<>();
    private SpeechRecognitionParameters speechRecognitionParameters = null;
    private Rule hypothesis = null;
    private float awarenessBonus = 0.0f;

    public SpeechRecognitionInputMethod(SpeechRecognizer speechRecognizer) {
        this.speechRecognizer = speechRecognizer;
        this.audioSignalProblems = new AudioSignalProblems();
        this.events = new SpeechRecognitionEvents();
        this.audioSync = speechRecognizer.audioSync;

        this.speechRecognitionStartedEventHandler = this::handleRecognitionStarted;
        this.audioLevelUpdatedEventHandler = this::handleAudioLevelUpdated;
        this.audioSignalProblemEventHandler = this::handleAudioSignalProblemDetected;
        this.speechDetectedEventHandler = this::handleSpeechDetected;
        this.recognitionRejected = this::handleRecognitionRejected;
        this.recognitionCompleted = this::handleRecogntionCompleted;
    }

    private void handleRecognitionStarted(SpeechRecognitionStartedEventArgs eventArgs) {
        active.updateAndGet(prompt -> {
            if (prompt == null) {
                return null;
            } else {
                try {
                    clearDetectedSpeech();
                    events.recognitionStarted.fire(eventArgs);
                    return prompt;
                } catch (Exception e) {
                    prompt.setException(e);
                    clearDetectedSpeech();
                    getRecognizer(prompt.choices.locale).endRecognition();
                    return null;
                }
            }
        });
    }

    private void clearDetectedSpeech() {
        audioSignalProblems.clear();
        hypothesis = null;
    }

    private void handleAudioLevelUpdated(AudioLevelUpdatedEventArgs audioLevelUpdatedEventArgs) {
        active.updateAndGet(prompt -> {
            try {
                events.audioLevelUpdated.fire(audioLevelUpdatedEventArgs);
                return prompt;
            } catch (Exception e) {
                prompt.setException(e);
                clearDetectedSpeech();
                getRecognizer(prompt.choices.locale).endRecognition();
                return null;
            }
        });
    }

    private void handleAudioSignalProblemDetected(
            AudioSignalProblemOccuredEventArgs audioSignalProblemOccuredEventArgs) {
        active.updateAndGet(prompt -> {
            try {
                audioSignalProblems.add(audioSignalProblemOccuredEventArgs.problem);
                events.audioSignalProblemOccured.fire(audioSignalProblemOccuredEventArgs);
                return prompt;
            } catch (Exception e) {
                prompt.setException(e);
                clearDetectedSpeech();
                getRecognizer(prompt.choices.locale).endRecognition();
                return null;
            }
        });
    }

    private void handleSpeechDetected(SpeechRecognizedEventArgs eventArgs) {
        active.updateAndGet(prompt -> {
            if (prompt == null) {
                return null;
            } else {
                try {
                    SpeechRecognition recognizer = getRecognizer(prompt);
                    if (audioSignalProblems.exceedLimits() && recognizer.audioSync.speechRecognitionInProgress()) {
                        logTooManyAudioSignalProblems();
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
                } catch (Exception e) {
                    prompt.setException(e);
                    clearDetectedSpeech();
                    getRecognizer(prompt.choices.locale).endRecognition();
                    return null;
                }
            }
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

        // DONE: rules and their children may not be consistent as SAPI replaces text in the matched rules
        // + however the rule index of such child rules is the right one - can be used to rebuild rule indices
        // -> If the children of the rule aren't distinct, no match

        // TODO: map rule indices back to choice indices when building srgs nodes
        // - much simpler then processing them here
        // - allows for optimizing rules, especially optional ones

        // TODO remember multiple hypotheses
        // + keep all hypothesis results
        // + keep those who continue in the next event
        // + copy over confidence from previous hypothesis

        List<Rule> candidates = new ArrayList<>(eventArgs.result.size());
        for (Rule rule : eventArgs.result) {
            if (!rule.text.isBlank()) {
                if (rule.hasTrailingNullRule()) {
                    rule = rule.withoutIgnoreableTrailingNullRules();
                }
                // TODO matching rule indices during repair must map to choices
                // -> avoids wrong repair in
                // teaselib.core.speechrecognition.SpeechRecognitionComplexTest.testSRGSBuilderMultipleChoicesAlternativePhrases()
                List<Rule> repaired = rule.repair(speechRecognitionParameters.slicedPhrases);
                if (repaired.isEmpty()) {
                    candidates.add(rule);
                } else {
                    candidates.addAll(repaired);
                }
            }
        }

        validate(candidates);

        Optional<Rule> result = bestSingleResult(candidates.stream(), toChoices());
        if (result.isPresent()) {
            Rule rule = result.get();
            if (rule.indices.equals(Rule.NoIndices)) {
                logger.info("Ignoring hypothesis {} since it contains results from multiple phrases", rule);
            } else {
                double expectedConfidence = expectedConfidence(prompt, rule, awarenessBonus);
                // Weighted probability is too low in tests for short hypotheses even with optimal probability of 1.0
                // This is intended because probability/confidence values of short short hypotheses cannot be trusted
                // -> confidence moves towards ground truth when more speech is detected
                float weightedProbability = weightedProbability(rule);
                if (weightedProbability >= expectedConfidence) {
                    if (hypothesis == null) {
                        setHypothesis(rule, weightedProbability);
                    } else if (hypothesis.indices.containsAll(rule.indices)) {
                        float hypothesisProbability = weightedProbability(hypothesis);
                        float h = hypothesis.children.size();
                        float r = rule.children.size();
                        float average = (hypothesisProbability * h + rule.probability * r) / (h + r);
                        setHypothesis(rule, Math.max(average, rule.probability));
                    }
                    logger.info("Considering as hypothesis");
                    events.speechDetected.fire(new SpeechRecognizedEventArgs(hypothesis));
                } else {
                    logger.info("Weighted confidence {} < expected confidence {} - hypothesis ignored", //
                            weightedProbability, expectedConfidence);
                }
            }
        }
    }

    private float weightedProbability(Rule rule) {
        if (rule.indices.equals(Rule.NoIndices))
            throw new IllegalArgumentException("Rule contains no indices - consider updating it after adding children");

        SlicedPhrases<PhraseString> slicedPhrases = speechRecognitionParameters.slicedPhrases;
        PhraseString text;
        List<PhraseString> complete;
        if (slicedPhrases != null) {
            text = new PhraseString(rule.text, rule.indices);
            complete = slicedPhrases.complete(text);
        } else {
            Integer index = rule.indices.iterator().next();
            text = new PhraseString(rule.text, index);
            complete = new PhraseString(speechRecognitionParameters.choices.get(index).phrases.get(0), index).words();
        }

        float weight = (float) text.words().size() / (float) complete.size();
        return rule.probability * weight;
    }

    private void setHypothesis(Rule rule, float probability) {
        hypothesis = new Rule(rule, Rule.HYPOTHESIS, probability, rule.confidence);
    }

    private static void validate(List<Rule> candidates) {
        candidates.stream().forEach(Rule::isValid);
    }

    private static float expectedConfidence(Prompt prompt, Rule rule, float awarenessBonus) {
        float weighted = confidence(prompt.choices.intention).weighted(PhraseString.words(rule.text).length);
        float expectedConfidence = weighted * awarenessBonus;
        if (awarenessBonus > 0.0f) {
            logger.info("Weighted confidence {} * Awareness bonus {} = expected confidence {}", weighted,
                    awarenessBonus, expectedConfidence);
        }
        return expectedConfidence;
    }

    private void handleRecognitionRejected(SpeechRecognizedEventArgs eventArgs) {
        active.updateAndGet(prompt -> {
            if (prompt == null) {
                return null;
            } else {
                try {
                    if (audioSignalProblems.exceedLimits()) {
                        logTooManyAudioSignalProblems();
                        reject(eventArgs);
                        return prompt;
                    } else {
                        if (hypothesis != null) {
                            // rejectedResult may contain better result than hypothesis
                            // TODO Are latest speech detection and recognitionRejected result the same?
                            // TODO accept only if hypothesis and recognitionRejected result have the same indices
                            float expectedConfidence = expectedConfidence(prompt, hypothesis, awarenessBonus);
                            if (hypothesis.probability >= expectedConfidence) {
                                logger.info("Considering hypothesis");
                                return handle(prompt, this::singleResult, hypothesis, expectedConfidence);
                            } else {
                                reject(eventArgs, "Ignoring hypothesis");
                                // Only if rejected by the speech recognition implementation
                                // - would result in too may events otherwise
                                signalHandlerInvocation(Notification.RecognitionRejected, eventArgs);
                                return prompt;
                            }
                        } else {
                            reject(eventArgs);
                            // Only if rejected by the speech recognition implementation - it's too may events otherwise
                            signalHandlerInvocation(Notification.RecognitionRejected, eventArgs);
                            return prompt;
                        }
                    }
                } catch (Exception e) {
                    prompt.setException(e);
                    getRecognizer(prompt.choices.locale).endRecognition();
                    return null;
                } finally {
                    clearDetectedSpeech();
                }
            }
        });
    }

    private void handleRecogntionCompleted(SpeechRecognizedEventArgs eventArgs) {
        active.updateAndGet(prompt -> {
            if (prompt == null) {
                return null;
            } else {
                try {
                    if (audioSignalProblems.exceedLimits()) {
                        logTooManyAudioSignalProblems();
                        return prompt;
                    } else {
                        if (prompt.acceptedResult == Result.Accept.Distinct) {
                            return handle(eventArgs, prompt, SpeechRecognitionInputMethod::bestSingleResult,
                                    this::singleResult);
                        } else if (prompt.acceptedResult == Result.Accept.Multiple) {
                            return handle(eventArgs, prompt, SpeechRecognitionInputMethod::bestMultipleChoices,
                                    this::multipleChoiceResults);
                        } else {
                            throw new UnsupportedOperationException(prompt.acceptedResult.toString());
                        }
                    }
                } catch (Exception e) {
                    prompt.setException(e);
                    getRecognizer(prompt.choices.locale).endRecognition();
                    return null;
                } finally {
                    clearDetectedSpeech();
                }
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
        validate(eventArgs.result);

        Optional<Rule> result = matcher.apply(eventArgs.result.stream(), toChoices());
        if (result.isPresent()) {
            Rule rule = result.get();
            if (hypothesis != null && hypothesis.probability > rule.probability
                    && resultor.apply(hypothesis, toChoices()).equals(resultor.apply(rule, toChoices()))) {
                float expectedConfidence = expectedConfidence(prompt, rule, awarenessBonus);
                return handle(prompt, resultor, hypothesis, expectedConfidence);
            } else {
                float expectedConfidence = expectedConfidence(prompt, rule, awarenessBonus);
                return handle(prompt, resultor, rule, expectedConfidence);
            }
        } else {
            reject(eventArgs, "No matching choice");
            return prompt;
        }
    }

    private Prompt handle(Prompt prompt, BiFunction<Rule, IntUnaryOperator, Prompt.Result> resultor, Rule rule,
            float expectedConfidence) {
        float audioProblemPenalty = audioSignalProblems.penalty() * AUDIO_PROBLEM_PENALTY_WEIGHT;

        if (rule.probability - audioProblemPenalty >= expectedConfidence) {
            Prompt.Result promptResult = resultor.apply(rule, toChoices());
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
        } else if (rule.probability >= expectedConfidence) {
            logAudioSignalProblemPenalty(expectedConfidence, audioProblemPenalty);
            reject(rule);
            return prompt;
        } else {
            logLackOfConfidence(rule.probability, expectedConfidence);
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
        reject(eventArgs);
    }

    private void reject(Rule rule) {
        reject(new SpeechRecognizedEventArgs(rule));
    }

    private void reject(SpeechRecognizedEventArgs eventArgs) {
        Optional<Rule> bestSingleResult = bestSingleResult(eventArgs.result.stream(), toChoices());
        if (bestSingleResult.isPresent()) {
            events.recognitionRejected.fire(new SpeechRecognizedEventArgs(bestSingleResult.get()));
        } else {
            events.recognitionRejected.fire(eventArgs);
        }
    }

    private IntUnaryOperator toChoices() {
        return speechRecognitionParameters.mapper;
    }

    private Prompt.Result multipleChoiceResults(Rule rule, @SuppressWarnings("unused") IntUnaryOperator toChoices) {
        RuleIndicesList choices = rule.indices();
        List<Integer> multipleChoices = choices.stream().filter(indices -> indices.size() == 1)
                .map(indices -> indices.iterator().next()).collect(toList());
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
            add(recognizer.events);
        }
        return recognizer;
    }

    private Prompt accept(Prompt prompt, Prompt.Result promptResult) {
        getRecognizer(prompt.choices.locale).endRecognition();
        signal(prompt, promptResult);
        return null;
    }

    private static void logLackOfConfidence(float confidence, float expectedConfidence) {
        logger.info("Lack of confidence {} < expected {})", confidence, expectedConfidence);
    }

    private void logTooManyAudioSignalProblems() {
        logger.info("Audio signal problems: {}", audioSignalProblems);
    }

    private static void logAudioSignalProblemPenalty(double confidence, double penalty) {
        logger.info("Audio signal problem penalty (required {} + penalty {} + = {})", confidence, penalty,
                confidence + penalty);
    }

    private void signal(Prompt prompt, Prompt.Result result) {
        prompt.lock.lock();
        try {
            prompt.signal(this, result);
        } finally {
            prompt.lock.unlock();
        }
    }

    private void signalHandlerInvocation(InputMethod.Notification eventType, SpeechRecognizedEventArgs eventArgs) {
        Prompt prompt = active.get();
        if (prompt != null) {
            prompt.lock.lock();
            try {
                prompt.signal(new SpeechRecognitionInputMethodEventArgs(eventType, eventArgs));
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

    private Setup srgsPhraseBuilder(Choices choices, SpeechRecognition recognizer)
            throws TransformerFactoryConfigurationError {
        try {
            SRGSPhraseBuilder builder = new SRGSPhraseBuilder(choices, recognizer.implementation.getLanguageCode());
            if (logger.isInfoEnabled()) {
                logger.info("{}", builder.slices);
                logger.info("{}", builder.toXML());
            }
            SpeechRecognitionParameters parameters = new SpeechRecognitionParameters(builder);
            return () -> {
                SpeechRecognitionInputMethod.this.speechRecognitionParameters = parameters;
                recognizer.setChoices(parameters);
            };
        } catch (ParserConfigurationException | TransformerException e) {
            throw asRuntimeException(e);
        }
    }

    private Setup simplePhraseBuilder(Choices choices, SpeechRecognition recognizer) {
        SpeechRecognitionParameters parameters = new SpeechRecognitionParameters(choices);
        return () -> {
            SpeechRecognitionInputMethod.this.speechRecognitionParameters = parameters;
            recognizer.setChoices(parameters);
        };
    }

    @Override
    public void show(Prompt prompt) {
        Objects.requireNonNull(prompt);

        active.updateAndGet(previousPrompt -> {
            if (previousPrompt != null) {
                throw new IllegalStateException("Trying to show prompt " + prompt + "when already showing another");
            }

            SpeechRecognition recognizer = getRecognizer(prompt.choices.locale);
            if (recognizer.isActive()) {
                throw new IllegalStateException("Speech recognizer already active");
            }

            prompt.inputMethodInitializers.setup(this);
            recognizer.startRecognition();
            return prompt;
        });
    }

    public Prompt getActivePrompt() {
        return active.get();
    }

    @Override
    public void dismiss(Prompt prompt) throws InterruptedException {
        Objects.requireNonNull(prompt);

        active.updateAndGet(activePrompt -> {
            if (activePrompt == null) {
                return null;
            } else {
                getRecognizer(activePrompt.choices.locale).endRecognition();
            }

            if (activePrompt != prompt) {
                throw new IllegalStateException("Trying to dismiss wrong prompt: " + prompt);
            } else {
                return null;
            }
        });
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

    private void add(SpeechRecognitionEvents speechRecognitionEvents) {
        speechRecognitionEvents.recognitionStarted.add(speechRecognitionStartedEventHandler);
        speechRecognitionEvents.audioLevelUpdated.add(audioLevelUpdatedEventHandler);
        speechRecognitionEvents.audioSignalProblemOccured.add(audioSignalProblemEventHandler);
        speechRecognitionEvents.speechDetected.add(speechDetectedEventHandler);
        speechRecognitionEvents.recognitionRejected.add(recognitionRejected);
        speechRecognitionEvents.recognitionCompleted.add(recognitionCompleted);
    }

    private void remove(SpeechRecognitionEvents speechRecognitionEvents) {
        speechRecognitionEvents.recognitionStarted.remove(speechRecognitionStartedEventHandler);
        speechRecognitionEvents.audioLevelUpdated.remove(audioLevelUpdatedEventHandler);
        speechRecognitionEvents.audioSignalProblemOccured.remove(audioSignalProblemEventHandler);
        speechRecognitionEvents.speechDetected.remove(speechDetectedEventHandler);
        speechRecognitionEvents.recognitionRejected.remove(recognitionRejected);
        speechRecognitionEvents.recognitionCompleted.remove(recognitionCompleted);
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
        usedRecognitionInstances.values().stream().map(recognizer -> recognizer.events).forEach(this::remove);
    }

    public void setFaceToFace(boolean aware) {
        awarenessBonus = aware ? 0.6666f : 1.0f;
    }

    public void completeSpeechRecognition() {
        audioSync.completeSpeechRecognition();
    }

}
