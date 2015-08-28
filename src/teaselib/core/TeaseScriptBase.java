package teaselib.core;

import java.io.IOException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

import teaselib.Actor;
import teaselib.Message;
import teaselib.Mood;
import teaselib.ScriptFunction;
import teaselib.TeaseLib;
import teaselib.core.events.Delegate;
import teaselib.core.events.Event;
import teaselib.core.speechrecognition.SpeechRecognition;
import teaselib.core.speechrecognition.SpeechRecognitionImplementation;
import teaselib.core.speechrecognition.SpeechRecognitionResult;
import teaselib.core.speechrecognition.SpeechRecognizer;
import teaselib.core.speechrecognition.events.SpeechRecognizedEventArgs;
import teaselib.core.texttospeech.TextToSpeechPlayer;
import teaselib.core.util.NamedExecutorService;

public abstract class TeaseScriptBase {

    public final TeaseLib teaseLib;
    public final ResourceLoader resources;

    public final Actor actor;
    public final String namespace;

    protected String mood = Mood.Neutral;
    protected String displayImage = Message.DominantImage;

    protected static final int NoTimeout = 0;

    private static final MediaRendererQueue renderQueue = new MediaRendererQueue();
    private final Deque<MediaRenderer> queuedRenderers = new ArrayDeque<MediaRenderer>();

    private ExecutorService choiceScriptFunctionExecutor = NamedExecutorService
            .newFixedThreadPool(1, getClass().getName() + " Script Function",
                    1, TimeUnit.SECONDS);

    /**
     * Construct a new script instance
     * 
     * @param teaseLib
     * @param locale
     */
    protected TeaseScriptBase(TeaseLib teaseLib, ResourceLoader resources,
            Actor actor, String namespace) {
        this.teaseLib = teaseLib;
        this.resources = resources;
        this.actor = actor;
        this.namespace = namespace;
        acquireVoice(actor);
    }

    /**
     * Construct a script with a different actor but with shared resources
     * 
     * @param script
     * @param actor
     */
    protected TeaseScriptBase(TeaseScriptBase script, Actor actor) {
        this.teaseLib = script.teaseLib;
        this.resources = script.resources;
        this.actor = actor;
        this.namespace = script.namespace;
        acquireVoice(actor);
    }

    private void acquireVoice(Actor actor) {
        try {
            TextToSpeechPlayer.instance().selectVoice(resources,
                    new Message(actor));
        } catch (IOException e) {
            TeaseLib.log(this, e);
        }
    }

    protected static List<String> buildChoicesFromArray(String choice,
            String... more) {
        List<String> choices = new ArrayList<String>(1 + more.length);
        choices.add(choice);
        choices.addAll(Arrays.asList(more));
        return choices;
    }

    public void completeStarts() {
        renderQueue.completeStarts();
    }

