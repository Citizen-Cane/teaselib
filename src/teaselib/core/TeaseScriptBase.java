package teaselib.core;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import teaselib.Actor;
import teaselib.Message;
import teaselib.Mood;
import teaselib.ScriptFunction;
import teaselib.core.media.MediaRenderer;
import teaselib.core.media.MediaRenderer.Replay.Position;
import teaselib.core.media.RenderInterTitle;
import teaselib.core.media.RenderMessage;
import teaselib.core.speechrecognition.SpeechRecognitionResult.Confidence;
import teaselib.core.texttospeech.TextToSpeechPlayer;
import teaselib.util.SpeechRecognitionRejectedScript;
import teaselib.util.TextVariables;

public abstract class TeaseScriptBase {
    private static final Logger logger = LoggerFactory
            .getLogger(TeaseScriptBase.class);

    public final TeaseLib teaseLib;
    public final ResourceLoader resources;

    public final Actor actor;
    public final String namespace;

    protected String mood = Mood.Neutral;
    protected String displayImage = Message.ActorImage;

    protected static final int NoTimeout = 0;

    private static final ChoicesStack choicesStack = new ChoicesStack();
    private final List<MediaRenderer> queuedRenderers = new ArrayList<MediaRenderer>();
    private final List<MediaRenderer.Threaded> backgroundRenderers = new ArrayList<MediaRenderer.Threaded>();

    private List<MediaRenderer> playedRenderers = null;

    public class Replay {
        final List<MediaRenderer> renderers;

        public Replay(List<MediaRenderer> renderers) {
            super();
            logger.info("Remembering renderers in replay " + this);
            this.renderers = new ArrayList<MediaRenderer>(renderers);
        }

