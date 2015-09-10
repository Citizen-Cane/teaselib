package teaselib.core;

import java.io.IOException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Stack;

import teaselib.Actor;
import teaselib.Message;
import teaselib.Mood;
import teaselib.ScriptFunction;
import teaselib.TeaseLib;
import teaselib.core.speechrecognition.SpeechRecognitionResult.Confidence;
import teaselib.core.texttospeech.TextToSpeechPlayer;

public abstract class TeaseScriptBase {

    public final TeaseLib teaseLib;
    public final ResourceLoader resources;

    public final Actor actor;
    public final String namespace;

    protected String mood = Mood.Neutral;
    protected String displayImage = Message.DominantImage;

    protected static final int NoTimeout = 0;

    private static final Stack<ShowChoices> choicesStack = new Stack<ShowChoices>();

    private static final MediaRendererQueue renderQueue = new MediaRendererQueue();
    private final Deque<MediaRenderer> queuedRenderers = new ArrayDeque<MediaRenderer>();

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

    protected String showChoices(final ScriptFunction scriptFunction,
            List<String> choices) {
        return showChoices(scriptFunction, choices, Confidence.Default);
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
     * @param recognitionConfidence
     *            The confidence threshold used for speech recognitions
     * @return
     */
    protected String showChoices(final ScriptFunction scriptFunction,
            List<String> choices, Confidence recognitionConfidence) {
        // argument checking and text variable replacement
        final List<String> derivedChoices = replaceTextVariables(choices);
        TeaseLib.log("showChoices: " + derivedChoices.toString());
        if (scriptFunction == null) {
            // If we don't have a script function,
            // then the mandatory part of the renderers
            // must be completed before displaying the ui choices
            completeMandatory();
        } else {
            if (scriptFunction.relation == ScriptFunction.Relation.Confirmation) {
                // A confirmation must appears like a normal button,
                // in a way it is concatenated to the last messagee
                completeMandatory();
            } else {
                // An autonomous script function does not relate to the current
                // message, therefore we'll wait until all of the last message
                // has been completed
                completeAll();
            }
        }
        final ShowChoices showChoices = new ShowChoices(this, choices,
                derivedChoices, scriptFunction, recognitionConfidence);
        String choice = showChoices(showChoices);
        TeaseLib.logDetail("Reply finished");
        // Object identity is supported by
        // returning an item of the original choices list
        return choice;
    }

    private static String showChoices(ShowChoices showChoices) {
        String choice = null;
        ShowChoices previous = null;
        synchronized (choicesStack) {
            if (!choicesStack.empty()) {
                previous = choicesStack.peek();
            }
            choicesStack.push(showChoices);
            if (previous != null) {
                previous.pause(ShowChoices.Paused);
            }
        }
        while (true) {
            choice = showChoices.show();
            synchronized (choicesStack) {
                if (choice == ShowChoices.Paused) {
                    // Someone dismissed our set of buttons in order to to show
                    // a different set, so we have to wait to restore
                    try {
                        choicesStack.wait();
                    } catch (InterruptedException e) {
                        throw new ScriptInterruptedException();
                    }
                    continue;
                } else {
                    choicesStack.pop();
                    if (!choicesStack.empty()) {
                        choicesStack.notifyAll();
                    }
                    break;
                }
            }
        }
        return choice;
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
