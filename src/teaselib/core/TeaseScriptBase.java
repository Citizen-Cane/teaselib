package teaselib.core;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import teaselib.Actor;
import teaselib.Config;
import teaselib.Message;
import teaselib.Mood;
import teaselib.Replay;
import teaselib.ScriptFunction;
import teaselib.core.media.MediaRenderer;
import teaselib.core.media.RenderInterTitle;
import teaselib.core.media.RenderMessage;
import teaselib.core.speechrecognition.SpeechRecognition;
import teaselib.core.speechrecognition.SpeechRecognitionResult.Confidence;
import teaselib.core.speechrecognition.SpeechRecognizer;
import teaselib.core.texttospeech.TextToSpeechPlayer;
import teaselib.core.ui.Choices;
import teaselib.core.ui.InputMethod;
import teaselib.core.ui.Prompt;
import teaselib.core.ui.SpeechRecognitionInputMethod;
import teaselib.util.TextVariables;

public abstract class TeaseScriptBase {
    private static final Logger logger = LoggerFactory.getLogger(TeaseScriptBase.class);

    public final TeaseLib teaseLib;
    public final ResourceLoader resources;

    public final Actor actor;
    public final String namespace;

    protected String mood = Mood.Neutral;
    protected String displayImage = Message.ActorImage;

    protected static final int NoTimeout = 0;

    private final List<MediaRenderer> queuedRenderers = new ArrayList<MediaRenderer>();
    private final List<MediaRenderer.Threaded> backgroundRenderers = new ArrayList<MediaRenderer.Threaded>();

    private List<MediaRenderer> playedRenderers = null;

    public class ReplayImpl implements Replay {
        final List<MediaRenderer> renderers;

        public ReplayImpl(List<MediaRenderer> renderers) {
            super();
            logger.info("Remembering renderers in replay " + this);
            this.renderers = new ArrayList<MediaRenderer>(renderers);
        }

        @Override
        public void replay(Replay.Position replayPosition) {
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
    protected TeaseScriptBase(TeaseLib teaseLib, ResourceLoader resources, Actor actor, String namespace) {
        this.teaseLib = teaseLib;
        this.resources = resources;
        this.actor = actor;
        this.namespace = namespace.replace(" ", "_");

        TextToSpeechPlayer textToSpeech = TextToSpeechPlayer.instance();
        // TODO Does nothing if already loaded, so we could merge the constructors
        textToSpeech.loadActorVoiceProperties(resources);
        textToSpeech.acquireVoice(actor);
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

        TextToSpeechPlayer textToSpeech = TextToSpeechPlayer.instance();
        textToSpeech.acquireVoice(actor);
    }

    protected static List<String> buildChoicesFromArray(String choice, String... more) {
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
     * Just wait for everything to be rendered (messages displayed, sounds played, delay expired), and continue
     * execution of the script.
     * <p>
     * This won't display a button, it just waits. Background threads will continue to run.
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
                    new Message(actor, expandTextVariables(Arrays.asList(text))), teaseLib);
            renderMessage(interTitle);
        } finally {
            displayImage = Message.ActorImage;
            mood = Mood.Neutral;
        }
    }

    protected void renderMessage(Message message, TextToSpeechPlayer ttsPlayer) {
        try {
            // inject speech parts to replay pre-recorded speech or use TTS
            // This has to be done first as subsequent parse steps
            // inject moods and thus change the message hash
            if (ttsPlayer != null) {
                if (ttsPlayer.prerenderedSpeechAvailable(message.actor)) {
                    // Don't use TTS, even if pre-recorded speech is missing
                    message = ttsPlayer.createPrerenderedSpeechMessage(message, resources);
                }
            }
            Message parsedMessage = injectImagesAndExpandTextVariables(message);
            renderMessage(new RenderMessage(resources, parsedMessage, ttsPlayer, teaseLib));
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
                List<MediaRenderer> nextSet = new ArrayList<MediaRenderer>(queuedRenderers);
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

    public Message injectImagesAndExpandTextVariables(Message message) {
        // Clone the actor to prevent the wrong actor image to be displayed
        // when changing the actor images right after rendering a message.
        // Without cloning one of the new actor images would be displayed
        // with the current message because the actor is shared between
        // script and message
        Message parsedMessage = new Message(new Actor(message.actor));

        // TODO hint actor aspect, camera position, posture

        if (message.isEmpty()) {
            ensureEmptyMessageContainsDisplayImage(parsedMessage, getActorOrDisplayImage(displayImage, mood));
        } else {
            String imageType = displayImage;
            String nextImage = null;
            String lastMood = null;
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
                        final String currentMood;
                        if (nextMood == null) {
                            currentMood = mood;
                        } else {
                            currentMood = nextMood;
                        }
                        // Inject mood if changed
                        if (currentMood != lastMood) {
                            parsedMessage.add(Message.Type.Mood, currentMood);
                            lastMood = currentMood;
                        }
                        imageType = nextImage = getActorOrDisplayImage(part.value, currentMood);
                        parsedMessage.add(part.type, nextImage);
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
                    // set mood if not done already
                    final String currentMood;
                    if (nextMood == null) {
                        currentMood = mood;
                    } else {
                        currentMood = nextMood;
                        nextMood = null;
                    }
                    // Inject mood if changed
                    if (currentMood != lastMood) {
                        parsedMessage.add(Message.Type.Mood, currentMood);
                        lastMood = currentMood;
                    }
                    // Update image if changed
                    if (imageType != nextImage) {
                        nextImage = getActorOrDisplayImage(imageType, currentMood);
                        parsedMessage.add(Message.Type.Image, nextImage);
                    }
                    // Replace text variables
                    parsedMessage.add(new Message.Part(part.type, expandTextVariables(part.value)));
                } else {
                    parsedMessage.add(part);
                }
            }

            if (parsedMessage.isEmpty()) {
                ensureEmptyMessageContainsDisplayImage(parsedMessage, getActorOrDisplayImage(imageType, mood));
            }
        }

        return parsedMessage;
    }

