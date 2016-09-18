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
        // This hard-wires sound resources to a sub folder %namespace% in the
        // root directory
        // TODO be able to store class relative and relative to script class
        // - don't reference resource loader basedir, to make resource loader a
        // singleton
        ttsPlayer.loadActorVoices(resources, "/" + namespace);
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
     * played, delay expired), and continue execution of the script. This won't
     * display a button, it just waits.
     */
    public void completeAll() {
        teaseLib.renderQueue.completeAll();
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
            Message parsedMessage = injectActorImagesAndExpandTextVariables(message);
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
        // Preprocess message
        boolean selectFirstImage = true;
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
                    imageType = nextImage = absoluteResource(part.value);
                }
            } else if (Message.Type.FileTypes.contains(part.type)) {
                parsedMessage.add(part.type, absoluteResource(part.value));
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
                // Resolve actor image
                if (imageType == Message.ActorImage) {
                    if (actor.images.hasNext()) {
                        if (selectFirstImage) {
                            // TODO hint aspect
                            // TODO hint camera position
                            // TODO hint posture
                            // TODO hint mood
                            nextImage = actor.images.next();
                            selectFirstImage = false;
                        } else {
                            nextImage = actor.images.next();
                        }
                    } else {
                        nextImage = Message.NoImage;
                    }
                }
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
        return parsedMessage;

    }

    /**
     * Return the absolute resource path of an resource. If the path is absolute
     * already, {@code path} is returned.
     * 
     * @param path
     *            The path to return the absolute resource path for.
     * @return The absolute resource path.
     */
    public String absoluteResource(String path) {
        if (path.startsWith("/")) {
            return path;
        } else {
            String folder = getClass().getPackage().getName().replace(".", "/")
                    + "/";
            if (path.startsWith(folder)) {
                return path;
            } else {
                return "/" + folder + path;
            }
        }
    }

    // // TODO must be part of preprocessMessage
    // private String getActorImage(Set<String> imageHints) {
    // final String path;
    // Images images = message.actor.images;
    // if (images != null) {
    // String[] hintArray = new String[imageHints.size()];
    // hintArray = imageHints.toArray(hintArray);
    // images.hint(hintArray);
    // path = images.next();
    // if (path == null && !teaseLib.getBoolean(Config.Namespace,
    // Config.Debug.IgnoreMissingResources)) {
    // logger.info("Actor '" + message.actor.name
    // + "': images missing - please initialize");
    // }
    // } else if (!teaseLib.getBoolean(Config.Namespace,
    // Config.Debug.IgnoreMissingResources)) {
    // logger.info("Actor '" + message.actor.name
    // + "': images missing - please initialize");
    // path = null;
    // } else {
    // path = null;
    // }
    // return path;
    // }

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
        Map<String, Runnable> pauseHandlers = new HashMap<String, Runnable>();
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

    private static Runnable pauseHandler(final ShowChoices showChoices) {
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
                    logger.info(
                            "Resuming choices " + showChoices.derivedChoices);
                } catch (InterruptedException e) {
                    throw new ScriptInterruptedException();
                }
            }
        };
    }

    private Runnable recognitionRejectedPauseHandler() {
        return new Runnable() {
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
                teaseLib.getTextVariables(actor.getLocale()), actor.textVariables);
    }
}
