package teaselib.core;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import teaselib.Actor;
import teaselib.Config;
import teaselib.Gadgets;
import teaselib.Message;
import teaselib.Mood;
import teaselib.Replay;
import teaselib.ScriptFunction;
import teaselib.core.media.MediaRenderer;
import teaselib.core.media.MediaRendererQueue;
import teaselib.core.media.RenderInterTitle;
import teaselib.core.media.RenderMessage;
import teaselib.core.speechrecognition.SpeechRecognitionResult.Confidence;
import teaselib.core.speechrecognition.SpeechRecognizer;
import teaselib.core.texttospeech.TextToSpeechPlayer;
import teaselib.core.ui.Choice;
import teaselib.core.ui.Choices;
import teaselib.core.ui.InputMethod;
import teaselib.core.ui.InputMethods;
import teaselib.core.ui.Prompt;
import teaselib.core.ui.Shower;
import teaselib.core.ui.SpeechRecognitionInputMethod;
import teaselib.core.util.ExceptionUtil;
import teaselib.core.util.ObjectMap;
import teaselib.motiondetection.Gesture;
import teaselib.motiondetection.MotionDetector;
import teaselib.util.SpeechRecognitionRejectedScript;
import teaselib.util.TextVariables;

public abstract class Script {
    private static final Logger logger = LoggerFactory.getLogger(Script.class);

    public final TeaseLib teaseLib;
    public final ResourceLoader resources;

    public final Actor actor;
    public final String namespace;

    protected String mood = Mood.Neutral;
    protected String displayImage = Message.ActorImage;

    protected static final int NoTimeout = 0;

    private final MediaRendererQueue renderQueue;
    private final List<MediaRenderer> queuedRenderers = new ArrayList<>();
    private final List<MediaRenderer.Threaded> backgroundRenderers = new ArrayList<>();

    private List<MediaRenderer> playedRenderers = null;

    public class ReplayImpl implements Replay {
        final List<MediaRenderer> renderers;

