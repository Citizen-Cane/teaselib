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
import teaselib.core.speechrecognition.SpeechRecognitionControl;
import teaselib.core.speechrecognition.events.AudioSignalProblemOccuredEventArgs;
import teaselib.core.speechrecognition.events.SpeechRecognitionStartedEventArgs;
import teaselib.core.speechrecognition.events.SpeechRecognizedEventArgs;
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

    private final Event<SpeechRecognitionControl, SpeechRecognitionStartedEventArgs> speechRecognitionStartedEventHandler;
    private final Event<SpeechRecognitionControl, AudioSignalProblemOccuredEventArgs> audioSignalProblemEventHandler;
    private final Event<SpeechRecognitionControl, SpeechRecognizedEventArgs> speechDetectedEventHandler;
    private final Event<SpeechRecognitionControl, SpeechRecognizedEventArgs> recognitionRejected;
    private final Event<SpeechRecognitionControl, SpeechRecognizedEventArgs> recognitionCompleted;

    private final AtomicReference<Prompt> active = new AtomicReference<>();
    private boolean speechRecognitionRejectedHandlerSignaled = false;

    public SpeechRecognitionInputMethod(SpeechRecognition speechRecognizer, Confidence expectedConfidence,
            Optional<SpeechRecognitionRejectedScript> speechRecognitionRejectedScript) {
        this.speechRecognizer = speechRecognizer;
        this.expectedConfidence = expectedConfidence;
        this.speechRecognitionRejectedScript = speechRecognitionRejectedScript;
        this.audioSignalProblems = new AudioSignalProblems();

        this.speechRecognitionStartedEventHandler = (sender, eventArgs) -> audioSignalProblems.clear();

        this.audioSignalProblemEventHandler = (sender, audioSignal) -> audioSignalProblems.add(audioSignal.problem);

        this.speechDetectedEventHandler = (sender, eventArgs) -> {
            if (audioSignalProblems.occured() && speechRecognizer.isSpeechRecognitionInProgress()) {
                Rule result = eventArgs.result[0];
                logAudioSignalProblem(result);
                speechRecognizer.restartRecognition();
            }
        };
        this.recognitionRejected = (sender, eventArgs) -> {
            if (eventArgs.result != null && eventArgs.result.length == 1) {
                if (!speechRecognitionRejectedHandlerSignaled && speechRecognitionRejectedScript.isPresent()
                        && speechRecognitionRejectedScript.get().canRun()) {
                    speechRecognitionRejectedHandlerSignaled = true;
                    signalHandlerInvocation(RECOGNITION_REJECTED_HANDLER_KEY);
                }
            }
        };

        this.recognitionCompleted = (sender, eventArgs) -> {
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
                                throw new IllegalArgumentException("No choice rules in: " + result);
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
        if (rule.id > Prompt.Result.DISMISSED.elements.get(0)) {
            // TODO rename id to choice or item (srgs element name)
            results.add(rule.id);
        }
        rule.children.stream().forEach(child -> results.addAll(gatherResults(child)));
        return results;
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

    private void enableSpeechRecognition() {
        speechRecognizer.events.recognitionStarted.add(speechRecognitionStartedEventHandler);
        speechRecognizer.events.audioSignalProblemOccured.add(audioSignalProblemEventHandler);
        speechRecognizer.events.speechDetected.add(speechDetectedEventHandler);
        speechRecognizer.events.recognitionRejected.add(recognitionRejected);
        speechRecognizer.events.recognitionCompleted.add(recognitionCompleted);
        speechRecognizer.startRecognition(addMumbleDetection(active.get().choices.toDisplay()), expectedConfidence);
    }

    private static List<String> addMumbleDetection(List<String> display) {
        List<String> choices = new ArrayList<>(display.size() + 1);
        choices.addAll(display);
        return choices;
    }

    private void disableSpeechRecognition() {
        logger.debug("Stopping speech recognition");
        speechRecognizer.events.recognitionStarted.remove(speechRecognitionStartedEventHandler);
        speechRecognizer.events.audioSignalProblemOccured.remove(audioSignalProblemEventHandler);
        speechRecognizer.events.speechDetected.remove(speechDetectedEventHandler);
        speechRecognizer.events.recognitionRejected.remove(recognitionRejected);
        speechRecognizer.events.recognitionCompleted.remove(recognitionCompleted);
        speechRecognizer.stopRecognition();
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
