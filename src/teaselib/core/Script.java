package teaselib.core;

import static java.util.stream.Collectors.toList;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import teaselib.Actor;
import teaselib.Answer;
import teaselib.Body;
import teaselib.Config;
import teaselib.Duration;
import teaselib.Gadgets;
import teaselib.Message;
import teaselib.Mood;
import teaselib.Replay;
import teaselib.ScriptFunction;
import teaselib.State;
import teaselib.core.ai.perception.HumanPose;
import teaselib.core.ai.perception.HumanPose.Proximity;
import teaselib.core.ai.perception.HumanPoseDeviceInteraction;
import teaselib.core.ai.perception.HumanPoseDeviceInteraction.PoseAspects;
import teaselib.core.devices.release.KeyReleaseDeviceInteraction;
import teaselib.core.devices.remote.LocalNetworkDevice;
import teaselib.core.media.RenderedMessage.Decorator;
import teaselib.core.media.ScriptMessageDecorator;
import teaselib.core.speechrecognition.Confidence;
import teaselib.core.speechrecognition.SpeechRecognizer;
import teaselib.core.speechrecognition.events.SpeechRecognizedEventArgs;
import teaselib.core.texttospeech.TextToSpeechPlayer;
import teaselib.core.ui.Choice;
import teaselib.core.ui.Choices;
import teaselib.core.ui.HeadGestureInputMethod;
import teaselib.core.ui.InputMethod;
import teaselib.core.ui.InputMethodEventArgs;
import teaselib.core.ui.InputMethods;
import teaselib.core.ui.Intention;
import teaselib.core.ui.Prompt;
import teaselib.core.ui.Prompt.Action;
import teaselib.core.ui.Shower;
import teaselib.core.ui.SpeechRecognitionInputMethod;
import teaselib.core.ui.SpeechRecognitionInputMethodEventArgs;
import teaselib.core.util.ExceptionUtil;
import teaselib.motiondetection.MotionDetector;
import teaselib.util.ItemGuid;
import teaselib.util.SpeechRecognitionRejectedScript;
import teaselib.util.TextVariables;
import teaselib.util.math.Hysteresis;

public abstract class Script {
    private static final Logger logger = LoggerFactory.getLogger(Script.class);

    public final TeaseLib teaseLib;
    public final ResourceLoader resources;

    public final Actor actor;
    public final String namespace;

    public final ScriptRenderer scriptRenderer;

    protected String mood = Mood.Neutral;
    protected String displayImage = Message.ActorImage;

    /**
     * Construct a new main-script instance
     * 
     * @param teaseLib
     * @param locale
     */
    @SuppressWarnings("resource")
    protected Script(TeaseLib teaseLib, ResourceLoader resources, Actor actor, String namespace) {
        this(teaseLib, resources, actor, namespace, //
                getOrDefault(teaseLib, ScriptRenderer.class, () -> new ScriptRenderer(teaseLib)));

        getOrDefault(teaseLib, Shower.class, () -> new Shower(teaseLib.host));
        getOrDefault(teaseLib, InputMethods.class, InputMethods::new);
        getOrDefault(teaseLib, DeviceInteractionImplementations.class, this::initScriptInteractions);
        // getOrDefault(teaseLib, TeaseLibAI.class, TeaseLibAI::new);

        try {
            teaseLib.config.addScriptSettings(namespace, namespace);
        } catch (IOException e) {
            ExceptionUtil.handleException(e, teaseLib.config, logger);
        }

        boolean startOnce = teaseLib.globals.get(ScriptCache.class) == null;
        if (startOnce) {
            syncAudioAndSpeechRecognition(teaseLib);
            handleAutoRemove();
            // bindMotionDetectorToVideoRenderer();
            bindNetworkProperties();
        }

        // TODO for multiple actors within the same namespace,
        // this needs to be moved to the Script(Script ...) constructors
        scriptRenderer.events.when().actorChanged(actor).thenOnce(e -> {
            if (e.actor == actor) {
                // TODO DebugSetup.applyTo(config) must set only defined aspects
                // - currently it's always enabled
                // -> massive change to test scripts
                // Configuration persistent properties fails to lookup values in default properties file
                if (Boolean.parseBoolean(teaseLib.config.get(Config.InputMethod.HeadGestures))) {
                    defineHumanPoseInteractions(actor);
                }
            }
        });
        // TODO initializing in actorChanged-event breaks device handling
        defineKeyReleaseInteractions(actor);
    }

