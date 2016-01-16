package teaselib.core;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import teaselib.Actor;
import teaselib.Message;
import teaselib.Mood;
import teaselib.ScriptFunction;
import teaselib.TeaseLib;
import teaselib.core.MediaRenderer.Replay.Position;
import teaselib.core.speechrecognition.SpeechRecognitionResult.Confidence;
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

    private static final ChoicesStack choicesStack = new ChoicesStack();
    private static final MediaRendererQueue renderQueue = new MediaRendererQueue();
    private final List<MediaRenderer> queuedRenderers = new ArrayList<MediaRenderer>();

    private List<MediaRenderer> playedRenderers = null;

    public class Replay {
        final List<MediaRenderer> renderers;

        public Replay(List<MediaRenderer> renderers) {
            super();
            teaseLib.log.info("Remembering renderers in replay " + this);
            this.renderers = new ArrayList<MediaRenderer>(renderers);
        }

        public void replay(Position replayPosition) {
            synchronized (queuedRenderers) {
                // Ensure all current renderers have been played before
                // restarting
                // Don't wait for all since the message is just redisplayed
                completeMandatory();
                teaseLib.log.info("Replaying renderers from replay " + this);
                queueRenderers(renderers);
                renderQueue.replay(queuedRenderers, teaseLib, replayPosition);
                playedRenderers = new ArrayList<MediaRenderer>(queuedRenderers);
            }
        }
    }

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
            teaseLib.log.error(this, e);
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

    /**
     * {@code recognitionConfidence} defaults to {@link Confidence#Default}
     * 
     * @see TeaseScriptBase#showChoices(ScriptFunction, List, Confidence)
     */
    protected final String showChoices(ScriptFunction scriptFunction,
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
     *            The confidence threshold used for speech recognition.
     * @return The choice made by the user, {@link ScriptFunction#Timeout} if
     *         the function has ended, or a custom result value set by the
     *         script function.
     */
    protected String showChoices(ScriptFunction scriptFunction,
            List<String> choices, Confidence recognitionConfidence) {
        // argument checking and text variable replacement
        final List<String> derivedChoices = replaceTextVariables(choices);
        ScriptFutureTask scriptTask = scriptFunction != null
                ? new ScriptFutureTask(this, scriptFunction, derivedChoices,
                        new ScriptFutureTask.TimeoutClick())
                : null;
        final boolean choicesStackContainsSRRejectedState = choicesStack
                .containsPauseState(ShowChoices.RecognitionRejected);
        final ShowChoices showChoices = new ShowChoices(this, choices,
                derivedChoices, scriptTask, recognitionConfidence,
                choicesStackContainsSRRejectedState);
        Map<String, Runnable> pauseHandlers = new HashMap<String, Runnable>();
        // The pause handler resumes displaying choices
        // when the choice object becomes the top-element of the choices stack
        // again
        pauseHandlers.put(ShowChoices.Paused, pauseHandler(showChoices));
        pauseHandlers.put(ShowChoices.RecognitionRejected,
                recognitionRejectedPauseHandler(showChoices));
        waitToStartScriptFunction(scriptFunction);
        teaseLib.log.info("showChoices: " + derivedChoices.toString());
        String choice = choicesStack.show(this, showChoices, pauseHandlers);
        teaseLib.log.debug("Reply finished");
        teaseLib.transcript.info("< " + choice);
        return choice;
    }

    private Runnable pauseHandler(final ShowChoices showChoices) {
        return new Runnable() {
            @Override
            public void run() {
                // Someone dismissed our set of buttons in order to to show
                // a different set, so we have to wait until we may restore
                try {
                    // only the top-most element may resume
                    while (choicesStack.peek() != showChoices) {
                        wait();
                    }
                    teaseLib.log.info(
                            "Resuming choices " + showChoices.derivedChoices);
                } catch (InterruptedException e) {
                    throw new ScriptInterruptedException();
                }
            }
        };
    }

    private Runnable recognitionRejectedPauseHandler(
            final ShowChoices showChoices) {
        // Handling speech recognition rejected events:
        // RecognitionRejectedEvent-scripts doesn't work in reply-calls that
        // invoke
        // script functions but they work inside script functions.
        // Reason are:
        // - The event handler would have to wait until messages rendered by the
        // script function are completed -> delay in response
        // - script functions may include timing which would be messed up by
        // pausing them
        // - Script functions may invoke other script functions, but the handler
        // management is neither multi-threading-aware nor synchronized
        // - The current code is unable to recover to the choice on top of the
        // choices stack after a recognition-rejected pause event

        // The recognitionRejected handler won't trigger immediately when
        // a script function renders messages, because it will wait until
        // the render queue is empty, and this includes message delays.
        // Therefore script functions are not supported, because the script
        // function would still render messages while the choices are shown.
        // However rendering messages while showing choices should be fine.
        return new Runnable() {
            @Override
            public void run() {
                SpeechRecognitionRejectedScript speechRecognitionRejectedScript = actor.speechRecognitionRejectedScript;
                // Test before pause to avoid button flicker
                if (speechRecognitionRejectedScript != null
                        && renderQueue.hasCompletedMandatory()) {
                    Replay beforeSpeechRecognitionRejected = new Replay(
                            playedRenderers);
                    teaseLib.log.info("Running SpeechRecognitionRejectedScript "
                            + speechRecognitionRejectedScript.toString());
                    speechRecognitionRejectedScript.run();
                    beforeSpeechRecognitionRejected.replay(Position.End);
                } else {
                    teaseLib.log.info("Skipping RecognitionRejected-handler  "
                            + speechRecognitionRejectedScript.toString()
                            + " while rendering message");
                }
            }
        };
    }

    private void waitToStartScriptFunction(
            final ScriptFunction scriptFunction) {
        // Wait for previous message to complete
        if (scriptFunction == null) {
            // If we don't have a script function,
            // then the mandatory part of the renderers
            // must be completed before displaying the ui choices
            completeMandatory();
        } else {
            if (scriptFunction.relation == ScriptFunction.Relation.Confirmation) {
                // A confirmation relates to the current message,
                // and must appears like a normal button,
                // so in a way it is concatenated to the current message
                completeMandatory();
            } else {
                // An autonomous script function does not relate to the current
                // message, therefore we'll wait until all of the last message
                // has been completed
                completeAll();
            }
        }
    }

    private String replaceVariables(String text) {
        String parsedText = text;
        for (Persistence.TextVariable name : Persistence.TextVariable
                .values()) {
            parsedText = replaceTextVariable(parsedText, name);
        }
        return parsedText;
    }

    private String replaceTextVariable(String text,
            Persistence.TextVariable var) {
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
