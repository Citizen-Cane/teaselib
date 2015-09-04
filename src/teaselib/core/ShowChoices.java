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
import teaselib.core.speechrecognition.SpeechRecognizer;
import teaselib.core.speechrecognition.events.SpeechRecognizedEventArgs;
import teaselib.core.util.NamedExecutorService;

/**
 *
 */
class ShowChoices {
    final List<String> choices;
    final List<String> derivedChoices;
    final ScriptFunction scriptFunction;

    final TeaseLib teaseLib;
    final SpeechRecognition speechRecognizer;

    final ScriptFutureTask scriptTask;
    final List<Integer> srChoiceIndices = new ArrayList<Integer>(1);
    final boolean recognizeSpeech;
    final Event<SpeechRecognitionImplementation, SpeechRecognizedEventArgs> recognitionCompleted;

    private ExecutorService choiceScriptFunctionExecutor = NamedExecutorService
            .newFixedThreadPool(1, getClass().getName() + " Script Function",
                    1, TimeUnit.SECONDS);

    public ShowChoices(TeaseScriptBase script, List<String> choices,
            List<String> derivedChoices, ScriptFunction scriptFunction) {
        super();
        this.choices = choices;
        this.derivedChoices = derivedChoices;
        this.scriptFunction = scriptFunction;
        this.teaseLib = script.teaseLib;
        this.speechRecognizer = SpeechRecognizer.instance
                .get(script.actor.locale);

        if (scriptFunction != null) {
            // The result of this future task is never queried for,
            // instead a timeout is signaled via the TimeoutClick class
            scriptTask = new ScriptFutureTask(script, scriptFunction,
                    derivedChoices, new ScriptFutureTask.TimeoutClick());
            // Start the script task right away
            choiceScriptFunctionExecutor.execute(scriptTask);
            script.completeStarts();
            // TODO completeStarts() doesn't work because first we need to
            // wait for render threads completing their starts
            // Workaround: A bit unsatisfying, but
            // otherwise the choice buttons would appear too early
            teaseLib.sleep(300, TimeUnit.MILLISECONDS);
        } else {
            scriptTask = null;
        }
        recognizeSpeech = speechRecognizer.isReady();
        recognitionCompleted = recognizeSpeech ? recognitionCompletedEvent(
                derivedChoices, scriptTask, srChoiceIndices) : null;
    }

    public String show() {
        if (recognizeSpeech) {
            speechRecognizer.events.recognitionCompleted
                    .add(recognitionCompleted);
            speechRecognizer.startRecognition(derivedChoices);
        }
        // Get the user's choice
        int choiceIndex;
        try {
            choiceIndex = teaseLib.host.reply(derivedChoices);
        } finally {
            if (scriptTask != null) {
                if (scriptTask.isDone()) {
                    TeaseLib.logDetail("choose: script task finished");
                } else {
                    TeaseLib.logDetail("choose: Cancelling script task");
                    scriptTask.cancel(true);
                }
            }
            if (recognizeSpeech) {
                TeaseLib.logDetail("choose: stopping speech recognition");
                speechRecognizer.events.recognitionCompleted
                        .remove(recognitionCompleted);
                speechRecognizer.stopRecognition();
            }
        }
        // Wait for the script task to end
        if (scriptTask != null) {
            scriptTask.join();
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