    protected void defineHumanPoseInteractions(Actor actor) {
        HumanPoseDeviceInteraction humanPoseInteraction = deviceInteraction(HumanPoseDeviceInteraction.class);
        humanPoseInteraction.definitions(actor).define(HumanPose.Interests.Proximity,
                new HumanPoseDeviceInteraction.Reaction(HumanPose.Interests.Proximity, new Consumer<PoseAspects>() {

                    private final Function<Boolean, Float> awareness = Hysteresis
                            .bool(Hysteresis.function(0.0f, 1.0f, 0.25f), 1.0f, 0.0f);

                    @Override
                    public void accept(PoseAspects pose) {

                        SpeechRecognitionInputMethod inputMethod = teaseLib.globals.get(InputMethods.class)
                                .get(SpeechRecognitionInputMethod.class);
                        boolean speechProximity = awareness.apply(pose.is(Proximity.FACE2FACE)) > 0.5f;
                        inputMethod.setFaceToFace(speechProximity);
                        if (speechProximity) {
                            scriptRenderer.makeActive();
                        } else {
                            scriptRenderer.makeInactive();
                        }

                    }
                }));
    }

    private void defineKeyReleaseInteractions(Actor actor) {
        deviceInteraction(KeyReleaseDeviceInteraction.class).setDefaults(actor);
    }

    private DeviceInteractionImplementations initScriptInteractions() {
        DeviceInteractionImplementations scriptInteractionImplementations = new DeviceInteractionImplementations();
        scriptInteractionImplementations.add(KeyReleaseDeviceInteraction.class,
                () -> new KeyReleaseDeviceInteraction(teaseLib, scriptRenderer));
        scriptInteractionImplementations.add(HumanPoseDeviceInteraction.class,
                () -> new HumanPoseDeviceInteraction(scriptRenderer));
        return scriptInteractionImplementations;
    }

    private void syncAudioAndSpeechRecognition(TeaseLib teaseLib) {
        InputMethods inputMethods = teaseLib.globals.get(InputMethods.class);
        AudioSync audioSync = scriptRenderer.audioSync;
        inputMethods.add(new SpeechRecognitionInputMethod(new SpeechRecognizer(teaseLib.config, audioSync)));
    }

    protected void handleAutoRemove() {
        long startupTimeSeconds = teaseLib.getTime(TimeUnit.SECONDS);
        State persistedDomains = teaseLib.state(TeaseLib.DefaultDomain, StateImpl.PERSISTED_DOMAINS);
        Collection<Object> domains = new ArrayList<>(((StateImpl) persistedDomains).peers());
        for (Object domain : domains) {
            domain = domain.equals(StateImpl.DEFAULT_DOMAIN_NAME) ? TeaseLib.DefaultDomain : domain;
            if (!handleUntilRemoved((String) domain, startupTimeSeconds).applied()
                    && !handleUntilExpired((String) domain).applied()) {
                persistedDomains.removeFrom(domain);
            }
        }
    }

    private State handleUntilRemoved(String domain, long startupTimeSeconds) {
        State untilRemoved = teaseLib.state(domain, State.Persistence.Until.Removed);
        Set<Object> persistedUntilRemoved = ((StateImpl) untilRemoved).peers();

        for (Object item : new ArrayList<>(persistedUntilRemoved)) {
            State state = teaseLib.state(domain, item);
            try {
                // Very implicit way of testing for unavailable items
                Duration duration = state.duration();
                if (state.applied() && duration.expired()) {
                    long limit = duration.limit(TimeUnit.SECONDS);
                    if (limit > State.TEMPORARY) {
                        long autoRemovalTime = duration.end(TimeUnit.SECONDS) + limit / 2;
                        if (autoRemovalTime <= startupTimeSeconds) {
                            state.remove();
                        }
                    }
                }
            } catch (IllegalArgumentException e) {
                removeUnavailableItem((StateImpl) state);
            }
        }

        return untilRemoved;
    }

