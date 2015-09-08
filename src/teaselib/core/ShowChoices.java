/**
 * 
 */
package teaselib.core;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

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

    private final List<String> choices;
    private final List<String> derivedChoices;
    private final ScriptFunction scriptFunction;

    private final TeaseLib teaseLib;
    private final SpeechRecognition speechRecognizer;
    private final Confidence recognitionConfidence;
    private final ScriptFutureTask scriptTask;
    private final List<Integer> srChoiceIndices;
    private final boolean recognizeSpeech;
    private final Event<SpeechRecognitionImplementation, SpeechRecognizedEventArgs> recognitionCompleted;

    private final static ExecutorService choiceScriptFunctionExecutor = NamedExecutorService
            .newFixedThreadPool(Integer.MAX_VALUE, ShowChoices.class.getName()
                    + " Script Function", 1, TimeUnit.HOURS);

    private boolean paused = false;

    public ShowChoices(TeaseScriptBase script, List<String> choices,
            List<String> derivedChoices, ScriptFunction scriptFunction,
            Confidence recognitionConfidence) {
        super();
        this.choices = choices;
        this.derivedChoices = derivedChoices;
        this.scriptFunction = scriptFunction;
        this.teaseLib = script.teaseLib;
        this.speechRecognizer = SpeechRecognizer.instance
                .get(script.actor.locale);
        this.recognitionConfidence = recognitionConfidence;
        if (scriptFunction != null) {
            // The result of this future task is never queried for,
            // instead a timeout is signaled via the TimeoutClick class
            scriptTask = new ScriptFutureTask(script, scriptFunction,
                    derivedChoices, new ScriptFutureTask.TimeoutClick());
        } else {
            scriptTask = null;
        }
        // Start SR first, otherwise there would be a race condition between
        // this thread and the script function when displaying/speaking a
        // message in the script function, causing the display of the choices to
        // be delayed, as starting SR waits for TTS to complete
        recognizeSpeech = speechRecognizer.isReady();
        if (recognizeSpeech) {
            srChoiceIndices = new ArrayList<Integer>(1);
            recognitionCompleted = recognitionCompletedEvent(derivedChoices,
                    scriptTask, srChoiceIndices);
            enableSpeechRecognition();
        } else {
            srChoiceIndices = null;
            recognitionCompleted = null;
        }
        // Now we can start the script task
        if (scriptTask != null) {
            // Start the script task right away
            choiceScriptFunctionExecutor.execute(scriptTask);
        }
    }

    public String show() {
        if (recognizeSpeech
                && !speechRecognizer.events.recognitionCompleted
                        .contains(recognitionCompleted)) {
            enableSpeechRecognition();
        }
        // Get the user's choice
        int choiceIndex;
        try {
            choiceIndex = teaseLib.host.reply(derivedChoices);
        } finally {
            if (!paused) {
                if (scriptTask != null) {
                    if (scriptTask.isDone()) {
                        TeaseLib.logDetail("choose: script task finished");
                    } else {
                        TeaseLib.logDetail("choose: Cancelling script task");
                        scriptTask.cancel(true);
                    }
                }
            }
            if (recognizeSpeech) {
                disableSpeechRecognition();
            }
        }
        if (paused) {
            synchronized (this) {
                notifyAll();
            }
            return Paused;
        } else {
            // Wait for the script task to end
            if (scriptTask != null) {
                scriptTask.join();
            }
        }
        // The result of the script function may override any result
        // from button clicks or speech recognition
        String choice = scriptFunction != null ? scriptFunction.result
                : ScriptFunction.Finished;
        if (choice == ScriptFunction.Finished) {
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
                // speech
                // recognition result, then it's a simple button click
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
        TeaseLib.logDetail("Stopping speech recognition");
        speechRecognizer.events.recognitionCompleted
                .remove(recognitionCompleted);
        speechRecognizer.stopRecognition();
    }

    /**
     * Dismiss clickables but keep script function running
     */
    public void pause() {
        synchronized (this) {
            paused = true;
            // Must wait until there is something to pause:
            // The main script thread starts the script function,
            // the script function runs to here and tries to pause
            // before the main script has realized the buttons
            // - the main thread waits 300ms for script function renderers to
            // complete their starts
            // - even when resolving this, we still don't have a synchronization
            // object that fires after realizing the buttons
            try {
                while (!teaseLib.host.dismissChoices(derivedChoices)) {
                    teaseLib.sleep(100, TimeUnit.MILLISECONDS);
                }
            } catch (Exception e) {
                TeaseLib.logDetail(this, e);
            }
            try {
                wait();
            } catch (InterruptedException e) {
                throw new ScriptInterruptedException();
            } finally {
                paused = false;
            }
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
                        TeaseLib.log("Clicked delegate for '"
                                + speechRecognitionResult.text + "' index="
                                + speechRecognitionResult.index);
                    } else {
                        TeaseLib.log("Button gone for choice "
                                + speechRecognitionResult.index + ": "
                                + speechRecognitionResult.text);
                    }
                } catch (Throwable t) {
                    TeaseLib.log(this, t);
                }
            }
        };
        return recognitionCompleted;
    }
}