        public ReplayImpl(List<MediaRenderer> renderers) {
            super();
            logger.info("Remembering renderers in replay " + this);
            this.renderers = new ArrayList<>(renderers);
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
                renderQueue.replay(renderers, replayPosition);
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
    protected Script(TeaseLib teaseLib, ResourceLoader resources, Actor actor, String namespace) {
        this(teaseLib, resources, actor, namespace,
                teaseLib.globals.store(MediaRendererQueue.class, MediaRendererQueue::new).get(MediaRendererQueue.class),
                teaseLib.globals.store(TextToSpeechPlayer.class, () -> new TextToSpeechPlayer(teaseLib.config))
                        .get(TextToSpeechPlayer.class));

        teaseLib.globals.store(Shower.class, () -> new Shower(teaseLib.host));
        teaseLib.globals.store(InputMethods.class, InputMethods::new);
        teaseLib.globals.store(SpeechRecognizer.class, () -> new SpeechRecognizer(teaseLib.config));
    }

    /**
     * Construct a script with a different actor but with shared resources
     * 
     * @param script
     * @param actor
     */
    protected Script(Script script, Actor actor) {
        this(script.teaseLib, script.resources, actor, script.namespace,
                script.teaseLib.globals.get(MediaRendererQueue.class),
                script.teaseLib.globals.get(TextToSpeechPlayer.class));
    }

    private Script(TeaseLib teaseLib, ResourceLoader resources, Actor actor, String namespace,
            MediaRendererQueue renderQueue, TextToSpeechPlayer textToSpeech) {
        this.teaseLib = teaseLib;
        this.resources = resources;
        this.actor = actor;
        this.renderQueue = renderQueue;
        this.namespace = namespace.replace(" ", "_");

        textToSpeech.acquireVoice(actor, resources);
    }

    protected static List<String> buildChoicesFromArray(String choice, String... more) {
        List<String> choices = new ArrayList<>(1 + more.length);
        choices.add(choice);
        choices.addAll(Arrays.asList(more));
        return choices;
    }

    private static final String SCRIPT_INSTANCES = "ScriptInstances";

    public <T extends Script> T script(Class<T> scriptClass) {
        ObjectMap scripts = getScriptStorage();
        return getScript(scriptClass, scripts);
    }

    private ObjectMap getScriptStorage() {
        ObjectMap scripts = teaseLib.globals.get(SCRIPT_INSTANCES);
        if (scripts == null) {
            scripts = teaseLib.globals.store(SCRIPT_INSTANCES, new ObjectMap());
        }
        return scripts;
    }

    private <T extends Script> T getScript(Class<T> scriptClass, ObjectMap scripts) {
        T script = scripts.get(scriptClass);
        if (script == null) {
            try {
                Constructor<T> constructor = findScriptConstructor(scriptClass);
                script = scripts.store(constructor.newInstance(this));
            } catch (ReflectiveOperationException e) {
                throw ExceptionUtil.asRuntimeException(e);
            }
        }
        return script;
    }

    @SuppressWarnings("unchecked")
    private <T extends Script> Constructor<T> findScriptConstructor(Class<T> scriptClass) {
        Class<?> type = this.getClass();
        while (type != null) {
            for (Constructor<T> constructor : (Constructor<T>[]) scriptClass.getConstructors()) {
                Class<?>[] parameterTypes = constructor.getParameterTypes();
                if (parameterTypes.length == 1 && parameterTypes[0] == type) {
                    return constructor;
                }
            }
            type = type.getSuperclass();
        }

        throw new NoSuchMethodError("Constructor " + scriptClass.getName() + "(" + this.getClass().getName() + ")");
    }

    public void completeStarts() {
        renderQueue.completeStarts();
    }

    public void completeMandatory() {
        renderQueue.completeMandatories();
    }

    /**
     * Just wait for everything to be rendered (messages displayed, sounds played, delay expired), and continue
     * execution of the script.
     * <p>
     * This won't display a button, it just waits. Background threads will continue to run.
     */
    public void completeAll() {
        renderQueue.completeAll();
        renderQueue.endAll();
    }

    /**
     * Stop rendering and end all render threads
     */
    public void endAll() {
        renderQueue.endAll();
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

    protected void renderMessage(Message message, boolean useTTS) {
        try {
            Optional<TextToSpeechPlayer> textToSpeech = useTTS
                    ? Optional.of(teaseLib.globals.get(TextToSpeechPlayer.class))
                    : Optional.empty();
            // inject speech parts to play pre-recorded speech audio render TTS first
            // as subsequent injections add mood thereby changing the message hash
            boolean applySpeech = useTTS && textToSpeech.isPresent();
            renderMessage(new RenderMessage(teaseLib, resources, textToSpeech, injectImagesAndExpandTextVariables(
                    applySpeech ? textToSpeech.get().createSpeechMessage(message, resources) : message)));
        } finally {
            displayImage = Message.ActorImage;
            mood = Mood.Neutral;
        }
    }

    private void renderMessage(MediaRenderer renderMessage) {
        synchronized (renderQueue) {
            synchronized (queuedRenderers) {
                queueRenderer(renderMessage);
                // Remember this set for replay
                playedRenderers = new ArrayList<>(queuedRenderers);
                // Remember in order to clear queued before completing
                // previous set
                List<MediaRenderer> nextSet = new ArrayList<>(queuedRenderers);
                // Must clear queue for next set before completing current,
                // because if the current set is cancelled,
                // the next set must be discarded
                queuedRenderers.clear();
                // Now the current set can be completed, and canceling the
                // current set will result in an empty next set
                completeAll();

                // Start a new message in the log
                teaseLib.transcript.info("");
                renderQueue.start(nextSet);
            }
            startBackgroundRenderers();
            renderQueue.completeStarts();
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
                    parsedMessage.add(part);
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
                } else if (part.type == Message.Type.DesktopItem
                        && !Boolean.parseBoolean(teaseLib.config.get(Config.Render.InstructionalImages))) {
                    // Ignore
                } else if (part.type == Message.Type.Sound
                        && !Boolean.parseBoolean(teaseLib.config.get(Config.Render.Sound))) {
                    // Ignore
                } else if (part.type == Message.Type.BackgroundSound
                        && !Boolean.parseBoolean(teaseLib.config.get(Config.Render.Sound))) {
                    // Ignore
                } else if (part.type == Message.Type.Speech
                        && !Boolean.parseBoolean(teaseLib.config.get(Config.Render.Speech))) {
                    // Ignore
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

    protected final String showChoices(Choices choices) {
        return showChoices(choices, null);
    }

    /**
     * {@code recognitionConfidence} defaults to {@link Confidence#Default}
     * 
     * @see Script#showChoices(List, ScriptFunction, Confidence)
     */
    protected final String showChoices(Choices choices, ScriptFunction scriptFunction) {
        return showChoices(choices, scriptFunction, Confidence.Default);
    }

    /**
     * Shows choices.
     * 
     * @param scriptFunction
     *            The script function to executed while waiting for the user input. This parameter may be null, in this
     *            case the choices are only shown after all renderers have completed their mandatory parts
     * @param recognitionConfidence
     *            The confidence threshold used for speech recognition.
     * @param text
     *            The first choice. This function doesn't make sense without showing at least one item, so one choice is
     *            mandatory
     * 
     * @return The choice made by the user, {@link ScriptFunction#Timeout} if the function has ended, or a custom result
     *         value set by the script function.
     */
    protected String showChoices(Choices choices, ScriptFunction scriptFunction, Confidence recognitionConfidence) {
        Prompt prompt = getPrompt(scriptFunction, recognitionConfidence, choices);
        return showPrompt(prompt, scriptFunction);
    }

    protected Choice choice(Gesture gesture, String yes) {
        return new Choice(gesture, yes, expandTextVariables(yes));
    }

    private String showPrompt(Prompt prompt, ScriptFunction scriptFunction) {
        waitToStartScriptFunction(scriptFunction);
        if (scriptFunction == null) {
            stopBackgroundRenderers();
        } else if (scriptFunction.relation != ScriptFunction.Relation.Autonomous) {
            stopBackgroundRenderers();
        }

        String choice;
        try {
            choice = teaseLib.globals.get(Shower.class).show(this, prompt);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ScriptInterruptedException();
        }
        endAll();
        teaseLib.host.endScene();

        logger.debug("Reply finished");
        teaseLib.transcript.info("< " + choice);

        return choice;
    }

    private Prompt getPrompt(ScriptFunction scriptFunction, Confidence recognitionConfidence, Choices choices) {
        InputMethods inputMethods = new InputMethods(teaseLib.globals.get(InputMethods.class));
        inputMethods.add(teaseLib.host.inputMethod());

        if (Boolean.parseBoolean(teaseLib.config.get(Config.InputMethod.SpeechRecognition))) {
            inputMethods.add(new SpeechRecognitionInputMethod(
                    teaseLib.globals.get(SpeechRecognizer.class).get(actor.locale()), recognitionConfidence,
                    Optional.ofNullable(actor.speechRecognitionRejectedScript != null
                            ? speechRecognitioneRejectedScript(scriptFunction)
                            : null)));
        }

        if (teaseLib.item(TeaseLib.DefaultDomain, Gadgets.Webcam).isAvailable()) {
            MotionDetector motionDetector = teaseLib.devices.get(MotionDetector.class).getDefaultDevice();
            inputMethods.add(motionDetector.getInputMethod());
        }

        Prompt prompt = new Prompt(choices, scriptFunction, inputMethods);
        logger.info("Prompt: " + prompt);
        for (InputMethod inputMethod : inputMethods) {
            logger.info(inputMethod.getClass().getSimpleName() + " " + inputMethod.toString());
        }
        return prompt;
    }

    public SpeechRecognitionRejectedScript speechRecognitioneRejectedScript(ScriptFunction scriptFunction) {
        return new SpeechRecognitionRejectedScript(this) {
            @Override
            public void run() {
                Script script = Script.this;
                script.endAll();
                Replay beforeSpeechRecognitionRejected = script.getReplay();
                script.actor.speechRecognitionRejectedScript.run();
                beforeSpeechRecognitionRejected.replay(Replay.Position.End);
            }

            // Handling speech recognition rejected events:
            // RecognitionRejectedEvent-scripts doesn't work in reply-calls that
            // invoke script functions but they work inside script functions.
            //
            // Reason are:
            // - The event handler would have to wait until messages rendered by
            // the script function are completed -> delay in response
            // - script functions may include timing which would be messed up by
            // pausing them
            // - Script functions may invoke other script functions, but the
            // handler management is neither multi-threading-aware nor
            // synchronized
            // - The current code is unable to recover to the choice on top of
            // the choices stack after a recognition-rejected pause event
            //
            // The recognitionRejected handler won't trigger immediately when
            // a script function renders messages, because it will wait until
            // the render queue is empty, and this includes message delays.
            // Therefore script functions are not supported, because the script
            // function would still render messages while the choices are shown.
            // However rendering messages while showing choices should be fine.
            @Override
            public boolean canRun() {
                SpeechRecognitionRejectedScript speechRecognitionRejectedScript = actor.speechRecognitionRejectedScript;
                // TODO Must search pause stack for speech recognition rejected scripts
                // - handlers of the same type shouldn't stack up -> handle in Shower
                boolean choicesStackContainsSRRejectedState = false;

                if (choicesStackContainsSRRejectedState == true) {
                    log(speechRecognitionRejectedScript,
                            "The choices stack contains already another SR rejection script");
                    return false;
                } else if (scriptFunction != null) {
                    // This would work for the built-in confirmative timeout script functions:
                    // - TimeoutBehavior.InDubioMitius and maybe also for
                    // - TimeoutBehavior.TimeoutBehavior.InDubioMitius
                    log(speechRecognitionRejectedScript,
                            scriptFunction.relation.toString() + " script functions running");
                    return false;
                } else if (!teaseLib.globals.get(MediaRendererQueue.class).hasCompletedMandatory()) {
                    // must complete all to avoid parallel rendering, see {@link Message#ShowChoices}
                    log(speechRecognitionRejectedScript, "Message rendering still in progress");
                    return false;
                } else if (speechRecognitionRejectedScript.canRun() == false) {
                    log(speechRecognitionRejectedScript,
                            "RecognitionRejectedScript  .canRun() returned false - skipping");
                    return false;
                } else {
                    return true;
                }
            }

            private void log(Script speechRecognitionRejectedScript, String message) {
                if (logger.isInfoEnabled()) {
                    String skipping = " - skipping RecognitionRejectedScript "
                            + speechRecognitionRejectedScript.toString();
                    logger.info(message + skipping);
                }
            }
        };
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

    protected String expandTextVariables(String s) {
        return allTextVariables().expand(s);
    }

    private TextVariables allTextVariables() {
        return new TextVariables(TextVariables.Defaults, teaseLib.getTextVariables(actor.locale()),
                actor.textVariables);
    }
}
