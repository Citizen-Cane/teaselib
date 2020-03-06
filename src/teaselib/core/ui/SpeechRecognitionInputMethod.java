package teaselib.core.ui;

import static java.util.Arrays.asList;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.IntUnaryOperator;
import java.util.stream.Collectors;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;

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
import teaselib.core.speechrecognition.SpeechRecognitionSRGS;
import teaselib.core.speechrecognition.SpeechRecognizer;
import teaselib.core.speechrecognition.events.AudioSignalProblemOccuredEventArgs;
import teaselib.core.speechrecognition.events.SpeechRecognitionStartedEventArgs;
import teaselib.core.speechrecognition.events.SpeechRecognizedEventArgs;
import teaselib.core.speechrecognition.srgs.SRGSPhraseBuilder;
import teaselib.core.ui.Prompt.Result;
import teaselib.core.util.ExceptionUtil;
import teaselib.util.SpeechRecognitionRejectedScript;

/**
 * @author Citizen-Cane
 *
 */
public class SpeechRecognitionInputMethod implements InputMethod, teaselib.core.Closeable {
    private static final double AUDIO_PROBLEM_PENALTY_WEIGHT = 0.005;

    private static final Logger logger = LoggerFactory.getLogger(SpeechRecognitionInputMethod.class);

    public enum Notification implements InputMethod.Notification {
        RecognitionRejected
    }

    final SpeechRecognizer speechRecognizers;
    private final Map<Locale, SpeechRecognition> usedRecognizers = new HashMap<>();

    final Optional<SpeechRecognitionRejectedScript> speechRecognitionRejectedScript;
    final AudioSignalProblems audioSignalProblems;

    private final Event<SpeechRecognitionStartedEventArgs> speechRecognitionStartedEventHandler;
    private final Event<AudioSignalProblemOccuredEventArgs> audioSignalProblemEventHandler;
    private final Event<SpeechRecognizedEventArgs> speechDetectedEventHandler;
    private final Event<SpeechRecognizedEventArgs> recognitionRejected;
    private final Event<SpeechRecognizedEventArgs> recognitionCompleted;

    private final AtomicReference<Prompt> active = new AtomicReference<>();

    public SpeechRecognitionInputMethod(SpeechRecognizer speechRecognizers,
            Optional<SpeechRecognitionRejectedScript> speechRecognitionRejectedScript) {
        this.speechRecognizers = speechRecognizers;
        this.speechRecognitionRejectedScript = speechRecognitionRejectedScript;
        this.audioSignalProblems = new AudioSignalProblems();

        this.speechRecognitionStartedEventHandler = eventArgs -> audioSignalProblems.clear();

        this.audioSignalProblemEventHandler = audioSignal -> audioSignalProblems.add(audioSignal.problem);

        this.speechDetectedEventHandler = eventArgs -> {
            if (audioSignalProblems.occured() && getRecognizer().speechRecognitionInProgress()) {
                logAudioSignalProblem(eventArgs.result);
                getRecognizer().restartRecognition();
            }
        };

        this.recognitionRejected = eventArgs -> {
            signalHandlerInvocation(Notification.RecognitionRejected, eventArgs);
        };

        this.recognitionCompleted = eventArgs -> {
            active.updateAndGet(prompt -> {
                if (audioSignalProblems.occured()) {
                    logAudioSignalProblem(eventArgs.result);
                    return prompt;
                } else {
                    Rule result = eventArgs.result[0];
                    if (eventArgs.result.length > 1) {
                        if (prompt.acceptedResult == Result.Accept.AllSame) {
                            result = distinct(eventArgs.result).orElseThrow();
                        } else {
                            throw new UnsupportedOperationException("TODO Define best result for multiple choices");
                        }
                    } else if (eventArgs.result.length == 1) {
                        result = eventArgs.result[0];
                    } else {
                        throw new IllegalStateException("RecognitionCompleted-event without result");
                    }

                    try {
                        RuleIndicesList choices = result.gather();

                        double penalty = audioSignalProblems.penalty();
                        Confidence expected = getRecognizer(prompt.choices.locale).getRecognitionConfidence();
                        if (!confidenceIsHighEnough(result, expected, penalty)) {
                            if (confidenceIsHighEnough(result, expected, 0)) {
                                logAudioSignalProblemPenalty(result, expected, penalty);
                            } else {
                                logLackOfConfidence(result, expected);
                            }
                            reject(eventArgs);
                            return prompt;
                        } else {
                            if (choices.isEmpty()) {
                                handleNoChoices(eventArgs, result);
                                return prompt;
                            } else if (prompt.acceptedResult == Result.Accept.Multiple) {
                                return handleMultipleChoices(eventArgs, result, prompt, choices);
                            } else if (prompt.acceptedResult == Result.Accept.AllSame) {
                                return handleDistinctChoice(eventArgs, result, prompt, choices);
                            } else {
                                throw new UnsupportedOperationException(prompt.acceptedResult.toString());
                            }
                        }
                    } catch (Exception e) {
                        prompt.setException(e);
                        getRecognizer(prompt.choices.locale).endRecognition();
                        return null;
                    }
                }
            });
        };
    }

