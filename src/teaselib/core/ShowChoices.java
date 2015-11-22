/**
 * 
 */
package teaselib.core;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

import teaselib.ScriptFunction;
import teaselib.TeaseLib;
import teaselib.core.events.Delegate;
import teaselib.core.events.Event;
import teaselib.core.speechrecognition.SpeechRecognition;
import teaselib.core.speechrecognition.SpeechRecognitionImplementation;
import teaselib.core.speechrecognition.SpeechRecognitionResult;
import teaselib.core.speechrecognition.SpeechRecognitionResult.Confidence;
import teaselib.core.speechrecognition.SpeechRecognizer;
import teaselib.core.speechrecognition.events.SpeechRecognizedEventArgs;
import teaselib.core.util.NamedExecutorService;

/**
 *
 */
class ShowChoices {
    public final static String Paused = "Paused";
    public final static String RecognitionRejected = "Recognition Rejected";

    public final List<String> choices;
    public final List<String> derivedChoices;
    private final ScriptFutureTask scriptTask;

    private final TeaseLib teaseLib;
    private final SpeechRecognition speechRecognizer;
    private final Confidence recognitionConfidence;
    private final List<Integer> srChoiceIndices;
    private final boolean recognizeSpeech;
    private final Event<SpeechRecognitionImplementation, SpeechRecognizedEventArgs> recognitionCompleted;

    private final static ExecutorService choiceScriptFunctionExecutor = NamedExecutorService
            .newFixedThreadPool(Integer.MAX_VALUE, ShowChoices.class.getName()
                    + " Script Function", 1, TimeUnit.HOURS);

    private boolean scriptTaskStarted = false;
    private final ReentrantLock pauseSync = new ReentrantLock();
    private volatile boolean paused = false;
    private volatile String reason = null;

    public ShowChoices(TeaseScriptBase script, List<String> choices,
            List<String> derivedChoices, ScriptFutureTask scriptTask,
            Confidence recognitionConfidence) {
        super();
        this.choices = choices;
        this.derivedChoices = derivedChoices;
        this.scriptTask = scriptTask;
        this.teaseLib = script.teaseLib;
        this.speechRecognizer = SpeechRecognizer.instance
                .get(script.actor.locale);
        this.recognitionConfidence = recognitionConfidence;
        recognizeSpeech = speechRecognizer.isReady();
        if (recognizeSpeech) {
            srChoiceIndices = new ArrayList<Integer>(1);
            recognitionCompleted = recognitionCompletedEvent(derivedChoices,
                    scriptTask, srChoiceIndices);
        } else {
            srChoiceIndices = null;
            recognitionCompleted = null;
        }
    }

    public String show() {
        teaseLib.log.info("Showing " + derivedChoices.toString());
        // Start SR first, otherwise there would be a race condition between
        // this thread and the script function when displaying/speaking a
        // message in the script function, causing the display of the choices to
        // be delayed, as starting SR waits for TTS to complete
        if (recognizeSpeech) {
            enableSpeechRecognition();
        }
        // Now we can start the script task
        if (scriptTask != null && !scriptTaskStarted) {
            // The result of this future task is never queried for,
            // instead a timeout is signaled via the TimeoutClick class,
            // or the script function can return a specific result
            choiceScriptFunctionExecutor.execute(scriptTask);
            scriptTaskStarted = true;
        }
        // Get the user's choice
        int choiceIndex;
        try {
            // We can just set pause to false here
            paused = false;
            choiceIndex = teaseLib.host.reply(derivedChoices);
        } finally {
            boolean stopScriptTask = !paused && scriptTask != null;
            if (stopScriptTask) {
                if (scriptTask.isDone()) {
                    teaseLib.log
                            .debug("choose: script task finished");
                } else {
                    teaseLib.log
                            .debug("choose: Cancelling script task");
                    scriptTask.cancel(true);
                }
            }
            if (recognizeSpeech) {
                disableSpeechRecognition();
            }
            if (stopScriptTask) {
                // Wait for the script task to end
                scriptTask.join();
            }
            // Sync on the pause state set by pause()
            pauseSync.lock();
            try {
                if (paused) {
                    teaseLib.log
                            .info("Entering pause state with reason " + reason);
                    return reason;
                }
            } finally {
                pauseSync.unlock();
            }
        }
        // The result of the script function may override any result
        // from button clicks or speech recognition
        String choice = scriptTask != null ? scriptTask
                .getScriptFunctionResult() : null;
        if (choice == null) {
            // Assign result from speech recognition,
            // script task timeout or button click
            if (!srChoiceIndices.isEmpty()) {
                // Use first speech recognition result
                choice = choices.get(srChoiceIndices.get(0));
            } else if (scriptTask != null && scriptTask.timedOut()) {
                // Timeout
                choice = ScriptFunction.Timeout;
            } else {
                // If the script function didn't timeout and there is no
                // speech recognition result, then it's a simple button click
                choice = choices.get(choiceIndex);
            }
        }
        return choice;
    }

