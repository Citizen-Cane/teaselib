package teaselib;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;

import teaselib.image.ImageIterator;
import teaselib.speechrecognition.SpeechRecognition;
import teaselib.speechrecognition.SpeechRecognitionImplementation;
import teaselib.speechrecognition.SpeechRecognitionResult;
import teaselib.speechrecognition.events.SpeechRecognizedEventArgs;
import teaselib.text.Message;
import teaselib.text.RenderMessage;
import teaselib.texttospeech.TextToSpeech;
import teaselib.texttospeech.TextToSpeechPlayer;
import teaselib.userinterface.MediaRenderer;
import teaselib.userinterface.MediaRendererQueue;
import teaselib.util.Delegate;
import teaselib.util.Event;

public abstract class TeaseScriptBase {

    public final TeaseLib teaseLib;

    protected final TextToSpeechPlayer speechSynthesizer;
    protected final SpeechRecognition speechRecognizer;

    protected final MediaRendererQueue renderQueue = new MediaRendererQueue();
    protected final Deque<MediaRenderer> deferredRenderers = new ArrayDeque<MediaRenderer>();

    public static final String Timeout = "Timeout";

    public TeaseScriptBase(TeaseLib teaseLib, String locale) {
        this.teaseLib = teaseLib;
        speechRecognizer = new SpeechRecognition(locale);
        speechSynthesizer = new TextToSpeechPlayer(teaseLib.resources,
                teaseLib.host, new TextToSpeech(), speechRecognizer);
    }

    /**
     * Just wait for everything to be rendered (messages displayed, sounds
     * played, delay expired), and continue execution of the script. This won't
     * display a button, it just waits.
     */
    public void completeAll() {
        renderQueue.completeAll();
    }

    /**
     * Workaround as of now because PCMPlayer must display the stop button
     * immediately
     */
    public void completeMandatory() {
        renderQueue.completeMandatories();
    }

    public void completeStarts() {
        renderQueue.completeStarts();
    }

    public void renderMessage(Message message,
            TextToSpeechPlayer speechSynthesizer, ImageIterator dominantImages,
            String displayImage, String mood) {
        renderDeferred();
        Set<String> hints = getHints(mood);
        RenderMessage renderMessage = new RenderMessage(message,
                speechSynthesizer, dominantImages, displayImage, hints);
        renderQueue.start(renderMessage, teaseLib);
        renderQueue.completeStarts();
    }

    private Set<String> getHints(String mood) {
        Set<String> hints = new HashSet<String>();
        // Within messages, images might change fast, and changing
        // the camera position, image size or aspect would be too distracting
        hints.add(ImageIterator.SameCameraPosition);
        hints.add(ImageIterator.SameResolution);
        hints.add(mood);
        return hints;
    }

    private void renderDeferred() {
        completeAll();
        renderQueue.start(deferredRenderers, teaseLib);
        deferredRenderers.clear();
    }

    private class TimeoutClick {
        public boolean clicked = false;
    }