    private SpeechRecognition getRecognizer() {
        Prompt prompt = active.get();
        if (prompt == null)
            throw new IllegalStateException("Prompt not set");
        return getRecognizer(prompt.choices.locale);
    }

    private SpeechRecognition getRecognizer(Locale locale) {
        SpeechRecognition recognizer = speechRecognizers.get(locale);
        if (usedRecognizers.put(recognizer.locale, recognizer) == null) {
            addEvents(recognizer);
        }
        return recognizer;
    }

    private static Optional<Rule> distinct(Rule... result) {
        return distinct(asList(result));
    }

    public static Optional<Rule> distinct(List<Rule> result) {
        return result.stream().filter(r -> r.gather().getCommonDistinctValue().isPresent())
                .reduce(Rule::maxProbability);
    }

    private void handleNoChoices(SpeechRecognizedEventArgs eventArgs, Rule result) {
        logger.warn("No choice rules in: {} - rejecting ", result);
        eventArgs.consumed = true;
        fireRecognitionRejectedEvent(eventArgs);
    }

    private Prompt handleMultipleChoices(SpeechRecognizedEventArgs eventArgs, Rule result, Prompt prompt,
            RuleIndicesList choices) {
        List<Integer> distinctChoices = choices.stream().map(indices -> indices.size() == 1 ? indices.iterator().next()
                : Prompt.Result.UNDEFINED.elements.iterator().next()).collect(Collectors.toList());
        Prompt.Result promptResult = new Prompt.Result(distinctChoices);
        if (promptResult.valid(prompt.choices)) {
            accept(prompt, promptResult);
            return null;
        } else {
            logger.warn("Undefined result index in {} : {} - rejecting", choices, result);
            reject(eventArgs);
            return prompt;
        }
    }

    private Prompt handleDistinctChoice(SpeechRecognizedEventArgs eventArgs, Rule result, Prompt prompt,
            RuleIndicesList choices) {
        Optional<Integer> distinctChoice = choices.getCommonDistinctValue();
        if (distinctChoice.isPresent()) {
            accept(prompt, new Prompt.Result(getRecognizer().mapPhraseToChoice(distinctChoice.get())));
            return null;
        } else {
            logger.warn("No distinct choice {} in {} - rejecting", choices, result);
            reject(eventArgs);
            return prompt;
        }
    }

    private Prompt accept(Prompt prompt, Prompt.Result promptResult) {
        getRecognizer(prompt.choices.locale).endRecognition();
        signal(prompt, promptResult);
        return null;
    }

    private void reject(SpeechRecognizedEventArgs eventArgs) {
        eventArgs.consumed = true;
        fireRecognitionRejectedEvent(eventArgs);
    }

    private void fireRecognitionRejectedEvent(SpeechRecognizedEventArgs eventArgs) {
        getRecognizer().events.recognitionRejected.run(new SpeechRecognizedEventArgs(eventArgs.result));
    }

