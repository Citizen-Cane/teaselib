package teaselib.core.ui;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import teaselib.Replay;
import teaselib.core.ScriptFutureTask;
import teaselib.core.TeaseScriptBase;
import teaselib.core.events.Event;
import teaselib.core.speechrecognition.SpeechRecognition;
import teaselib.core.speechrecognition.SpeechRecognitionImplementation;
import teaselib.core.speechrecognition.SpeechRecognitionResult;
import teaselib.core.speechrecognition.SpeechRecognitionResult.Confidence;
import teaselib.core.speechrecognition.events.SpeechRecognizedEventArgs;
import teaselib.core.ui.PromptQueue.Todo;
import teaselib.util.SpeechRecognitionRejectedScript;

/**
 * @author Citizen-Cane
 *
 */

public class SpeechRecognitionInputMethod implements InputMethod {

    private static final Logger logger = LoggerFactory.getLogger(SpeechRecognitionInputMethod.class);

    private static final int RECOGNITION_REJECTED = -666;
    private static final String RECOGNITION_REJECTED_HNADLER_KEY = "Recognition Rejected";

    private final TeaseScriptBase script;
    private final SpeechRecognition speechRecognizer;
    private final Confidence recognitionConfidence;

    private final Event<SpeechRecognitionImplementation, SpeechRecognizedEventArgs> recognitionRejected;
    private Event<SpeechRecognitionImplementation, SpeechRecognizedEventArgs> recognitionCompleted;

    private final AtomicReference<Todo> active = new AtomicReference<Todo>();

    public SpeechRecognitionInputMethod(final TeaseScriptBase script, SpeechRecognition speechRecognizer,
            final Confidence recognitionConfidence) {
        this.script = script;
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
                if (runSpeechRecognitionRejectedScript(script)) {
                    signal(RECOGNITION_REJECTED);
                }
            }

            private boolean runSpeechRecognitionRejectedScript(final TeaseScriptBase script) {
                SpeechRecognitionRejectedScript speechRecognitionRejectedScript = script.actor.speechRecognitionRejectedScript;
                // run speech recognition rejected script?
                if (speechRecognitionRejectedScript != null) {
                    Todo todo = active.get();
                    Prompt prompt = todo.prompt;
                    ScriptFutureTask scriptTask = prompt.scriptTask;

                    // TODO Must search pause stack for speech recognition
                    // rejected scripts - handlers of the same type shouldn't
                    // stack up -> handle in Shower
                    boolean choicesStackContainsSRRejectedState = false;

                    if (choicesStackContainsSRRejectedState == true) {
                        logger.info("The choices stack contains already another SR rejection script"
                                + " - skipping RecognitionRejectedScript "
                                + speechRecognitionRejectedScript.toString());
                    } else if (scriptTask != null) {
                        // This would work for the built-in confirmative
                        // timeout script functions:
                        // - TimeoutBehavior.InDubioMitius and maybe also for
                        // - TimeoutBehavior.TimeoutBehavior.InDubioMitius
                        logger.info(scriptTask.getRelation().toString() + " script functions running"
                                + " - skipping RecognitionRejectedScript "
                                + speechRecognitionRejectedScript.toString());
                    } else if (!script.teaseLib.renderQueue.hasCompletedMandatory()) {
                        // must complete all to avoid parallel rendering
                        // see {@link Message#ShowChoices}
                        logger.info(" message rendering still in progress" + " - skipping RecognitionRejectedScript "
                                + speechRecognitionRejectedScript.toString());
                    } else if (speechRecognitionRejectedScript.canRun() == false) {
                        logger.info("RecognitionRejectedScript " + speechRecognitionRejectedScript.toString()
                                + ".canRun() returned false - skipping");
                    } else {
                        return true;
                    }
                }
                return false;
            }
        };
        recognitionCompleted = new Event<SpeechRecognitionImplementation, SpeechRecognizedEventArgs>() {
            @Override
            public void run(SpeechRecognitionImplementation sender, SpeechRecognizedEventArgs eventArgs) {
                synchronized (SpeechRecognitionInputMethod.this) {
                    if (eventArgs.result.length == 1) {
                        SpeechRecognitionResult result = eventArgs.result[0];
                        if (confidenceIsHighEnough(result, recognitionConfidence)) {
                            signal(result.index);
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

    private void signal(int resultIndex) {
        Todo todo = active.get();
        Prompt prompt = todo.prompt;
        synchronized (prompt) {
            if (resultIndex == RECOGNITION_REJECTED) {
                todo.prompt.inputHandlerKey = RECOGNITION_REJECTED_HNADLER_KEY;
            } else {
                todo.setResultOnce(resultIndex);
            }
            // TODO Needed - check with prompt queue
            synchronized (SpeechRecognitionInputMethod.this) {
                SpeechRecognitionInputMethod.this.notifyAll();
            }
            prompt.lock.lock();
            try {
                if (todo.paused.get() == false) {
                    prompt.click.signalAll();
                }
            } finally {
                prompt.lock.unlock();
            }
        }
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

    @Override
    public Map<String, Runnable> getHandlers() {
        HashMap<String, Runnable> handlers = new HashMap<String, Runnable>();
        handlers.put(RECOGNITION_REJECTED_HNADLER_KEY, new Runnable() {

            @Override
            public void run() {
                script.endAll();
                Replay beforeSpeechRecognitionRejected = script.getReplay();
                script.actor.speechRecognitionRejectedScript.run();
                beforeSpeechRecognitionRejected.replay(Replay.Position.End);
            }
        });
        return handlers;
    }

}