        public void replay(Position replayPosition) {
            synchronized (queuedRenderers) {
                logger.info("Replaying renderers from replay " + this);
                // Finish current set before replaying
                completeMandatory();
                // Restore the prompt that caused running the SR-rejected script
                // as soon as possible
                endAll();
                teaseLib.renderQueue.replay(renderers, replayPosition);
                playedRenderers = renderers;
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
        TextToSpeechPlayer ttsPlayer = TextToSpeechPlayer.instance();
        ttsPlayer.loadActorVoiceProperties(resources);
        ttsPlayer.acquireVoice(actor);
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
        TextToSpeechPlayer ttsPlayer = TextToSpeechPlayer.instance();
        ttsPlayer.acquireVoice(actor);
    }

    protected static List<String> buildChoicesFromArray(String choice,
            String... more) {
        List<String> choices = new ArrayList<String>(1 + more.length);
        choices.add(choice);
        choices.addAll(Arrays.asList(more));
        return choices;
    }

    public void completeStarts() {
        teaseLib.renderQueue.completeStarts();
    }

    public void completeMandatory() {
        teaseLib.renderQueue.completeMandatories();
    }

    /**
     * Just wait for everything to be rendered (messages displayed, sounds
     * played, delay expired), and continue execution of the script.
     * <p>
     * This won't display a button, it just waits. Background threads will
     * continue to run.
     */
    public void completeAll() {
        teaseLib.renderQueue.completeAll();
        teaseLib.renderQueue.endAll();
    }

    /**
     * Stop rendering and end all render threads
     */
    public void endAll() {
        teaseLib.renderQueue.endAll();
        stopBackgroundRenderers();
    }

    protected void renderIntertitle(String... text) {
        try {
            RenderInterTitle interTitle = new RenderInterTitle(
                    new Message(actor,
                            expandTextVariables(Arrays.asList(text))),
                    teaseLib);
            renderMessage(interTitle);
        } finally {
            displayImage = Message.ActorImage;
            mood = Mood.Neutral;
        }
    }

    protected void renderMessage(Message message,
            TextToSpeechPlayer ttsPlayer) {
        try {
            // inject speech parts to replay pre-recorded speech or use TTS
            // This has to be done first as subsequent parse steps
            // inject moods and thus change the message hash
            if (ttsPlayer != null) {
                if (ttsPlayer.prerenderedSpeechAvailable(message.actor)) {
                    // Don't use TTS, even if pre-recorded speech is missing
                    message = ttsPlayer.createPrerenderedSpeechMessage(message,
                            resources);
                }
            }
            Message parsedMessage = injectActorImagesAndExpandTextVariables(
                    message);
            renderMessage(new RenderMessage(resources, parsedMessage, ttsPlayer,
                    teaseLib));
        } finally {
            displayImage = Message.ActorImage;
            mood = Mood.Neutral;
        }
    }

    private void renderMessage(MediaRenderer renderMessage) {
        synchronized (teaseLib.renderQueue) {
            synchronized (queuedRenderers) {
                queueRenderer(renderMessage);
                // Remember this set for replay
                playedRenderers = new ArrayList<MediaRenderer>(queuedRenderers);
                // Remember in order to clear queued before completing
                // previous set
                List<MediaRenderer> nextSet = new ArrayList<MediaRenderer>(
                        queuedRenderers);
                // Must clear queue for next set before completing current,
                // because if the current set is cancelled,
                // the next set must be discarded
                queuedRenderers.clear();
                // Now the current set can be completed, and canceling the
                // current set will result in an empty next set
                completeAll();
                // Start a new message in the log
                teaseLib.transcript.info("");
                teaseLib.renderQueue.start(nextSet);
            }
            startBackgroundRenderers();
            teaseLib.renderQueue.completeStarts();
        }
    }

    private Message injectActorImagesAndExpandTextVariables(Message message) {
        // Clone the actor to prevent the wrong actor image to be displayed
        // when changing the actor images right after saying a message.
        // Without cloning one of the new actor images would be displayed
        // with the current message because the actor is shared between
        // script and message
        Message parsedMessage = new Message(new Actor(message.actor));

        // TODO hint actor aspect, camera position, posture

        if (message.isEmpty()) {
            ensureEmptyMessageContainsDisplayImage(parsedMessage,
                    getActorOrDisplayImage(displayImage));
        } else {
            String imageType = displayImage;
            String selectedImage = "";
            String nextImage = displayImage;
            String nextMood = null;

            for (Message.Part part : message.getParts()) {
                if (part.type == Message.Type.Image) {
                    // Remember what type of image to display
                    // with the next text element
                    if (part.value == Message.ActorImage) {
                        imageType = part.value;
                    } else if (part.value == Message.NoImage) {
                        imageType = part.value;
                    } else {
                        imageType = nextImage = part.value;
                    }
                } else if (Message.Type.FileTypes.contains(part.type)) {
                    parsedMessage.add(part.type, part.value);
                } else if (part.type == Message.Type.Keyword) {
                    if (part.value == Message.ActorImage) {
                        imageType = part.value;
                    } else if (part.value == Message.NoImage) {
                        imageType = part.value;
                    } else {
                        parsedMessage.add(part);
                    }
                } else if (part.type == Message.Type.Mood) {
                    nextMood = part.value;
                } else if (part.type == Message.Type.Text) {
                    nextImage = getActorOrDisplayImage(imageType);
                    // Update image if changed
                    if (!nextImage.equalsIgnoreCase(selectedImage)) {
                        parsedMessage.add(Message.Type.Image, nextImage);
                        selectedImage = nextImage;
                    }
                    // set mood if not done already
                    if (nextMood == null) {
                        parsedMessage.add(Message.Type.Mood, mood);
                    } else {
                        // Reset mood after each text part
                        nextMood = null;
                    }
                    // Replace text variables
                    parsedMessage.add(new Message.Part(part.type,
                            expandTextVariables(part.value)));
                } else {
                    parsedMessage.add(part);
                }
            }

            if (parsedMessage.isEmpty()) {
                ensureEmptyMessageContainsDisplayImage(parsedMessage,
                        getActorOrDisplayImage(imageType));
            }
        }

        return parsedMessage;

    }

    private String getActorOrDisplayImage(String imageType) {
        final String nextImage;
        if (imageType == Message.ActorImage) {
            if (actor.images.hasNext()) {
                nextImage = actor.images.next();
            } else {
                nextImage = Message.NoImage;
            }
        } else {
            nextImage = imageType;
        }
        return nextImage;
    }

    private static void ensureEmptyMessageContainsDisplayImage(
            Message parsedMessage, String nextImage) {
        parsedMessage.add(Message.Type.Image, nextImage);
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

    protected void queueBackgropundRenderer(MediaRenderer.Threaded renderer) {
        synchronized (backgroundRenderers) {
            backgroundRenderers.add(renderer);
        }
    }

    private void startBackgroundRenderers() {
        synchronized (backgroundRenderers) {
            for (MediaRenderer.Threaded renderer : backgroundRenderers) {
                if (!renderer.hasCompletedStart()) {
                    try {
                        renderer.render();
                    } catch (IOException e) {
                        logger.error(e.getMessage(), e);
                    }
                }
            }
        }
    }

    private void stopBackgroundRenderers() {
        synchronized (backgroundRenderers) {
            for (MediaRenderer.Threaded renderer : backgroundRenderers) {
                renderer.interrupt();
            }
            backgroundRenderers.clear();
        }
    }

    /**
     * {@code recognitionConfidence} defaults to {@link Confidence#Default}
     * 
     * @see TeaseScriptBase#showChoices(ScriptFunction, Confidence, List)
     */
    protected final String showChoices(ScriptFunction scriptFunction,
            List<String> choices) {
        return showChoices(scriptFunction, Confidence.Default, choices);
    }

    /**
     * Shows choices.
     * 
     * @param scriptFunction
     *            The script function to executed while waiting for the user
     *            input. This parameter may be null, in this case the choices
     *            are only shown after all renderers have completed their
     *            mandatory parts
     * @param recognitionConfidence
     *            The confidence threshold used for speech recognition.
     * @param choice
     *            The first choice. This function doesn't make sense without
     *            showing at least one item, so one choice is mandatory
     * @return The choice made by the user, {@link ScriptFunction#Timeout} if
     *         the function has ended, or a custom result value set by the
     *         script function.
     */
    protected String showChoices(ScriptFunction scriptFunction,
            Confidence recognitionConfidence, List<String> choices) {
        // argument checking and text variable replacement
        final List<String> derivedChoices = expandTextVariables(choices);
        ScriptFutureTask scriptTask = scriptFunction != null
                ? new ScriptFutureTask(this, scriptFunction, derivedChoices,
                        new ScriptFutureTask.TimeoutClick())
                : null;
        final boolean choicesStackContainsSRRejectedState = choicesStack
                .containsPauseState(ShowChoices.RecognitionRejected);
        final ShowChoices showChoices = new ShowChoices(this, choices,
                derivedChoices, scriptTask, recognitionConfidence,
                choicesStackContainsSRRejectedState);
        Map<String, PauseHandler> pauseHandlers = new HashMap<String, PauseHandler>();
        // The pause handler resumes displaying choices when the choice object
        // becomes the top-element of the choices stack again
        pauseHandlers.put(ShowChoices.Paused, pauseHandler(showChoices));
        pauseHandlers.put(ShowChoices.RecognitionRejected,
                recognitionRejectedPauseHandler());
        waitToStartScriptFunction(scriptFunction);
        if (scriptFunction == null) {
            stopBackgroundRenderers();
        } else if (scriptFunction.relation != ScriptFunction.Relation.Autonomous) {
            stopBackgroundRenderers();
        }
        logger.info("showChoices: " + derivedChoices.toString());
        String choice = choicesStack.show(this, showChoices, pauseHandlers);
        logger.debug("Reply finished");
        teaseLib.transcript.info("< " + choice);
        return choice;
    }

    private static PauseHandler pauseHandler(final ShowChoices showChoices) {
        return new PauseHandler() {
            @Override
            public boolean endRenderers() {
                // Always false because the top element on the choices stack
                // decides whether to end the previous set of renderers
                return false;
            }

            @Override
            public void run() {
                // Someone dismissed our set of buttons in order to to show
                // a different set, so we have to wait until we may restore
                try {
                    // only the top-most element may resume
                    synchronized (choicesStack) {
                        while (choicesStack.peek() != showChoices) {
                            choicesStack.wait();
                        }
                    }
                    logger.info(
                            "Resuming choices " + showChoices.derivedChoices);
                } catch (InterruptedException e) {
                    throw new ScriptInterruptedException();
                }
            }
        };
    }

    private PauseHandler recognitionRejectedPauseHandler() {
        return new PauseHandler() {
            @Override
            public boolean endRenderers() {
                return true;
            }

            @Override
            public void run() {
                if (playedRenderers != null) {
                    SpeechRecognitionRejectedScript speechRecognitionRejectedScript = actor.speechRecognitionRejectedScript;
                    logger.info("Running SpeechRecognitionRejectedScript "
                            + speechRecognitionRejectedScript.toString());
                    Replay beforeSpeechRecognitionRejected = new Replay(
                            playedRenderers);
                    speechRecognitionRejectedScript.run();
                    beforeSpeechRecognitionRejected.replay(Position.End);
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

    private List<String> expandTextVariables(List<String> prompts) {
        return allTextVariables().expand(prompts);
    }

    private String expandTextVariables(String s) {
        return allTextVariables().expand(s);
    }

    private TextVariables allTextVariables() {
        return new TextVariables(TextVariables.Defaults,
                teaseLib.getTextVariables(actor.getLocale()),
                actor.textVariables);
    }
}