    private static void logLackOfConfidence(Rule result, Confidence confidence) {
        logger.info("Rejecting result '{}' due to lack of confidence (expected {})", result, confidence);
    }

    private void logAudioSignalProblem(Rule[] result) {
        logger.info("Rejecting result '{}' due to audio signal problems {}", result, audioSignalProblems);
    }

    private static void logAudioSignalProblemPenalty(Rule result, Confidence confidence, double penalty) {
        logger.info("Rejecting result '{}' due to audio signal problem penalty (required  {} + {} + = {})", result,
                confidence, penalty, confidence.probability + penalty);
    }

    private static boolean confidenceIsHighEnough(Rule result, Confidence expected, double penalty) {
        return result.probability - penalty * AUDIO_PROBLEM_PENALTY_WEIGHT >= expected.probability
                && result.confidence.isAsHighAs(expected);
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
        SpeechRecognitionImplementation sr = recognizer.sr;
        if (sr instanceof SpeechRecognitionSRGS) {
            try {
                SRGSPhraseBuilder builder = new SRGSPhraseBuilder(choices, sr.getLanguageCode());
                if (logger.isInfoEnabled()) {
                    logger.info("{}", builder.toXML());
                }

                IntUnaryOperator mapper = builder::map;
                byte[] srgs = builder.toBytes();

                return () -> recognizer.setChoices(choices, srgs, mapper);
            } catch (ParserConfigurationException | TransformerException e) {
                throw ExceptionUtil.asRuntimeException(e);
            }
        } else if (sr instanceof SpeechRecognitionChoices) {
            return () -> recognizer.setChoices(choices, null, value -> value);
        } else {
            throw new UnsupportedOperationException();
        }
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

    // TODO Resolve race condition: lazy initialization versus dismiss in main thread on timeout
    // -> recognition not started yet but main thread dismisses prompt

    // TODO add events at startup and make InputMethod AutoCloseable -> less synchronization issues plus stable testing
    // + event sources are synchronized
    // + disabling the recognizer should be enough
    // + events have to be added early in the queue because the SR input method consumes events and fires new events
    // -> propagation doesn't work when events are added temporarily since they are added last
    // Alternatively remove firing new events by moving rejection into the speech detection handler
    // see also teaselib.core.speechrecognition.SpeechRecognitionTestUtils.awaitResult(...)
    private void startSpeechRecognition(Prompt prompt) {
        SpeechRecognition recognizer = getRecognizer(prompt.choices.locale);
        if (recognizer.isActive()) {
            throw new IllegalStateException("Speech recognizer already active");
        }

        recognizer.startRecognition(map(prompt.choices.intention));
    }

    private static Confidence map(Intention intention) {
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
        events.audioSignalProblemOccured.add(audioSignalProblemEventHandler);
        events.speechDetected.add(speechDetectedEventHandler);
        events.recognitionRejected.add(recognitionRejected);
        events.recognitionCompleted.add(recognitionCompleted);
    }

    private void removeEvents(SpeechRecognition speechRecognizer) {
        SpeechRecognitionEvents events = speechRecognizer.events;
        events.recognitionStarted.remove(speechRecognitionStartedEventHandler);
        events.audioSignalProblemOccured.remove(audioSignalProblemEventHandler);
        events.speechDetected.remove(speechDetectedEventHandler);
        events.recognitionRejected.remove(recognitionRejected);
        events.recognitionCompleted.remove(recognitionCompleted);
    }

    @Override
    public String toString() {
        Prompt prompt = active.get();
        String object = prompt != null ? getRecognizer(prompt.choices.locale).toString() : "<inactive>";
        String expectedConfidence = prompt != null
                ? getRecognizer(prompt.choices.locale).getRecognitionConfidence().toString()
                : "<?>";
        return "SpeechRecognizer=" + object + " confidence=" + expectedConfidence;
    }

    @Override
    public void close() {
        usedRecognizers.values().stream().forEach(this::removeEvents);
    }

}