    private State handleUntilExpired(String domain) {
        State untilExpired = teaseLib.state(domain, State.Persistence.Until.Expired);
        Set<Object> peers = ((StateImpl) untilExpired).peers();

        for (Object object : new ArrayList<>(peers)) {
            State state = teaseLib.state(domain, object);
            try {
                // Very implicit way of testing for unavailable items
                Duration duration = state.duration();
                if (state.applied() && duration.expired()) {
                    state.remove();
                }
            } catch (IllegalArgumentException e) {
                removeUnavailableItem((StateImpl) state);
            }
        }

        return untilExpired;
    }

    private void removeUnavailableItem(StateImpl state) {
        // User items configuration changed - item not available anymore
        // Only items are looked up in user configuration - states are programmatically definable
        List<ItemGuid> guids = state.peers().stream().filter(peer -> peer instanceof ItemGuid)
                .map(peer -> (ItemGuid) peer).collect(toList());

        String name = guids.stream().map(ItemGuid::name).findFirst().orElse("???");
        logger.warn("User items configuration changed: Item {} \"{}\" not available anymore", state.item, name);

        guids.stream().forEach(guid -> {
            state.peers().stream().forEach(peer -> StateImpl.StateStorage.delete(teaseLib, state.domain, peer));
        });
        state.remove();
    }

    private static <T> T getOrDefault(TeaseLib teaseLib, Class<T> clazz, Supplier<T> supplier) {
        return teaseLib.globals.getOrDefault(clazz, supplier);
    }

    private void bindNetworkProperties() {
        if (Boolean.parseBoolean(teaseLib.config.get(LocalNetworkDevice.Settings.EnableDeviceDiscovery))) {
            teaseLib.devices.get(LocalNetworkDevice.class).getDevicePaths();
        }
    }

    /**
     * Construct a script with a different actor but with shared resources
     * 
     * @param script
     * @param actor
     */
    protected Script(Script script, Actor actor) {
        this(script.teaseLib, script.resources, actor, script.namespace, script.scriptRenderer);
    }

    private Script(TeaseLib teaseLib, ResourceLoader resources, Actor actor, String namespace,
            ScriptRenderer scriptRenderer) {
        this.teaseLib = teaseLib;
        this.resources = resources;
        this.actor = actor;
        this.scriptRenderer = scriptRenderer;
        this.namespace = namespace.replace(" ", "_");

        Optional<TextToSpeechPlayer> textToSpeech = getTextToSpeech();
        if (textToSpeech.isPresent()) {
            textToSpeech.get().acquireVoice(actor, resources);
        }
    }

    public <T extends Script> T script(Class<T> scriptClass) {
        ScriptCache scripts = teaseLib.globals.getOrDefault(ScriptCache.class, ScriptCache::new);
        return scripts.get(this, scriptClass);
    }

    public <T extends ScriptInteraction> T interaction(Class<T> scriptInteractionClass) {
        ScriptInteractionCache scriptInteractions = teaseLib.globals.getOrDefault(ScriptInteractionCache.class,
                ScriptInteractionCache::new);
        return scriptInteractions.get(this, scriptInteractionClass);
    }

    private <T extends DeviceInteractionImplementation<?, ?>> T deviceInteraction(Class<T> deviceInteraction) {
        return teaseLib.globals.get(DeviceInteractionImplementations.class).get(deviceInteraction);
    }

    public void completeStarts() {
        scriptRenderer.completeStarts();
    }

    public void completeMandatory() {
        scriptRenderer.completeMandatory();
    }

    /**
     * Just wait for everything to be rendered (messages displayed, sounds played, delay expired), and continue
     * execution of the script.
     * <p>
     * This won't display a button, it just waits. Background threads will continue to run.
     */
    public void completeAll() {
        scriptRenderer.completeAll();
    }