    private void enableSpeechRecognition() {
        speechRecognizer.events.recognitionCompleted.add(recognitionCompleted);
        speechRecognizer
                .startRecognition(derivedChoices, recognitionConfidence);
    }

    private void disableSpeechRecognition() {
        teaseLib.log.debug("Stopping speech recognition");
        speechRecognizer.events.recognitionCompleted
                .remove(recognitionCompleted);
        speechRecognizer.stopRecognition();
    }

    /**
     * Dismiss clickables but keep script function running
     */

    // TODO Avoid deadlock pause() in speech-rejected-script-handler waiting for
    // a notify, while showChoices wants to remove its rejected-handler
    // - both sync on the EventSource for SR-Rejected-Events
    // It seems that the choice was somehow considered as a valid choice,
    // instead of a pause -> using volatile for signaling
    // This issue needs to be observed (Mine has a SR Rejected script handler)

    public void pause(String reason) {
        if (!paused) {
            pauseSync.lock();
            try {
                this.paused = true;
                this.reason = reason;
                teaseLib.log.info("Pausing "
                        + derivedChoices.toString());
                // Must wait until there is something to pause, because
                // the buttons are realized by the host, and therefore
                // we don't have a synchronization object that defines a
                // before-after relationship for that situation.
                // The main script thread starts the script function,
                // and the script function may run to here trying to pause the
                // main thread before the main script has realized any buttons.
                try {
                    while (!teaseLib.host.dismissChoices(derivedChoices)) {
                        teaseLib.sleep(100, TimeUnit.MILLISECONDS);
                    }
                } catch (ScriptInterruptedException e) {
                    throw new ScriptInterruptedException();
                } catch (Exception e) {
                    teaseLib.log.debug(this, e);
                }
            } finally {
                pauseSync.unlock();
            }
            // Keep the pause status until the choices are about to be
            // realized again in the user interface
        } else {
            teaseLib.log.info("Paused aready "
                    + derivedChoices.toString());
        }
    }

    private Event<SpeechRecognitionImplementation, SpeechRecognizedEventArgs> recognitionCompletedEvent(
            final List<String> derivedChoices,
            final ScriptFutureTask scriptTask,
            final List<Integer> srChoiceIndices) {
        Event<SpeechRecognitionImplementation, SpeechRecognizedEventArgs> recognitionCompleted;
        recognitionCompleted = new Event<SpeechRecognitionImplementation, SpeechRecognizedEventArgs>() {
            @Override
            public void run(SpeechRecognitionImplementation sender,
                    SpeechRecognizedEventArgs eventArgs) {
                if (eventArgs.result.length == 1) {
                    // Find the button to click
                    SpeechRecognitionResult speechRecognitionResult = eventArgs.result[0];
                    if (!speechRecognitionResult.isChoice(derivedChoices)) {
                        throw new IllegalArgumentException(
                                speechRecognitionResult.toString());
                    }
                    // Assign the result even if the buttons have been
                    // unrealized
                    srChoiceIndices.add(speechRecognitionResult.index);
                    clickChoice(derivedChoices, scriptTask,
                            speechRecognitionResult);
                } else {
                    // none or more than one result means incorrect
                    // recognition
                }
            }

            private void clickChoice(final List<String> derivedChoices,
                    final ScriptFutureTask scriptTask,
                    SpeechRecognitionResult speechRecognitionResult) {
                List<Delegate> uiElements = teaseLib.host
                        .getClickableChoices(derivedChoices);
                try {
                    Delegate delegate = uiElements
                            .get(speechRecognitionResult.index);
                    if (delegate != null) {
                        if (scriptTask != null) {
                            scriptTask.cancel(true);
                        }
                        // Click the button
                        delegate.run();
                        teaseLib.log.info("Clicked delegate for '"
                                + speechRecognitionResult.text + "' index="
                                + speechRecognitionResult.index);
                    } else {
                        teaseLib.log.info("Button gone for choice "
                                + speechRecognitionResult.index + ": "
                                + speechRecognitionResult.text);
                    }
                } catch (Throwable t) {
                    teaseLib.log.error(this, t);
                }
            }
        };
        return recognitionCompleted;
    }
}