    public String showChoices(final Runnable scriptFunction,
            final List<String> choices) {
        // arguments check
        for (String choice : choices) {
            if (choice == null) {
                throw new IllegalArgumentException("Choice may not be null");
            }
        }
        TeaseLib.log("choose: " + choices.toString());
        // Script closure
        final TimeoutClick timeoutClick = new TimeoutClick();
        final FutureTask<String> scriptTask = scriptFunction == null ? null
                : new FutureTask<String>(new Callable<String>() {
                    @Override
                    public String call() throws Exception {
                        try {
                            scriptFunction.run();
                            // Keep choices available until the last part of
                            // the script function has finished rendering
                            completeAll();
                        } catch (ScriptInterruptedException e) {
                            return null;
                        }
                        // Script function finished, click any and return
                        // timeout
                        List<Delegate> clickables = teaseLib.host
                                .getClickableChoices(choices);
                        if (!clickables.isEmpty()) {
                            Delegate clickable = clickables.get(0);
                            if (clickable != null) {
                                // Flag timeout and click any button
                                timeoutClick.clicked = true;
                                clickables.get(0).run();
                            } else {
                                // Host implementation is incomplete
                                new IllegalStateException(
                                        "Host didn't return clickables for choices: "
                                                + choices.toString());
                            }
                        }
                        return TeaseScript.Timeout;
                    }
                });
        // Speech recognition
        final List<Integer> srChoiceIndices = new ArrayList<Integer>(1);
        final Event<SpeechRecognitionImplementation, SpeechRecognizedEventArgs> speechRecognizedEvent;
        if (speechRecognizer.isReady()) {
            speechRecognizedEvent = new Event<SpeechRecognitionImplementation, SpeechRecognizedEventArgs>() {
                @Override
                public void run(SpeechRecognitionImplementation sender,
                        SpeechRecognizedEventArgs eventArgs) {
                    if (eventArgs.result.length == 1) {
                        List<Delegate> uiElements = teaseLib.host
                                .getClickableChoices(choices);
                        // Find the button to click
                        SpeechRecognitionResult speechRecognitionResult = eventArgs.result[0];
                        if (speechRecognitionResult.isChoice(choices)) {
                            // This assigns the result even if the buttons have
                            // unrealized
                            int choice = speechRecognitionResult.index;
                            srChoiceIndices.add(choice);
                            try {
                                Delegate delegate = uiElements.get(choice);
                                if (delegate != null) {
                                    if (scriptTask != null) {
                                        scriptTask.cancel(true);
                                    }
                                    // Click the button
                                    timeoutClick.clicked = false;
                                    delegate.run();
                                } else {
                                    TeaseLib.log("Button gone for choice "
                                            + choice + ": "
                                            + speechRecognitionResult.text);
                                }
                            } catch (Throwable t) {
                                TeaseLib.log(this, t);
                            }
                        }
                    } else {
                        // TODO none or more than one result means incorrect
                        // recognition
                    }
                }
            };
            speechRecognizer.events.recognitionCompleted
                    .add(speechRecognizedEvent);
            speechRecognizer.startRecognition(choices);
        } else {
            speechRecognizedEvent = null;
        }
        // Get the user's choice
        int choiceIndex;
        try {
            if (scriptTask != null) {
                ExecutorService executor = Executors.newFixedThreadPool(1);
                executor.execute(scriptTask);
                renderQueue.completeStarts();
                // TODO completeStarts() doesn't work because first we need to
                // wait for render threads that can be completed
                // Workaround: A bit unsatisfying, but otherwise the choice
                // buttons would appear too early
                teaseLib.host.sleep(300);
            }
            choiceIndex = teaseLib.host.choose(choices);
            if (scriptTask != null) {
                // TODO Doesn't always work:
                // The stop is sometimes applied only
                // after the delay renderer has finished
                TeaseLib.logDetail("choose: Cancelling script task");
                scriptTask.cancel(true);
            }
            if (speechRecognizer.isReady()) {
                TeaseLib.logDetail("choose: completing speech recognition");
                speechRecognizer.completeSpeechRecognitionInProgress();
            }
        } finally {
            if (speechRecognizer.isReady()) {
                TeaseLib.logDetail("choose: stopping speech recognition");
                speechRecognizer.stopRecognition();
                speechRecognizer.events.recognitionCompleted
                        .remove(speechRecognizedEvent);
            }
        }
        // Assign result from speech recognition,
        // script task timeout or button click
        String choice = null;
        if (!srChoiceIndices.isEmpty()) {
            // Use the first speech recognition result
            choiceIndex = srChoiceIndices.get(0);
            choice = choices.get(choiceIndex);
        } else if (timeoutClick.clicked) {
            choice = Timeout;
        }
        TeaseLib.logDetail("choose: ending render queue");
        renderQueue.endAll();
        if (choice == null) {
            choice = choices.get(choiceIndex);
        }
        return choice;
    }
}