    public void completeMandatory() {
        renderQueue.completeMandatories();
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
     * Stop rendering and end all render threads
     */
    public void endAll() {
        renderQueue.endAll();
        clearQueuedRenderers();
    }

    protected void renderMessage(Message message,
            TextToSpeechPlayer speechSynthesizer) {
        try {
            renderMessage(message, speechSynthesizer, displayImage, mood);
        } finally {
            displayImage = Message.DominantImage;
            mood = Mood.Neutral;
        }
    }

    protected void renderMessage(Message message,
            TextToSpeechPlayer speechSynthesizer, String displayImage,
            String mood) {
        synchronized (renderQueue) {
            // Clone the actor to prevent the wrong actor image to be displayed
            // when changing the actor images right after saying a message.
            // Without cloning one of the new actor images would be displayed
            // with the current message because the actor is shared between
            // script and message
            Message parsedMessage = new Message(new Actor(message.actor));
            // Replace text variables
            for (Message.Part part : message.getParts()) {
                if (part.type == Message.Type.Text) {
                    parsedMessage.add(parsedMessage.new Part(part.type,
                            replaceVariables(part.value)));
                } else {
                    parsedMessage.add(part);
                }
            }
            Set<String> hints = getHints();
            hints.add(mood);
            RenderMessage renderMessage = new RenderMessage(resources,
                    parsedMessage, speechSynthesizer, displayImage, hints);
            queueRenderer(renderMessage);
            startQueuedRenderers();
            renderQueue.completeStarts();
        }
    }

    private static Set<String> getHints() {
        Set<String> hints = new HashSet<String>();
        // Within messages, images might change fast, and changing
        // the camera position, image size or aspect would be too distracting
        hints.add(Images.SameCameraPosition);
        hints.add(Images.SameResolution);
        return hints;
    }

    protected void queueRenderer(MediaRenderer renderer) {
        synchronized (queuedRenderers) {
            queuedRenderers.add(renderer);
        }
    }

    private void startQueuedRenderers() {
        synchronized (queuedRenderers) {
            renderQueue.start(queuedRenderers, teaseLib);
            queuedRenderers.clear();
        }
    }

    private void clearQueuedRenderers() {
        synchronized (queuedRenderers) {
            queuedRenderers.clear();
        }
    }

    /**
     * Shows choices.
     * 
     * @param scriptFunction
     *            The script function to executed while waiting for the user
     *            input. This parameter may be null, in this case the choices
     *            are only shown after all renderers have completed their
     *            mandatory parts
     * @param choice
     *            The first choice. This function doesn't make sense without
     *            showing at least one item, so one choice is mandatory
     * @param moreChoices
     *            More choices
     * @return
     */
    protected String showChoices(final ScriptFunction scriptFunction,
            List<String> choices) {
        // argument checking and text variable replacement
        final List<String> derivedChoices = replaceTextVariables(choices);
        TeaseLib.log("showChoices: " + derivedChoices.toString());
        // The result of this future task is never queried for,
        // instead a timeout is signaled via the TimeoutClick class
        // Run the script function while displaying the button
        // Speech recognition
        SpeechRecognition speechRecognizer = SpeechRecognizer.instance
                .get(actor.locale);
        final boolean recognizeSpeech = speechRecognizer.isReady();
        final ScriptFutureTask scriptTask;
        if (scriptFunction != null) {
            scriptTask = new ScriptFutureTask(this, scriptFunction,
                    derivedChoices, new ScriptFutureTask.TimeoutClick());
            // Start the script task right away
        } else {
            // If we don't have a script function, then the mandatory part of
            // the renderers must be completed before displaying the ui choices
            scriptTask = null;
            completeMandatory();
        }
        final List<Integer> srChoiceIndices = new ArrayList<Integer>(1);
        Event<SpeechRecognitionImplementation, SpeechRecognizedEventArgs> recognitionCompleted = recognitionCompletedEvent(
                derivedChoices, scriptTask, srChoiceIndices);
        speechRecognizer.events.recognitionCompleted.add(recognitionCompleted);
        if (recognizeSpeech) {
            speechRecognizer.startRecognition(derivedChoices);
        }
        // Get the user's choice
        int choiceIndex;
        try {
            if (scriptTask != null) {
                choiceScriptFunctionExecutor.execute(scriptTask);
                renderQueue.completeStarts();
                // TODO completeStarts() doesn't work because first we need to
                // wait for render threads that can be waited for completing
                // their starts
                // Workaround: A bit unsatisfying, but otherwise the choice
                // buttons would appear too early
                teaseLib.sleep(300, TimeUnit.MILLISECONDS);
            }
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
            TeaseLib.logDetail("choose: stopping speech recognition");
            speechRecognizer.events.recognitionCompleted
                    .remove(recognitionCompleted);
            speechRecognizer.stopRecognition();
        }
        // The script function may override any result from button clicks or
        // speech recognition
        String choice = scriptFunction != null ? scriptFunction.result
                : ScriptFunction.Finished;
        if (choice == ScriptFunction.Finished) {
            // Assign result from speech recognition
            // script task timeout or button click
            // supporting object identity by
            // returning an item of the original choices list
            if (!srChoiceIndices.isEmpty()) {
                // Use the first speech recognition result
                choice = choices.get(srChoiceIndices.get(0));
            } else if (scriptTask != null && scriptTask.timedOut()) {
                choice = ScriptFunction.Timeout;
            } else {
                choice = choices.get(choiceIndex);
            }
        }
        // Done in script task
        // TeaseLib.logDetail("showChoices: ending render queue");
        // renderQueue.endAll();
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

    private String replaceVariables(String text) {
        String parsedText = text;
        for (Persistence.TextVariable name : Persistence.TextVariable.values()) {
            parsedText = replaceTextVariable(parsedText, name);
        }
        return parsedText;
    }

    private String replaceTextVariable(String text, Persistence.TextVariable var) {
        final String value = var.toString();
        text = replaceTextVariable(text, var, "#" + value);
        text = replaceTextVariable(text, var, "#" + value.toLowerCase());
        return text;
    }

    private String replaceTextVariable(String text,
            Persistence.TextVariable var, String match) {
        if (text.contains(match)) {
            String value = teaseLib.get(var, actor.locale);
            text = text.replace(match, value);
        }
        return text;
    }

    private List<String> replaceTextVariables(List<String> choices) {
        final List<String> derivedChoices = new ArrayList<String>(
                choices.size());
        for (String derivedChoice : choices) {
            if (derivedChoice != null) {
                derivedChoices.add(replaceVariables(derivedChoice));
            } else {
                throw new IllegalArgumentException("Choice may not be null");
            }
        }
        return derivedChoices;
    }
}
