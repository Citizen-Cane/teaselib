package teaselib.core.ui;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import teaselib.core.events.Event;
import teaselib.core.speechrecognition.AudioSignalProblems;
import teaselib.core.speechrecognition.Confidence;
import teaselib.core.speechrecognition.Rule;
import teaselib.core.speechrecognition.SpeechRecognition;
import teaselib.core.speechrecognition.events.AudioSignalProblemOccuredEventArgs;
import teaselib.core.speechrecognition.events.SpeechRecognitionStartedEventArgs;
import teaselib.core.speechrecognition.events.SpeechRecognizedEventArgs;
import teaselib.core.speechrecognition.srgs.Phrases;
import teaselib.core.ui.Prompt.Result;
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

        this.speechRecognitionStartedEventHandler = (eventArgs) -> audioSignalProblems.clear();

        this.audioSignalProblemEventHandler = (audioSignal) -> audioSignalProblems.add(audioSignal.problem);

        this.speechDetectedEventHandler = (eventArgs) -> {
            if (audioSignalProblems.occured() && speechRecognizer.isSpeechRecognitionInProgress()) {
                Rule result = eventArgs.result[0];
                logAudioSignalProblem(result);
                speechRecognizer.restartRecognition();
            }
        };

        this.recognitionRejected = (eventArgs) -> {
            if (eventArgs.result != null && eventArgs.result.length == 1) {
                if (!speechRecognitionRejectedHandlerSignaled && speechRecognitionRejectedScript.isPresent()
                        && speechRecognitionRejectedScript.get().canRun()) {
                    speechRecognitionRejectedHandlerSignaled = true;
                    signalHandlerInvocation(RECOGNITION_REJECTED_HANDLER_KEY);
                }
            }
        };

        this.recognitionCompleted = (eventArgs) -> {
            if (eventArgs.result.length == 1) {
                Rule result = eventArgs.result[0];
                logger.info("rules \n{}", result.prettyPrint());

                if (audioSignalProblems.occured()) {
                    logAudioSignalProblem(result);
                } else {
                    double penalty = audioSignalProblems.penalty();
                    if (!confidenceIsHighEnough(result, expectedConfidence, penalty)) {
                        if (confidenceIsHighEnough(result, expectedConfidence, 0)) {
                            logAudioSignalProblemPenalty(result, penalty);
                        } else {
                            logLackOfConfidence(result);
                        }
                    } else {
                        disableSpeechRecognition();
                        try {
                            List<Integer> choices = gatherResults(result);
                            if (choices.isEmpty()) {
                                logger.info("No choice rules in: {} - rejecting ", result);
                                eventArgs.consumed = true;
                                fireRecognitionRejectedEvent(result);
                            } else if (active.get().acceptedResult == Result.Accept.AllSame
                                    && choices.stream().distinct().count() > 1) {
                                logger.info("ambiguous choice rules {} in: {} - rejecting ", choices, result);
                                eventArgs.consumed = true;
                                fireRecognitionRejectedEvent(result);
                            } else {
                                signal(new Prompt.Result(choices));
                            }
                        } catch (Exception e) {
                            active.get().setException(e);
                        } finally {
                            active.set(null);
                        }
                    }
                }
            } else {
                logger.info("Ignoring none or more than one result");
            }
        };
    }

    private List<Integer> gatherResults(Rule rule) {
        ArrayList<Integer> results = new ArrayList<>();
        if (rule.choiceIndex > Prompt.Result.DISMISSED.elements.get(0)) {
            results.add(rule.choiceIndex);
        }
        rule.children.stream().forEach(child -> results.addAll(gatherResults(child)));
        return results;
    }

    private void fireRecognitionRejectedEvent(Rule result) {
        SpeechRecognizedEventArgs recognitionRejectedEventArgs = new SpeechRecognizedEventArgs(result);
        speechRecognizer.events.recognitionRejected.run(recognitionRejectedEventArgs);
    }

    private void logLackOfConfidence(Rule result) {
        logger.info("Dropping result '{}' due to lack of confidence (expected {})", result, expectedConfidence);
    }

    private void logAudioSignalProblem(Rule result) {
        logger.info("Dropping result '{}' due to audio signal problems {}", result, audioSignalProblems);
    }

    private void logAudioSignalProblemPenalty(Rule result, double penalty) {
        logger.info("Dropping result '{}' due to audio signal problem penalty  (required  {} + {} + = {})", result,
                expectedConfidence, penalty, expectedConfidence.probability + penalty);
    }

    private static boolean confidenceIsHighEnough(Rule result, Confidence expected, double penalty) {
        return result.probability - penalty * AUDIO_PROBLEM_PENALTY_WEIGHT >= expected.probability
                && result.confidence.isAsHighAs(expected);
    }

    private void signal(Prompt.Result result) {
        Prompt prompt = active.get();
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
    public synchronized void show(Prompt prompt) {
        Objects.requireNonNull(prompt);
        active.set(prompt);
        enableSpeechRecognition();
    }

    @Override
    public boolean dismiss(Prompt prompt) throws InterruptedException {
        Prompt activePrompt = active.get();

        if (activePrompt == null) {
            return false;
        } else if (activePrompt != prompt) {
            throw new IllegalStateException("Trying to dismiss wrong prompt");
        } else {
            disableSpeechRecognition();
            active.set(null);
            return prompt.result() == Prompt.Result.UNDEFINED;
        }
    }

    // TODO Resolve race condition lazy initialization versus dismiss in main thread on timeout
    // -> recognition not started yet but main thread dismisses prompt

    private void enableSpeechRecognition() {
        if (speechRecognizer.isActive()) {
            throw new IllegalStateException("Speech recognizer already active");
        }

        speechRecognizer.events.recognitionStarted.add(speechRecognitionStartedEventHandler);
        speechRecognizer.events.audioSignalProblemOccured.add(audioSignalProblemEventHandler);
        speechRecognizer.events.speechDetected.add(speechDetectedEventHandler);
        speechRecognizer.events.recognitionRejected.add(recognitionRejected);
        speechRecognizer.events.recognitionCompleted.add(recognitionCompleted);

        Phrases phrases = Phrases.of(active.get().choices);
        speechRecognizer.startRecognition(phrases, expectedConfidence);
    }

    private void disableSpeechRecognition() {
        if (!speechRecognizer.isActive()) {
            throw new IllegalStateException("Speech recognizer not active");
        }

        logger.debug("Stopping speech recognition");
        speechRecognizer.stopRecognition();

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
