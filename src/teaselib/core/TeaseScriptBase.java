package teaselib.core;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

import teaselib.Actor;
import teaselib.Message;
import teaselib.Mood;
import teaselib.ScriptFunction;
import teaselib.TeaseLib;
import teaselib.core.MediaRenderer.Replay.Position;
import teaselib.core.events.Event;
import teaselib.core.speechrecognition.SpeechRecognition;
import teaselib.core.speechrecognition.SpeechRecognitionImplementation;
import teaselib.core.speechrecognition.SpeechRecognitionResult.Confidence;
import teaselib.core.speechrecognition.SpeechRecognizer;
import teaselib.core.speechrecognition.events.SpeechRecognizedEventArgs;
import teaselib.core.texttospeech.TextToSpeechPlayer;
import teaselib.util.SpeechRecognitionRejectedScript;

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
    private final List<MediaRenderer> queuedRenderers = new ArrayList<MediaRenderer>();

    private List<MediaRenderer> playedRenderers = null;

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
            // Remember renderers in order to be able to replay them
            playedRenderers = new ArrayList<MediaRenderer>(queuedRenderers);
            startQueuedRenderers();
            renderQueue.completeStarts();
        }
    }

    private static Set<String> getHints() {
        Set<String> hints = new HashSet<String>();
        // Within messages, images might change fast, and changing
        // the camera position, image size or aspect would be too distracting,
        // and an aspect change might even change the text position
        hints.add(Images.SameCameraPosition);
        hints.add(Images.SameResolution);
        return hints;
    }

    protected void queueRenderers(List<MediaRenderer> renderers) {
        synchronized (queuedRenderers) {
            queuedRenderers.addAll(renderers);
        }
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

    private void replay(Position replayPosition) {
        // Ensure all current renderers have been played before restarting
        completeAll();
        queueRenderers(playedRenderers);
        synchronized (queuedRenderers) {
            renderQueue.replay(queuedRenderers, teaseLib, replayPosition);
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
        ScriptFutureTask scriptTask = scriptFunction != null ? new ScriptFutureTask(
                this, scriptFunction, derivedChoices,
                new ScriptFutureTask.TimeoutClick()) : null;
        final ShowChoices showChoices = new ShowChoices(this, choices,
                derivedChoices, scriptTask, recognitionConfidence);
        Map<String, Runnable> pauseHandlers = new HashMap<String, Runnable>();
        addPauseHandler(pauseHandlers, showChoices);
        // Comment speech recognition rejections if we aren't doing this already
        final SpeechRecognition speechRecognition = SpeechRecognizer.instance
                .get(actor.locale);
        // The RecognitionRejectedEvent-handler doesn't work in replies that run
        // script functions but it works inside script functions. Reason are:
        // - The event handler would have to wait until messages rendered by the
        // script function are completed
        // -> delay in response
        // - script functions may include timing which would be messed up by
        // pausing them
        // - Script functions may invoke other script functions, but the handler
        // management is neither multi-threading-aware nor synchronized
        // - The current code is unable to recover to the choice on top of the
        // choices stack after a recognition-rejected pause event
        boolean handleRecognitionRejectedEvents = scriptFunction == null
                && actor.speechRecognitionRejectedHandler != null
                && !(this instanceof SpeechRecognitionRejectedScript)
                && speechRecognition.isReady();
        final Event<SpeechRecognitionImplementation, SpeechRecognizedEventArgs> recognitionRejected;
        if (handleRecognitionRejectedEvents) {
            recognitionRejected = addRecognitionRejectedHandler(showChoices,
                    pauseHandlers, speechRecognition);
        } else {
            recognitionRejected = null;
        }
        waitToStartScriptFunction(scriptFunction);
        TeaseLib.log("showChoices: " + derivedChoices.toString());
        // Show the choices
        final String choice;
        try {
            choice = showChoices(showChoices, pauseHandlers);
        } finally {
            if (handleRecognitionRejectedEvents) {
                speechRecognition.events.recognitionRejected
                        .remove(recognitionRejected);
            }
        }
        TeaseLib.logDetail("Reply finished");
        // Object identity is supported by
        // returning an item of the original choices list
        return choice;
    }

    private static void addPauseHandler(Map<String, Runnable> pauseHandlers,
            final ShowChoices showChoices) {
        pauseHandlers.put(ShowChoices.Paused, new Runnable() {
            @Override
            public void run() {
                // Someone dismissed our set of buttons in order to to show
                // a different set, so we have to wait until we may restore
                try {
                    // only the top-most element may resume
                    while (choicesStack.peek() != showChoices) {
                        choicesStack.wait();
                    }
                    TeaseLib.log("Resuming choices "
                            + showChoices.derivedChoices);
                } catch (InterruptedException e) {
                    throw new ScriptInterruptedException();
                }
            }
        });
    }

    private Event<SpeechRecognitionImplementation, SpeechRecognizedEventArgs> addRecognitionRejectedHandler(
            final ShowChoices showChoices, Map<String, Runnable> pauseHandlers,
            final SpeechRecognition speechRecognition) {
        final Event<SpeechRecognitionImplementation, SpeechRecognizedEventArgs> recognitionRejected;
        recognitionRejected = new Event<SpeechRecognitionImplementation, SpeechRecognizedEventArgs>() {
            @Override
            public void run(SpeechRecognitionImplementation sender,
                    SpeechRecognizedEventArgs eventArgs) {
                showChoices.pause(ShowChoices.RecognitionRejected);
            }
        };
        // The recognitionRejected handler may not trigger immediately when
        // a script function renders messages, because it must wait until
        // the render queue is empty, and this includes message delays.
        // Script functions are not supported, but the message may still
        // render comments while the choices are shown.
        pauseHandlers.put(ShowChoices.RecognitionRejected, new Runnable() {
            @Override
            public void run() {
                if (renderQueue.hasCompletedMandatory()) {
                    actor.speechRecognitionRejectedHandler.run();
                    TeaseScriptBase.this.replay(Position.End);
                } else {
                    TeaseLib.log("Skipping RecognitionRejected-handler while rendering message");
                }
            }
        });
        speechRecognition.events.recognitionRejected.add(recognitionRejected);
        return recognitionRejected;
    }

    private void waitToStartScriptFunction(final ScriptFunction scriptFunction) {
        // Wait for previous message to complete
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
    }

    private static final Object showChoicesSyncObject = new Object();

    private static String showChoices(ShowChoices showChoices,
            Map<String, Runnable> pauseHandlers) {
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
            // Ensure only one thread at a time can realize ui elements
            synchronized (showChoicesSyncObject) {
                choice = showChoices.show();
            }
            synchronized (choicesStack) {
                if (pauseHandlers.containsKey(choice)) {
                    TeaseLib.log("Invoking choices handler for choices="
                            + showChoices.derivedChoices.toString()
                            + " reason=" + choice.toString());
                    pauseHandlers.get(choice).run();
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
