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
import teaselib.TeaseLib;
import teaselib.core.speechrecognition.SpeechRecognition;
import teaselib.core.speechrecognition.SpeechRecognitionHypothesisEventHandler;
import teaselib.core.speechrecognition.SpeechRecognizer;
import teaselib.core.texttospeech.TextToSpeechPlayer;
import teaselib.core.util.NamedExecutorService;

public abstract class TeaseScriptBase {

    public final TeaseLib teaseLib;
    public final ResourceLoader resources;

    public final Actor actor;
    public final String namespace;

    public static final String Timeout = "Timeout";

    protected String mood = Mood.Neutral;
    protected String displayImage = Message.DominantImage;

    protected static final int NoTimeout = 0;

    private static final MediaRendererQueue renderQueue = new MediaRendererQueue();
    private final Deque<MediaRenderer> deferredRenderers = new ArrayDeque<MediaRenderer>();

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
            renderDeferred();
            // Clone the actor to prevent the wrong actor image to be displayed
            // when changing the actor images right after saying a message.
            // Without cloning one of the new actor images would be displayed
            // with the current message because the actor is shared between
            // script and message
            Message parsedMessage = new Message(new Actor(message.actor));
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
            renderQueue.start(renderMessage, teaseLib);
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

    protected void addDeferred(MediaRenderer renderer) {
        synchronized (deferredRenderers) {
            deferredRenderers.add(renderer);
        }
    }

    void clearDeferred() {
        synchronized (deferredRenderers) {
            deferredRenderers.clear();
        }
    }

    private void renderDeferred() {
        synchronized (deferredRenderers) {
            completeAll();
            renderQueue.start(deferredRenderers, teaseLib);
            deferredRenderers.clear();
        }
    }

    /**
     * @param scriptFunction
     * @param choice
     *            The first choice. This function doesn't make sense without
     *            showing at least one item, so one choice is mandatory
     * @param moreChoices
     *            More choices
     * @return
     */
    protected String showChoices(final Runnable scriptFunction,
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
        SpeechRecognitionHypothesisEventHandler eventHandler = new SpeechRecognitionHypothesisEventHandler(
                this.teaseLib, speechRecognizer);
        eventHandler.setChoices(derivedChoices);
        if (recognizeSpeech) {
            speechRecognizer.startRecognition(derivedChoices);
        }
        final ScriptFutureTask scriptTask;
        if (scriptFunction != null) {
            scriptTask = new ScriptFutureTask(this, scriptFunction,
                    derivedChoices, new ScriptFutureTask.TimeoutClick());
        } else {
            scriptTask = null;
        }
        eventHandler.scriptTask = scriptTask;
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
            if (scriptTask != null) {
                TeaseLib.logDetail("choose: Cancelling script task");
                scriptTask.cancel(true);
            }
            if (recognizeSpeech) {
                TeaseLib.logDetail("choose: completing speech recognition");
                speechRecognizer.completeSpeechRecognitionInProgress();
            }
        } finally {
            TeaseLib.logDetail("choose: stopping speech recognition");
            speechRecognizer.stopRecognition();
            eventHandler.dispose();
        }
        // Assign result from speech recognition
        // script task timeout or button click
        // supporting object identity by
        // returning an item of the original choices list
        String chosen = null;
        int srChoiceIndex = eventHandler.getChoiceIndex();
        if (srChoiceIndex >= 0) {
            // Use the first speech recognition result
            chosen = choices.get(srChoiceIndex);
        } else if (scriptTask != null && scriptTask.timeout.clicked) {
            chosen = Timeout;
        } else {
            chosen = choices.get(choiceIndex);
        }
        TeaseLib.logDetail("showChoices: ending render queue");
        renderQueue.endAll();
        return chosen;
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
