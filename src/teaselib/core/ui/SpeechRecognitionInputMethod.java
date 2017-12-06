package teaselib.core.ui;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import teaselib.core.events.Event;
import teaselib.core.speechrecognition.SpeechRecognition;
import teaselib.core.speechrecognition.SpeechRecognition.AudioSignalProblem;
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

    private final SpeechRecognitionStartedEventHandler speechRecognitionStartedEventHandler;
    private final AudioSignalProblemEventHandler audioSignalProblemEventHandler;
    private final SpeechDetectedEventHandler speechDetectedEventHandler;
    private final SpeechRecognitionRejectedEventHandler recognitionRejected;
    private final SpeechRecognitionCompletedEventHandler recognitionCompleted;

    private final AtomicReference<Prompt> active = new AtomicReference<>();

    public SpeechRecognitionInputMethod(SpeechRecognition speechRecognizer, Confidence recognitionConfidence,
            Optional<SpeechRecognitionRejectedScript> speechRecognitionRejectedScript) {
        this.speechRecognizer = speechRecognizer;
        this.confidence = recognitionConfidence;
        this.speechRecognitionRejectedScript = speechRecognitionRejectedScript;
        this.audioSignalProblems = new AudioSignalProblems();

        this.speechRecognitionStartedEventHandler = new SpeechRecognitionStartedEventHandler(audioSignalProblems);
        this.audioSignalProblemEventHandler = new AudioSignalProblemEventHandler(audioSignalProblems);
        speechDetectedEventHandler = new SpeechDetectedEventHandler(speechRecognizer, audioSignalProblems);
        this.recognitionRejected = new SpeechRecognitionRejectedEventHandler(this, speechRecognitionRejectedScript);
        this.recognitionCompleted = new SpeechRecognitionCompletedEventHandler(this, confidence, audioSignalProblems);
    }

    static final class AudioSignalProblems {
        Map<SpeechRecognition.AudioSignalProblem, AtomicInteger> problems = new EnumMap<>(AudioSignalProblem.class);

        public AudioSignalProblems() {
            clear();
        }

        void clear() {
            problems.clear();
            for (AudioSignalProblem audioSignalProblem : AudioSignalProblem.values()) {
                problems.put(audioSignalProblem, new AtomicInteger(0));
            }
        }

        void add(AudioSignalProblem audioSignalProblem) {
            problems.get(audioSignalProblem).incrementAndGet();
        }

        public boolean occured() {
            for (AtomicInteger value : problems.values()) {
                if (value.get() > 0) {
                    return true;
                }
            }
            return false;
        }

        @Override
        public String toString() {
            return problems.toString();
        }
    }

    static final class SpeechRecognitionStartedEventHandler
            implements Event<SpeechRecognitionImplementation, SpeechRecognitionStartedEventArgs> {
        private final AudioSignalProblems audioSignalProblems;

        public SpeechRecognitionStartedEventHandler(AudioSignalProblems audioSignalProblems) {
            this.audioSignalProblems = audioSignalProblems;
        }

        @Override
        public void run(SpeechRecognitionImplementation sender, SpeechRecognitionStartedEventArgs eventArgs) {
            audioSignalProblems.clear();
        }
    }

    static final class AudioSignalProblemEventHandler
            implements Event<SpeechRecognitionImplementation, AudioSignalProblemOccuredEventArgs> {
        private final AudioSignalProblems audioSignalProblems;

        public AudioSignalProblemEventHandler(AudioSignalProblems audioSignalProblems) {
            this.audioSignalProblems = audioSignalProblems;
        }

        @Override
        public void run(SpeechRecognitionImplementation sender, AudioSignalProblemOccuredEventArgs audioSignalProblem) {
            audioSignalProblems.add(audioSignalProblem.problem);
        }
    }

    static final class SpeechDetectedEventHandler
            implements Event<SpeechRecognitionImplementation, SpeechRecognizedEventArgs> {

        private final SpeechRecognition speechRecognizer;
        private AudioSignalProblems audioSignalProblems;

        public SpeechDetectedEventHandler(SpeechRecognition speechRecognizer, AudioSignalProblems audioSignalProblems) {
            this.speechRecognizer = speechRecognizer;
            this.audioSignalProblems = audioSignalProblems;
        }

        @Override
        public void run(SpeechRecognitionImplementation sender, SpeechRecognizedEventArgs eventArgs) {
            if (audioSignalProblems.occured() && speechRecognizer.isSpeechRecognitionInProgress()) {
                SpeechRecognitionResult result = eventArgs.result[0];
                logAudioSignalProblem(result);
                speechRecognizer.stopRecognition();
                speechRecognizer.resumeRecognition();
            }
        }

        private void logAudioSignalProblem(SpeechRecognitionResult result) {
            logger.info("Dropping result '" + result + "' due to audio signal problems " + audioSignalProblems);
        }
    }

    static final class SpeechRecognitionRejectedEventHandler
            implements Event<SpeechRecognitionImplementation, SpeechRecognizedEventArgs> {
        private final Optional<SpeechRecognitionRejectedScript> speechRecognitionRejectedScript;
        private final SpeechRecognitionInputMethod inputMethod;

        public SpeechRecognitionRejectedEventHandler(SpeechRecognitionInputMethod inputMethod,
                Optional<SpeechRecognitionRejectedScript> speechRecognitionRejectedScript) {
            this.inputMethod = inputMethod;
            this.speechRecognitionRejectedScript = speechRecognitionRejectedScript;
        }

        @Override
        public void run(SpeechRecognitionImplementation sender, SpeechRecognizedEventArgs eventArgs) {
            if (speechRecognitionRejectedScript.isPresent() && speechRecognitionRejectedScript.get().canRun()) {
                inputMethod.signal(RECOGNITION_REJECTED);
            }
        }
    }

    static final class SpeechRecognitionCompletedEventHandler
            implements Event<SpeechRecognitionImplementation, SpeechRecognizedEventArgs> {
        private final SpeechRecognitionInputMethod inputMethod;
        private final Confidence confidence;
        private final AudioSignalProblems audioSignalProblems;

        public SpeechRecognitionCompletedEventHandler(SpeechRecognitionInputMethod inputMethod, Confidence confidence,
                AudioSignalProblems audioSignalProblems) {
            this.inputMethod = inputMethod;
            this.audioSignalProblems = audioSignalProblems;
            this.confidence = confidence;
        }

        @Override
        public void run(SpeechRecognitionImplementation sender, SpeechRecognizedEventArgs eventArgs) {
            if (eventArgs.result.length == 1) {
                SpeechRecognitionResult result = eventArgs.result[0];
                if (audioSignalProblems.occured()) {
                    logAudioSignalProblem(result);
                } else if (!confidenceIsHighEnough(result, confidence)) {
                    logLackOfConfidence(result);
                } else {
                    inputMethod.signal(result.index);
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