    /**
     * Stop rendering and end all render threads
     */
    public void endAll() {
        scriptRenderer.endAll();
    }

    protected void renderIntertitle(String... text) {
        try {
            scriptRenderer.renderIntertitle(teaseLib, new Message(actor, expandTextVariables(Arrays.asList(text))));
        } finally {
            displayImage = Message.ActorImage;
            mood = Mood.Neutral;
        }
    }

    protected void prependMessage(Message message) {
        scriptRenderer.prependMessage(message);
    }

    protected void renderMessage(Message message, boolean useTTS) {
        Optional<TextToSpeechPlayer> textToSpeech = getTextToSpeech(useTTS);
        try {
            scriptRenderer.renderMessage(teaseLib, resources, message, decorators(textToSpeech));
        } finally {
            displayImage = Message.ActorImage;
            mood = Mood.Neutral;
        }
    }

    private Optional<TextToSpeechPlayer> getTextToSpeech() {
        return Optional.ofNullable(scriptRenderer.sectionRenderer.textToSpeechPlayer);
    }

    private Optional<TextToSpeechPlayer> getTextToSpeech(boolean useTTS) {
        return useTTS ? getTextToSpeech() : Optional.empty();
    }

    Decorator[] decorators(Optional<TextToSpeechPlayer> textToSpeech) {
        return new ScriptMessageDecorator(teaseLib.config, displayImage, actor, mood, resources,
                this::expandTextVariables, textToSpeech).messageModifiers();
    }

    protected void appendMessage(Message message) {
        scriptRenderer.appendMessage(teaseLib, resources, actor, message, decorators(getTextToSpeech()));
    }

    protected void replaceMessage(Message message) {
        scriptRenderer.replaceMessage(teaseLib, resources, actor, message, decorators(getTextToSpeech()));
    }

    protected final Answer showChoices(List<Answer> answers) {
        return showChoices(answers, null);
    }

