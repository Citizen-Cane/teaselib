package teaselib.core.ui;

import java.util.concurrent.atomic.AtomicReference;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import teaselib.core.events.Event;
import teaselib.core.speechrecognition.SpeechRecognition;
import teaselib.core.speechrecognition.SpeechRecognitionImplementation;
import teaselib.core.speechrecognition.SpeechRecognitionResult;
import teaselib.core.speechrecognition.SpeechRecognitionResult.Confidence;
import teaselib.core.speechrecognition.events.SpeechRecognizedEventArgs;
import teaselib.core.ui.PromptQueue.Todo;

/**
 * @author Citizen-Cane
 *
 */

public class SpeechRecognitionInputMethod implements InputMethod {
    private static final Logger logger = LoggerFactory.getLogger(SpeechRecognitionInputMethod.class);

    private final SpeechRecognition speechRecognizer;
    private final Confidence recognitionConfidence;

    private final Event<SpeechRecognitionImplementation, SpeechRecognizedEventArgs> recognitionRejected;
    private Event<SpeechRecognitionImplementation, SpeechRecognizedEventArgs> recognitionCompleted;

    private final AtomicReference<Todo> active = new AtomicReference<Todo>();

    public SpeechRecognitionInputMethod(SpeechRecognition speechRecognizer, final Confidence recognitionConfidence) {
        this.speechRecognizer = speechRecognizer;
        this.recognitionConfidence = recognitionConfidence;
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
        recognitionRejected = new Event<SpeechRecognitionImplementation, SpeechRecognizedEventArgs>() {
            @Override
            public void run(SpeechRecognitionImplementation sender, SpeechRecognizedEventArgs eventArgs) {
                // SpeechRecognitionRejectedScript
                // speechRecognitionRejectedScript =
                // script.actor.speechRecognitionRejectedScript;
                // // run speech recognition rejected script?
                // if (speechRecognitionRejectedScript != null) {
                // if (choicesStackContainsSRRejectedState == true) {
                // logger.info("The choices stack contains already another
                // SR rejection script"
                // + " - skipping RecognitionRejectedScript "
                // + speechRecognitionRejectedScript.toString());
                // } else if (scriptTask != null) {
                // // This would work for the built-in confirmative
                // // timeout script functions
                // // TimeoutBehavior.InDubioMitius and maybe also for
                // // TimeoutBehavior.TimeoutBehavior.InDubioMitius
                // logger.info(scriptTask.getRelation().toString() + "
                // script functions running"
                // + " - skipping RecognitionRejectedScript "
                // + speechRecognitionRejectedScript.toString());
                // } else if (!teaseLib.renderQueue.hasCompletedAll()) {
                // // must complete all to avoid parallel rendering
                // // see {@link Message#ShowChoices}
                // logger.info(
                // " message rendering still in progress" + " - skipping
                // RecognitionRejectedScript "
                // + speechRecognitionRejectedScript.toString());
                // } else if (speechRecognitionRejectedScript.canRun() ==
                // false) {
                // logger.info("RecognitionRejectedScript " +
                // speechRecognitionRejectedScript.toString()
                // + ".canRun() returned false - skipping");
                // } else {
                // // all negative conditions sorted out
                // pause(ShowChoices.RecognitionRejected);
                // }
                // }
            }
        };
        recognitionCompleted = new Event<SpeechRecognitionImplementation, SpeechRecognizedEventArgs>() {
            @Override
            public void run(SpeechRecognitionImplementation sender, SpeechRecognizedEventArgs eventArgs) {
                synchronized (SpeechRecognitionInputMethod.this) {
                    if (eventArgs.result.length == 1) {
                        SpeechRecognitionResult result = eventArgs.result[0];
                        if (confidenceIsHighEnough(result, recognitionConfidence)) {
                            Prompt prompt = active.get().prompt;
                            synchronized (prompt) {
                                active.get().setResultOnce(result.index);
                                SpeechRecognitionInputMethod.this.notifyAll();
                                prompt.lock.lock();
                                try {
                                    if (active.get().paused.get() == false) {
                                        prompt.click.signalAll();
                                    }
                                } finally {
                                    prompt.lock.unlock();
                                }
                            }
                        } else {
                            logger.info(
                                    "Dropping result '" + result.toString() + "' due to lack of confidence (Confidence="
                                            + recognitionConfidence + " expected)");
                        }
                    } else {
                        logger.info("Ignoring none or more than one result");
                    }
                }
            }

            private boolean confidenceIsHighEnough(SpeechRecognitionResult result, Confidence confidence) {
                return result.confidence.propability >= confidence.propability;
            }
        };
    }

    @Override
    public void show(final Todo todo) {
        active.set(todo);

        enableSpeechRecognition();
    }

    @Override
    public boolean dismiss(Prompt prompt) throws InterruptedException {
        Todo todo = active.get();
        if (todo != null) {
            boolean dismissed = todo.result() == Prompt.UNDEFINED;
            disableSpeechRecognition();
            active.set(null);
            return dismissed;
        } else {
            return false;
        }
    }

    private void enableSpeechRecognition() {
        speechRecognizer.events.recognitionRejected.add(recognitionRejected);
        speechRecognizer.events.recognitionCompleted.add(recognitionCompleted);
        speechRecognizer.startRecognition(active.get().prompt.derived, recognitionConfidence);
    }

    private void disableSpeechRecognition() {
        logger.debug("Stopping speech recognition");
        speechRecognizer.events.recognitionRejected.remove(recognitionRejected);
        speechRecognizer.events.recognitionCompleted.remove(recognitionCompleted);
        speechRecognizer.stopRecognition();
    }

}
