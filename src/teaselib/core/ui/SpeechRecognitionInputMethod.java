package teaselib.core.ui;

import static java.util.Arrays.asList;

import java.util.HashMap;
import java.util.List;
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
import teaselib.core.speechrecognition.SpeechRecognitionImplementation;
import teaselib.core.speechrecognition.SpeechRecognitionSRGS;
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
public class SpeechRecognitionInputMethod implements InputMethod {
    private static final double AUDIO_PROBLEM_PENALTY_WEIGHT = 0.005;

    private static final Logger logger = LoggerFactory.getLogger(SpeechRecognitionInputMethod.class);

    private static final String RECOGNITION_REJECTED_HANDLER_KEY = "Recognition Rejected";

    final SpeechRecognition speechRecognizer;
    final Confidence expectedConfidence;
    final Optional<SpeechRecognitionRejectedScript> speechRecognitionRejectedScript;
    final AudioSignalProblems audioSignalProblems;

    private final Event<SpeechRecognitionStartedEventArgs> speechRecognitionStartedEventHandler;
    private final Event<AudioSignalProblemOccuredEventArgs> audioSignalProblemEventHandler;
    private final Event<SpeechRecognizedEventArgs> speechDetectedEventHandler;
    private final Event<SpeechRecognizedEventArgs> recognitionRejected;
    private final Event<SpeechRecognizedEventArgs> recognitionCompleted;

    private final AtomicReference<Prompt> active = new AtomicReference<>();
    private boolean speechRecognitionRejectedHandlerSignaled = false;