    /**
     * {@code recognitionConfidence} defaults to {@link Confidence#Default}
     * 
     * @see Script#showChoices(List, ScriptFunction, Confidence)
     */
    protected final Answer showChoices(List<Answer> answers, ScriptFunction scriptFunction) {
        return showChoices(answers, scriptFunction,
                scriptFunction != null || answers.size() > 1 ? Intention.Decide : Intention.Confirm);
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
    protected Answer showChoices(List<Answer> answers, ScriptFunction scriptFunction, Intention intention) {
        if (scriptRenderer.hasPrependedMessages()) {
            Optional<TextToSpeechPlayer> textToSpeech = getTextToSpeech(true);
            scriptRenderer.renderPrependedMessages(teaseLib, resources, actor, decorators(textToSpeech));
        }

        InputMethods inputMethods = getInputMethods(answers);
        Choices choices = choices(answers, intention);
        Prompt prompt = getPrompt(choices, inputMethods, scriptFunction);

        waitToStartScriptFunction(scriptFunction);
        if (scriptFunction == null || scriptFunction.relation != ScriptFunction.Relation.Autonomous) {
            scriptRenderer.stopBackgroundRenderers();
        }

        Optional<SpeechRecognitionRejectedScript> speechRecognitionRejectedScript = speechRecognitioneRejectedScript(
                scriptFunction);
        if (speechRecognitionRejectedScript.isPresent()) {
            addRecognitionRejectedAction(prompt, speechRecognitionRejectedScript.get());
        }

        Choice choice = showPrompt(prompt).get(0);

        String chosen = "< " + choice;
        logger.info("{}", chosen);
        teaseLib.transcript.info(chosen);

        return choice.answer;
    }

    private static void addRecognitionRejectedAction(Prompt prompt, SpeechRecognitionRejectedScript script) {
        prompt.when(SpeechRecognitionInputMethod.Notification.RecognitionRejected).run(new Action() {
            boolean speechRecognitionRejectedHandlerSignaled = false;

            @Override
            public void run(InputMethodEventArgs e) {
                SpeechRecognizedEventArgs eventArgs = ((SpeechRecognitionInputMethodEventArgs) e).eventArgs;
                if (eventArgs.result != null && eventArgs.result.size() == 1) {
                    if (!speechRecognitionRejectedHandlerSignaled && script.canRun()) {
                        // TODO generalize this -> invoke action once for this prompt, once for whole prompt stack
                        speechRecognitionRejectedHandlerSignaled = true;
                        script.run();
                    }
                }
            }
        });
    }

    private Choices choices(List<Answer> answers, Intention intention) {
        List<Choice> choices = answers.stream().map(answer -> new Choice(answer,
                expandTextVariables(selectPhrase(answer)), expandTextVariables(answer.text)))
                .collect(Collectors.toList());

        return new Choices(actor.locale(), intention, choices);
    }

    private String selectPhrase(Answer answer) {
        // TODO Use a random function that is not used for control flow
        return answer.text.get(teaseLib.random(0, answer.text.size() - 1));
    }

    private List<Choice> showPrompt(Prompt prompt) {
        scriptRenderer.events.beforeChoices.fire(new ScriptEventArgs());

        List<Choice> choice;
        try {
            choice = teaseLib.globals.get(Shower.class).show(prompt);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ScriptInterruptedException();
        }
        // TODO endAll() cancels renderers, but doesn't wait for completion
        // -> integrate this with endScene() which is just there to workaround the delay until the next say() command
        endAll();

        scriptRenderer.events.afterChoices.fire(new ScriptEventArgs());

        teaseLib.host.endScene();

        return choice;
    }

    private InputMethods getInputMethods(List<Answer> answers) {
        InputMethods inputMethods = new InputMethods(teaseLib.globals.get(InputMethods.class));
        inputMethods.add(teaseLib.host.inputMethod());

        // TODO Move this into head gestures method
        if (teaseLib.item(TeaseLib.DefaultDomain, Gadgets.Webcam).isAvailable()
                && teaseLib.state(TeaseLib.DefaultDomain, Body.InMouth).applied()
                && HeadGestureInputMethod.distinctGestures(answers)) {
            inputMethods.add(new HeadGestureInputMethod(scriptRenderer.getInputMethodExecutorService(),
                    teaseLib.devices.get(MotionDetector.class)::getDefaultDevice));
        }

        inputMethods.add(scriptRenderer.scriptEventInputMethod);

        return inputMethods;

    }

    private Prompt getPrompt(Choices choices, InputMethods inputMethods, ScriptFunction scriptFunction) {
        Prompt prompt = new Prompt(this, choices, inputMethods, scriptFunction, Prompt.Result.Accept.Distinct);
        logger.info("Prompt: {}", prompt);
        for (InputMethod inputMethod : inputMethods) {
            logger.info("{} {}", inputMethod.getClass().getSimpleName(), inputMethod);
        }
        return prompt;
    }

    private Optional<SpeechRecognitionRejectedScript> speechRecognitioneRejectedScript(ScriptFunction scriptFunction) {
        return actor.speechRecognitionRejectedScript != null
                ? Optional.of(new SpeechRecognitionRejectedScriptAdapter(this, scriptFunction))
                : Optional.empty();
    }

    public Replay getReplay() {
        return scriptRenderer.getReplay();
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

    public List<String> expandTextVariables(List<String> strings) {
        return allTextVariables().expand(strings);
    }

    /**
     * Expand text variables. Expands all text variables({@link teaselib.util.TextVariables#Defaults},
     * {@link teaselib.core.TeaseLib#getTextVariables}, {@link teaselib.Actor#textVariables}.
     */
    public String expandTextVariables(String string) {
        return allTextVariables().expand(string);
    }

    private TextVariables allTextVariables() {
        return new TextVariables(teaseLib.getTextVariables(TeaseLib.DefaultDomain, actor.locale()),
                actor.textVariables);
    }

    ExecutorService getScriptFuntionExecutorService() {
        return scriptRenderer.getScriptFunctionExecutorService();
    }
}