    private String getActorOrDisplayImage(String imageType, String currentMood) {
        final String nextImage;
        if (imageType == Message.ActorImage
                && Boolean.parseBoolean(teaseLib.config.get(Config.Render.ActorImages)) == false) {
            nextImage = Message.NoImage;
        } else if (imageType != Message.NoImage
                && Boolean.parseBoolean(teaseLib.config.get(Config.Render.InstructionalImages)) == false) {
            nextImage = Message.NoImage;
        } else if (imageType == Message.ActorImage) {
            if (actor.images.hasNext()) {
                actor.images.hint(currentMood);
                nextImage = actor.images.next();
            } else {
                nextImage = Message.NoImage;
            }
        } else {
            nextImage = imageType;
        }
        return nextImage;
    }

    private static void ensureEmptyMessageContainsDisplayImage(Message parsedMessage, String nextImage) {
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
    protected final String showChoices(ScriptFunction scriptFunction, List<String> choices) {
        return showChoices(scriptFunction, Confidence.Default, choices);
    }

    /**
     * Shows choices.
     * 
     * @param scriptFunction
     *            The script function to executed while waiting for the user input. This parameter may be null, in this
     *            case the choices are only shown after all renderers have completed their mandatory parts
     * @param recognitionConfidence
     *            The confidence threshold used for speech recognition.
     * @param choice
     *            The first choice. This function doesn't make sense without showing at least one item, so one choice is
     *            mandatory
     * @return The choice made by the user, {@link ScriptFunction#Timeout} if the function has ended, or a custom result
     *         value set by the script function.
     */
    protected String showChoices(ScriptFunction scriptFunction, Confidence recognitionConfidence,
            List<String> choices) {
        // argument checking and text variable replacement
        final List<String> derivedChoices = expandTextVariables(choices);

        waitToStartScriptFunction(scriptFunction);
        if (scriptFunction == null) {
            stopBackgroundRenderers();
        } else if (scriptFunction.relation != ScriptFunction.Relation.Autonomous) {
            stopBackgroundRenderers();
        }

        List<InputMethod> inputMethods = new ArrayList<InputMethod>();
        inputMethods.add(teaseLib.hostInputMethod);

        SpeechRecognition sR = SpeechRecognizer.instance.get(actor.getLocale());
        if (sR.isReady() && Boolean.parseBoolean(teaseLib.config.get(Config.InputMethod.SpeechRecognition))) {
            inputMethods.add(new SpeechRecognitionInputMethod(this, sR, recognitionConfidence));
        }

        Prompt prompt = new Prompt(new Choices(choices), new Choices(derivedChoices), scriptFunction, inputMethods);
        logger.info("Prompt: " + prompt);
        for (InputMethod inputMethod : inputMethods) {
            logger.info(inputMethod.getClass().getSimpleName() + " " + inputMethod.toString());
        }
        String choice = teaseLib.shower.show(this, prompt);

        logger.debug("Reply finished");
        teaseLib.transcript.info("< " + choice);
        return choice;
    }

    public Replay getReplay() {
        return new ReplayImpl(playedRenderers);
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
        return new TextVariables(TextVariables.Defaults, teaseLib.getTextVariables(actor.getLocale()),
                actor.textVariables);
    }
}