    public SpeechRecognitionInputMethod(SpeechRecognition speechRecognizer, Confidence expectedConfidence,
            Optional<SpeechRecognitionRejectedScript> speechRecognitionRejectedScript) {
        this.speechRecognizer = speechRecognizer;
        this.expectedConfidence = expectedConfidence;
        this.speechRecognitionRejectedScript = speechRecognitionRejectedScript;
        this.audioSignalProblems = new AudioSignalProblems();

        this.speechRecognitionStartedEventHandler = eventArgs -> audioSignalProblems.clear();

        this.audioSignalProblemEventHandler = audioSignal -> audioSignalProblems.add(audioSignal.problem);

        this.speechDetectedEventHandler = eventArgs -> {
            if (audioSignalProblems.occured() && speechRecognizer.isSpeechRecognitionInProgress()) {
                logAudioSignalProblem(eventArgs.result);
                speechRecognizer.restartRecognition();
            }
        };

        this.recognitionRejected = eventArgs -> {
            if (eventArgs.result != null && eventArgs.result.length == 1) {
                if (!speechRecognitionRejectedHandlerSignaled && speechRecognitionRejectedScript.isPresent()
                        && speechRecognitionRejectedScript.get().canRun()) {
                    speechRecognitionRejectedHandlerSignaled = true;
                    signalHandlerInvocation(RECOGNITION_REJECTED_HANDLER_KEY);
                }
            }
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
                        if (!confidenceIsHighEnough(result, expectedConfidence, penalty)) {
                            if (confidenceIsHighEnough(result, expectedConfidence, 0)) {
                                logAudioSignalProblemPenalty(result, penalty);
                            } else {
                                logLackOfConfidence(result);
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
                        endSpeechRecognition();
                        return null;
                    }
                }
            });
        };
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
            accept(prompt, new Prompt.Result(speechRecognizer.mapPhraseToChoice(distinctChoice.get())));
            return null;
        } else {
            logger.warn("No distinct choice {} in {} - rejecting", choices, result);
            reject(eventArgs);
            return prompt;
        }
    }

    private Prompt accept(Prompt prompt, Prompt.Result promptResult) {
        endSpeechRecognition();
        signal(prompt, promptResult);
        return null;
    }

    private void reject(SpeechRecognizedEventArgs eventArgs) {
        eventArgs.consumed = true;
        fireRecognitionRejectedEvent(eventArgs);
    }

    private void fireRecognitionRejectedEvent(SpeechRecognizedEventArgs eventArgs) {
        speechRecognizer.events.recognitionRejected.run(new SpeechRecognizedEventArgs(eventArgs.result));
    }

    private void logLackOfConfidence(Rule result) {
        logger.info("Rejecting result '{}' due to lack of confidence (expected {})", result, expectedConfidence);
    }

    private void logAudioSignalProblem(Rule[] result) {
        logger.info("Rejecting result '{}' due to audio signal problems {}", result, audioSignalProblems);
    }

    private void logAudioSignalProblemPenalty(Rule result, double penalty) {
        logger.info("Rejecting result '{}' due to audio signal problem penalty  (required  {} + {} + = {})", result,
                expectedConfidence, penalty, expectedConfidence.probability + penalty);
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

    private void signalHandlerInvocation(String handlerKey) {
        Prompt prompt = active.get();
        prompt.lock.lock();
        try {
            prompt.signalHandlerInvocation(handlerKey);
        } finally {
            prompt.lock.unlock();
        }
    }

    @Override
    public Setup getSetup(Choices choices) {
        SpeechRecognitionImplementation sr = speechRecognizer.sr;
        if (sr instanceof SpeechRecognitionSRGS) {
            try {
                SRGSPhraseBuilder builder = new SRGSPhraseBuilder(choices, sr.getLanguageCode());
                if (logger.isInfoEnabled()) {
                    logger.info("{}", builder.toXML());
                }

                IntUnaryOperator mapper = builder::map;
                byte[] srgs = builder.toBytes();

                return () -> speechRecognizer.setChoices(choices, srgs, mapper);
            } catch (ParserConfigurationException | TransformerException e) {
                throw ExceptionUtil.asRuntimeException(e);
            }
        } else if (sr instanceof SpeechRecognitionChoices) {
            return () -> speechRecognizer.setChoices(choices, null, value -> value);
        } else {
            throw new UnsupportedOperationException();
        }
    }

    @Override
    public void show(Prompt prompt) {
        Objects.requireNonNull(prompt);
        active.set(prompt);
        prompt.inputMethodInitializers.setup(this);
        startSpeechRecognition();
    }

    @Override
    public boolean dismiss(Prompt prompt) throws InterruptedException {
        Prompt activePrompt = active.getAndSet(null);
        if (activePrompt == null) {
            return false;
        } else if (activePrompt != prompt) {
            throw new IllegalStateException("Trying to dismiss wrong prompt");
        } else {
            endSpeechRecognition();
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
    private void startSpeechRecognition() {
        if (speechRecognizer.isActive()) {
            throw new IllegalStateException("Speech recognizer already active");
        }

        addEvents();
        speechRecognizer.startRecognition(expectedConfidence);
    }

    private void addEvents() {
        speechRecognizer.events.recognitionStarted.add(speechRecognitionStartedEventHandler);
        speechRecognizer.events.audioSignalProblemOccured.add(audioSignalProblemEventHandler);
        speechRecognizer.events.speechDetected.add(speechDetectedEventHandler);
        speechRecognizer.events.recognitionRejected.add(recognitionRejected);
        speechRecognizer.events.recognitionCompleted.add(recognitionCompleted);
    }

    private void endSpeechRecognition() {
        try {
            speechRecognizer.endRecognition();
        } finally {
            removeEvents();
        }
    }

    private void removeEvents() {
        speechRecognizer.events.recognitionStarted.remove(speechRecognitionStartedEventHandler);
        speechRecognizer.events.audioSignalProblemOccured.remove(audioSignalProblemEventHandler);
        speechRecognizer.events.speechDetected.remove(speechDetectedEventHandler);
        speechRecognizer.events.recognitionRejected.remove(recognitionRejected);
        speechRecognizer.events.recognitionCompleted.remove(recognitionCompleted);
    }

    @Override
    public Map<String, Runnable> getHandlers() {
        HashMap<String, Runnable> handlers = new HashMap<>();
        if (speechRecognitionRejectedScript.isPresent()) {
            SpeechRecognitionRejectedScript script = speechRecognitionRejectedScript.get();
            handlers.put(RECOGNITION_REJECTED_HANDLER_KEY, script::run);
        }
        return handlers;
    }

    @Override
    public String toString() {
        return "SpeechRecognizer=" + speechRecognizer + " confidence=" + expectedConfidence;
    }

}
