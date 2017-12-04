package teaselib.core.ui;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import teaselib.core.TeaseScriptBase;
import teaselib.core.events.Event;
import teaselib.core.speechrecognition.SpeechRecognition;
import teaselib.core.speechrecognition.SpeechRecognitionImplementation;
import teaselib.core.speechrecognition.SpeechRecognitionResult;
import teaselib.core.speechrecognition.SpeechRecognitionResult.Confidence;
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

    private final SpeechRecognition speechRecognizer;
    private final Confidence confidence;
    private final Optional<SpeechRecognitionRejectedScript> speechRecognitionRejectedScript;

    private final Event<SpeechRecognitionImplementation, SpeechRecognizedEventArgs> recognitionRejected;
    private final Event<SpeechRecognitionImplementation, SpeechRecognizedEventArgs> recognitionCompleted;

    private final AtomicReference<Prompt> active = new AtomicReference<>();

    public SpeechRecognitionInputMethod(SpeechRecognition speechRecognizer, Confidence recognitionConfidence,
            Optional<SpeechRecognitionRejectedScript> speechRecognitionRejectedScript) {
        this.speechRecognizer = speechRecognizer;
        this.confidence = recognitionConfidence;
        this.speechRecognitionRejectedScript = speechRecognitionRejectedScript;
        this.recognitionRejected = new SpeechRecognitionRejectedEventHandler();
        this.recognitionCompleted = new SpeechRecognitionCompletedEventHandler();
    }

    final class SpeechRecognitionRejectedEventHandler
            implements Event<SpeechRecognitionImplementation, SpeechRecognizedEventArgs> {
        // Handling speech recognition rejected events:
        // RecognitionRejectedEvent-scripts doesn't work in reply-calls that
        // invoke script functions but they work inside script functions.
        //
        // Reason are:
        // - The event handler would have to wait until messages rendered by
        // the script function are completed -> delay in response
        // - script functions may include timing which would be messed up by
        // pausing them
        // - Script functions may invoke other script functions, but the
        // handler management is neither multi-threading-aware nor
        // synchronized
        // - The current code is unable to recover to the choice on top of
        // the choices stack after a recognition-rejected pause event
        //
        // The recognitionRejected handler won't trigger immediately when
        // a script function renders messages, because it will wait until
        // the render queue is empty, and this includes message delays.
        // Therefore script functions are not supported, because the script
        // function would still render messages while the choices are shown.
        // However rendering messages while showing choices should be fine.
        @Override
        public void run(SpeechRecognitionImplementation sender, SpeechRecognizedEventArgs eventArgs) {
            if (speechRecognitionRejectedScript.isPresent() && speechRecognitionRejectedScript.get().canRun()) {
                signal(RECOGNITION_REJECTED);
            }
        }

        private void log(TeaseScriptBase speechRecognitionRejectedScript, String message) {
            if (logger.isInfoEnabled()) {
                String skipping = " - skipping RecognitionRejectedScript " + speechRecognitionRejectedScript.toString();
                logger.info(message + skipping);
            }
        }
    }

    final class SpeechRecognitionCompletedEventHandler
            implements Event<SpeechRecognitionImplementation, SpeechRecognizedEventArgs> {
        @Override
        public void run(SpeechRecognitionImplementation sender, SpeechRecognizedEventArgs eventArgs) {
            if (eventArgs.result.length == 1) {
                SpeechRecognitionResult result = eventArgs.result[0];
                if (confidenceIsHighEnough(result, confidence)) {
                    signal(result.index);
                } else {
                    logger.info("Dropping result '" + result.toString() + "' due to lack of confidence (Confidence="
                            + confidence + " expected)");
                }
            } else {
                logger.info("Ignoring none or more than one result");
            }
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
        speechRecognizer.events.recognitionRejected.add(recognitionRejected);
        speechRecognizer.events.recognitionCompleted.add(recognitionCompleted);
        speechRecognizer.startRecognition(active.get().derived, confidence);
    }

    private void disableSpeechRecognition() {
        logger.debug("Stopping speech recognition");
        speechRecognizer.events.recognitionRejected.remove(recognitionRejected);
        speechRecognizer.events.recognitionCompleted.remove(recognitionCompleted);
        speechRecognizer.stopRecognition();
    }

    @Override
    public Map<String, Runnable> getHandlers() {
        HashMap<String, Runnable> handlers = new HashMap<>();
        handlers.put(RECOGNITION_REJECTED_HNADLER_KEY, new Runnable() {
            @Override
            public void run() {
                if (speechRecognitionRejectedScript.isPresent()) {
                    speechRecognitionRejectedScript.get().run();
                }
            }
        });
        return handlers;
    }

    @Override
    public String toString() {
        return "SpeechRecognizer=" + speechRecognizer.toString() + " confidence=" + confidence;
    }
}
