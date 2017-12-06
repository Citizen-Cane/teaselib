package teaselib.core.ui;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import teaselib.core.events.Event;
import teaselib.core.speechrecognition.AudioSignalProblems;
import teaselib.core.speechrecognition.SpeechRecognition;
import teaselib.core.speechrecognition.SpeechRecognitionImplementation;
import teaselib.core.speechrecognition.SpeechRecognitionResult;
import teaselib.core.speechrecognition.SpeechRecognitionResult.Confidence;
import teaselib.core.speechrecognition.events.AudioSignalProblemOccuredEventArgs;
import teaselib.core.speechrecognition.events.SpeechRecognitionStartedEventArgs;
import teaselib.core.speechrecognition.events.SpeechRecognizedEventArgs;
import teaselib.util.SpeechRecognitionRejectedScript;

/**
 * @author Citizen-Cane
 *
 */
public class SpeechRecognitionInputMethod implements InputMethod {
    private static final Logger logger = LoggerFactory.getLogger(SpeechRecognitionInputMethod.class);

    private static final int RECOGNITION_REJECTED = -666;
    private static final String RECOGNITION_REJECTED_HNADLER_KEY = "Recognition Rejected";

    final SpeechRecognition speechRecognizer;
    final Confidence confidence;
    final Optional<SpeechRecognitionRejectedScript> speechRecognitionRejectedScript;
    final AudioSignalProblems audioSignalProblems;

    private final Event<SpeechRecognitionImplementation, SpeechRecognitionStartedEventArgs> speechRecognitionStartedEventHandler;
    private final Event<SpeechRecognitionImplementation, AudioSignalProblemOccuredEventArgs> audioSignalProblemEventHandler;
    private final Event<SpeechRecognitionImplementation, SpeechRecognizedEventArgs> speechDetectedEventHandler;
    private final Event<SpeechRecognitionImplementation, SpeechRecognizedEventArgs> recognitionRejected;
    private final Event<SpeechRecognitionImplementation, SpeechRecognizedEventArgs> recognitionCompleted;

    private final AtomicReference<Prompt> active = new AtomicReference<>();

    public SpeechRecognitionInputMethod(SpeechRecognition speechRecognizer, Confidence recognitionConfidence,
            Optional<SpeechRecognitionRejectedScript> speechRecognitionRejectedScript) {
        this.speechRecognizer = speechRecognizer;
        this.confidence = recognitionConfidence;
        this.speechRecognitionRejectedScript = speechRecognitionRejectedScript;
        this.audioSignalProblems = new AudioSignalProblems();

        this.speechRecognitionStartedEventHandler = (sender, eventArgs) -> audioSignalProblems.clear();
        this.audioSignalProblemEventHandler = (sender, audioSignal) -> audioSignalProblems.add(audioSignal.problem);
        this.speechDetectedEventHandler = this::handleSpeechDetected;
        this.recognitionRejected = this::handleSpeechRecognitionRejected;
        this.recognitionCompleted = this::handleSpeechRecognitionCompleted;
    }

    private void handleSpeechDetected(SpeechRecognitionImplementation sender, SpeechRecognizedEventArgs eventArgs) {
        if (audioSignalProblems.occured() && speechRecognizer.isSpeechRecognitionInProgress()) {
            SpeechRecognitionResult result = eventArgs.result[0];
            logAudioSignalProblem(result);
            speechRecognizer.stopRecognition();
            speechRecognizer.resumeRecognition();
        }
    }

    private void handleSpeechRecognitionRejected(SpeechRecognitionImplementation sender,
            SpeechRecognizedEventArgs eventArgs) {
        if (speechRecognitionRejectedScript.isPresent() && speechRecognitionRejectedScript.get().canRun()) {
            signal(RECOGNITION_REJECTED);
        }
    }

    private void handleSpeechRecognitionCompleted(SpeechRecognitionImplementation sender,
            SpeechRecognizedEventArgs eventArgs) {
        if (eventArgs.result.length == 1) {
            SpeechRecognitionResult result = eventArgs.result[0];
            if (audioSignalProblems.occured()) {
                logAudioSignalProblem(result);
            } else if (!confidenceIsHighEnough(result, confidence)) {
                logLackOfConfidence(result);
            } else {
                signal(result.index);
            }
        } else {
            logger.info("Ignoring none or more than one result");
        }
    }

    private void logLackOfConfidence(SpeechRecognitionResult result) {
        logger.info("Dropping result '" + result + "' due to lack of confidence (expected " + confidence + ")");
    }

    private void logAudioSignalProblem(SpeechRecognitionResult result) {
        logger.info("Dropping result '" + result + "' due to audio signal problems " + audioSignalProblems);
    }

    private boolean confidenceIsHighEnough(SpeechRecognitionResult result, Confidence confidence) {
        return result.probability >= confidence.probability || result.confidence.isAsHighAs(confidence);
    }

    private void signal(int resultIndex) {
        Prompt prompt = active.get();
        prompt.lock.lock();

        try {
            if (resultIndex == RECOGNITION_REJECTED) {
                prompt.signalHandlerInvocation(RECOGNITION_REJECTED_HNADLER_KEY);
            } else {
                prompt.signalResult(resultIndex);
            }
        } finally {
            prompt.lock.unlock();
        }
    }

    @Override
    public synchronized void show(Prompt prompt) {
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
            boolean dismissed = prompt.result() == Prompt.UNDEFINED;
            disableSpeechRecognition();
            active.set(null);
            return dismissed;
        }
    }

    private void enableSpeechRecognition() {
        speechRecognizer.events.recognitionStarted.add(speechRecognitionStartedEventHandler);
        speechRecognizer.events.audioSignalProblemOccured.add(audioSignalProblemEventHandler);
        speechRecognizer.events.speechDetected.add(speechDetectedEventHandler);
        speechRecognizer.events.recognitionRejected.add(recognitionRejected);
        speechRecognizer.events.recognitionCompleted.add(recognitionCompleted);
        speechRecognizer.startRecognition(active.get().derived, confidence);
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
        handlers.put(RECOGNITION_REJECTED_HNADLER_KEY, () -> {
            if (speechRecognitionRejectedScript.isPresent()) {
                speechRecognitionRejectedScript.get().run();
            }
        });
        return handlers;
    }

    @Override
    public String toString() {
        return "SpeechRecognizer=" + speechRecognizer + " confidence=" + confidence;
    }
}
