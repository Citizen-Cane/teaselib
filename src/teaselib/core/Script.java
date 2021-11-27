package teaselib.core;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.BooleanSupplier;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import teaselib.Actor;
import teaselib.Answer;
import teaselib.Body;
import teaselib.Config;
import teaselib.Duration;
import teaselib.Message;
import teaselib.Message.Type;
import teaselib.Mood;
import teaselib.Replay;
import teaselib.Resources;
import teaselib.ScriptFunction;
import teaselib.State;
import teaselib.TeaseScript;
import teaselib.core.ai.TeaseLibAI;
import teaselib.core.ai.perception.HeadGesturesV2InputMethod;
import teaselib.core.ai.perception.HumanPose.Interest;
import teaselib.core.ai.perception.HumanPose.Proximity;
import teaselib.core.ai.perception.HumanPoseDeviceInteraction;
import teaselib.core.ai.perception.HumanPoseScriptInteraction;
import teaselib.core.ai.perception.PoseAspects;
import teaselib.core.configuration.Configuration;
import teaselib.core.devices.release.KeyReleaseDeviceInteraction;
import teaselib.core.devices.remote.LocalNetworkDevice;
import teaselib.core.media.RenderedMessage.Decorator;
import teaselib.core.media.ScriptMessageDecorator;
import teaselib.core.speechrecognition.Confidence;
import teaselib.core.speechrecognition.SpeechRecognitionInputMethod;
import teaselib.core.texttospeech.TextToSpeechPlayer;
import teaselib.core.ui.Choice;
import teaselib.core.ui.Choices;
import teaselib.core.ui.InputMethod;
import teaselib.core.ui.InputMethod.UiEvent;
import teaselib.core.ui.InputMethodEventArgs;
import teaselib.core.ui.InputMethods;
import teaselib.core.ui.Intention;
import teaselib.core.ui.Prompt;
import teaselib.core.ui.Prompt.Action;
import teaselib.core.ui.Shower;
import teaselib.core.util.ExceptionUtil;
import teaselib.core.util.QualifiedString;
import teaselib.core.util.WildcardPattern;
import teaselib.util.Item;
import teaselib.util.SpeechRecognitionRejectedScript;
import teaselib.util.TextVariables;
import teaselib.util.math.Random;

public abstract class Script {

    static final Logger logger = LoggerFactory.getLogger(Script.class);

    public final TeaseLib teaseLib;
    public final ResourceLoader resources;

    public final Actor actor;
    public final String namespace;
    public final Random random;

    public final ScriptRenderer scriptRenderer;

    protected String mood = Mood.Neutral;
    protected String displayImage = Message.ActorImage;

    /**
     * Construct a new main-script instance
     * 
     * @param teaseLib
     * @param locale
     */
    protected Script(TeaseLib teaseLib, ResourceLoader resources, Actor actor, String namespace) {
        this(teaseLib, resources, actor, namespace, //
                getOrDefault(teaseLib, ScriptRenderer.class, () -> new ScriptRenderer(teaseLib)));

        getOrDefault(teaseLib, Shower.class, Shower::new);
        getOrDefault(teaseLib, InputMethods.class, InputMethods::new);
        Configuration config = teaseLib.config;
        if (Boolean.parseBoolean(config.get(Config.InputMethod.HeadGestures))
                || Boolean.parseBoolean(config.get(Config.InputMethod.SpeechRecognition))
                || Boolean.parseBoolean(config.get(Config.Render.ActorImages))) {
            getOrDefault(teaseLib, TeaseLibAI.class, TeaseLibAI::new);
        }
        getOrDefault(teaseLib, DeviceInteractionImplementations.class, this::initDeviceInteractions);

        try {
            config.addScriptSettings(this.namespace);
        } catch (IOException e) {
            ExceptionUtil.handleException(e, config, logger);
        }

        boolean startOnce = teaseLib.globals.get(ScriptCache.class) == null;
        if (startOnce) {
            handleAutoRemove();
            bindNetworkProperties();

            var inputMethods = teaseLib.globals.get(InputMethods.class);
            BooleanSupplier canSpeak = () -> !teaseLib.state(TeaseLib.DefaultDomain, Body.InMouth).applied();
            inputMethods.add(new SpeechRecognitionInputMethod(config, scriptRenderer.audioSync), canSpeak);
            inputMethods.add(teaseLib.host.inputMethod());
            inputMethods.add(scriptRenderer.scriptEventInputMethod);

            if (teaseLib.globals.has(TeaseLibAI.class)
                    && Boolean.parseBoolean(config.get(Config.InputMethod.HeadGestures))) {
                // TODO humanPoseInteraction should be zero
                HumanPoseScriptInteraction humanPoseInteraction = interaction(HumanPoseScriptInteraction.class);
                if (humanPoseInteraction != null) {
                    var speechRecognitionInputMethod = inputMethods.get(SpeechRecognitionInputMethod.class);
                    speechRecognitionInputMethod.events.recognitionStarted.add(ev -> humanPoseInteraction
                            .setPause(speechRecognitionInputMethod::completeSpeechRecognition));
                    // TODO Generalize - HeadGestures -> Perception
                    if (Boolean.parseBoolean(config.get(Config.InputMethod.HeadGestures))) {
                        inputMethods.add(new HeadGesturesV2InputMethod( //
                                humanPoseInteraction, scriptRenderer.getInputMethodExecutorService()),
                                () -> !canSpeak.getAsBoolean());
                    }
                }

            }
        }

        // TODO initializing in actorChanged-event breaks device handling
        deviceInteraction(KeyReleaseDeviceInteraction.class).setDefaults(actor);
    }

    // TODO move speech recognition-related part to sr input method, find out how to deal with absent camera

    protected void define(HumanPoseScriptInteraction humanPoseInteraction) {
        humanPoseInteraction.deviceInteraction.addEventListener(actor,
                humanPoseInteraction.deviceInteraction.proximitySensor);
    }

    protected void undefine(HumanPoseScriptInteraction humanPoseInteraction) {
        humanPoseInteraction.deviceInteraction.removeEventListener(actor,
                humanPoseInteraction.deviceInteraction.proximitySensor);
        teaseLib.host.setActorProximity(Proximity.FAR);
    }

    private DeviceInteractionImplementations initDeviceInteractions() {
        var deviceInteractionImplementations = new DeviceInteractionImplementations();
        deviceInteractionImplementations.add(KeyReleaseDeviceInteraction.class,
                () -> new KeyReleaseDeviceInteraction(teaseLib, scriptRenderer));
        if (Boolean.parseBoolean(teaseLib.config.get(Config.InputMethod.HeadGestures))) {
            deviceInteractionImplementations.add(HumanPoseDeviceInteraction.class,
                    () -> new HumanPoseDeviceInteraction(teaseLib, teaseLib.globals.get(TeaseLibAI.class),
                            scriptRenderer));
        }
        return deviceInteractionImplementations;
    }

    private static final float UNTIL_REMOVE_LIMIT = 1.5f;
    private static final float UNTIL_EXPIRED_LIMIT = 1.0f;

    protected void handleAutoRemove() {
        long startupTimeSeconds = teaseLib.getTime(TimeUnit.SECONDS);
        var persistedDomains = teaseLib.state(TeaseLib.DefaultDomain, StateImpl.Internal.PERSISTED_DOMAINS_STATE);
        List<String> domains = ((StateImpl) persistedDomains).peers().stream().map(QualifiedString::toString).toList();
        for (String domain : domains) {
            if (domain.equalsIgnoreCase(StateImpl.Domain.LAST_USED)) {
                continue;
            } else {
                domain = domain.equalsIgnoreCase(StateImpl.Internal.DEFAULT_DOMAIN_NAME) ? TeaseLib.DefaultDomain
                        : domain;
                if (!handleUntilRemoved(domain, startupTimeSeconds).applied()
                        && !handleUntilExpired(domain, startupTimeSeconds).applied()) {
                    persistedDomains.removeFrom(domain);
                }
            }
        }
    }

    private State handleUntilRemoved(String domain, long startupTimeSeconds) {
        return handle(domain, State.Persistence.Until.Removed, startupTimeSeconds, UNTIL_REMOVE_LIMIT);
    }

    private State handleUntilExpired(String domain, long startupTimeSeconds) {
        return handle(domain, State.Persistence.Until.Expired, startupTimeSeconds, UNTIL_EXPIRED_LIMIT);
    }

    private State handle(String domain, State.Persistence.Until until, long startupTimeSeconds, float limitFactor) {
        var untilState = (StateImpl) teaseLib.state(domain, until);
        Set<QualifiedString> peers = untilState.peers();
        for (QualifiedString peer : new ArrayList<>(peers)) {
            if (!peer.isItem()) {
                var state = (StateImpl) teaseLib.state(domain, peer);
                if (!cleanupRemovedUserItemReferences(state)) {
                    remove(state, startupTimeSeconds, limitFactor);
                }
            }
        }
        return untilState;
    }

    private boolean cleanupRemovedUserItemReferences(StateImpl state) {
        List<QualifiedString> peers = state.peers().stream().filter(peer -> peer.guid().isPresent()).toList();

        List<Item> items = peers.stream().map(peer -> {
            return teaseLib.findItem(state.domain, peer.kind(), peer.guid().get());
        }).filter(Predicate.not(Item.NotFound::equals)).toList();

        if (items.isEmpty()) {
            state.remove();
            return true;
        } else {
            return false;
        }
    }

    private static void remove(State state, long startupTimeSeconds, float limitFactor) {
        // Very implicit way of testing for unavailable items
        var duration = state.duration();
        if (state.applied() && duration.expired()) {
            long limit = duration.limit(TimeUnit.SECONDS);
            if (limit >= State.TEMPORARY && limit < Duration.INFINITE) {
                long autoRemovalTime = duration.start(TimeUnit.SECONDS) + (long) (limit * limitFactor);
                if (autoRemovalTime <= startupTimeSeconds) {
                    state.remove();
                }
            }
        }
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
        this.random = teaseLib.random;

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

    public void awaitStartCompleted() {
        scriptRenderer.awaitStartCompleted();
    }

    public void awaitMandatoryCompleted() {
        scriptRenderer.awaitMandatoryCompleted();
    }

    /**
     * Just wait for everything to be rendered (messages displayed, sounds played, delay expired), and continue
     * execution of the script.
     * <p>
     * This won't display a button, it just waits. Background threads will continue to run.
     */
    public void awaitAllCompleted() {
        scriptRenderer.awaitAllCompleted();
    }

    /**
     * Stop rendering and end all render threads
     */
    public void endAll() {
        scriptRenderer.endAll();
    }

    protected void renderIntertitle(String... text) {
        try {
            scriptRenderer.renderIntertitle(teaseLib, new Message(actor, text), justText());
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
        teaseLib.config.flushSettings();
    }

    private Optional<TextToSpeechPlayer> getTextToSpeech() {
        return Optional.ofNullable(scriptRenderer.sectionRenderer.textToSpeechPlayer);
    }

    private Optional<TextToSpeechPlayer> getTextToSpeech(boolean useTTS) {
        return useTTS ? getTextToSpeech() : Optional.empty();
    }

    Decorator[] decorators(Optional<TextToSpeechPlayer> textToSpeech) {
        return new ScriptMessageDecorator(teaseLib.config, displayImage, actor, mood, resources,
                this::expandTextVariables, textToSpeech).all();
    }

    Decorator[] withoutSpeech() {
        return decorators(Optional.empty());
    }

    Decorator[] justText() {
        return new ScriptMessageDecorator(teaseLib.config, displayImage, actor, mood, resources,
                this::expandTextVariables).justText();
    }

    protected void appendMessage(Message message) {
        scriptRenderer.appendMessage(teaseLib, resources, actor, message, decorators(getTextToSpeech()));
    }

    protected void replaceMessage(Message message) {
        scriptRenderer.replaceMessage(teaseLib, resources, actor, message, decorators(getTextToSpeech()));
    }

    void showAll(double delaySeconds) {
        var message = new Message(actor);
        message.add(Type.Keyword, Message.ShowChoices);
        message.add(Type.Delay, Double.toString(delaySeconds));
        if (!scriptRenderer.isShowingInstructionalImage()) {
            message.add(Type.Image, displayImage);
        }
        scriptRenderer.showAll(teaseLib, resources, actor, message, withoutSpeech());
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

        var prompt = getPrompt(answers, intention, scriptFunction);

        if (scriptFunction == null) {
            // If we don't have a script function,
            // then the mandatory part of the renderers
            // must be completed before displaying the ui choices
            completeSection();
        } else {
            completeSectionBeforeStarting(scriptFunction);
        }

        if (scriptFunction == null || scriptFunction.relation != ScriptFunction.Relation.Autonomous) {
            scriptRenderer.stopBackgroundRenderers();
        }

        if (scriptFunction == null) {
            speechRecognitioneRejectedScript().ifPresent(script -> addRecognitionRejectedAction(prompt, script));
        }

        scriptRenderer.events.beforePrompt.fire(new ScriptEventArgs());
        var answer = anwser(prompt);
        endAll();
        teaseLib.host.endScene();
        scriptRenderer.events.afterPrompt.fire(new ScriptEventArgs());
        return answer;
    }

    private void completeSection() {
        if (scriptRenderer.isInterTitle()) {
            awaitMandatoryCompleted();
        } else {
            showAll(5.0);
        }
    }

    private void completeSectionBeforeStarting(ScriptFunction scriptFunction) {
        if (scriptFunction.relation == ScriptFunction.Relation.Confirmation) {
            // A confirmation relates to the current message,
            // and must appears like a normal button,
            // so in a way it is concatenated to the current message
            awaitMandatoryCompleted();
        } else {
            // An autonomous script function does not relate to the current
            // message, therefore we'll wait until all of the last message
            // has been completed
            awaitAllCompleted();
        }
    }

    private Optional<SpeechRecognitionRejectedScript> speechRecognitioneRejectedScript() {
        return actor.speechRecognitionRejectedScript != null
                ? Optional.of(new SpeechRecognitionRejectedScriptAdapter(this))
                : Optional.empty();
    }

    private static void addRecognitionRejectedAction(Prompt prompt, SpeechRecognitionRejectedScript script) {
        prompt.when(SpeechRecognitionInputMethod.Notification.RecognitionRejected).then(new Action() {
            boolean speechRecognitionRejectedHandlerRunOnce = false;

            @Override
            public boolean canRun(InputMethodEventArgs e) {
                return !speechRecognitionRejectedHandlerRunOnce && script.canRun();
            }

            @Override
            public void run(InputMethodEventArgs e) {
                // TODO generalize this -> invoke action once for this prompt, once for whole prompt stack
                speechRecognitionRejectedHandlerRunOnce = true;
                script.run();
            }
        });
    }

    private Answer anwser(Prompt prompt) {
        Choice choice;
        if (Boolean.parseBoolean(teaseLib.config.get(Config.InputMethod.HeadGestures))) {
            HumanPoseScriptInteraction humanPoseInteraction = interaction(HumanPoseScriptInteraction.class);
            if (humanPoseInteraction != null && humanPoseInteraction.deviceInteraction.isActive()) {
                try {
                    define(humanPoseInteraction);
                    choice = showPrompt(prompt).get(0);
                } finally {
                    undefine(humanPoseInteraction);
                }
            } else {
                choice = showPrompt(prompt).get(0);
            }
        } else {
            choice = showPrompt(prompt).get(0);
        }

        String answer = "< " + choice.display;
        logger.info("{}", answer);
        teaseLib.transcript.info(answer);

        return choice.answer;
    }

    private Choices choices(List<Answer> answers, Intention intention) {
        List<Choice> choices = answers.stream().map(answer -> new Choice(answer,
                expandTextVariables(selectPhrase(answer)), expandTextVariables(answer.text)))
                .collect(Collectors.toList());

        return new Choices(actor.locale(), intention, choices);
    }

    private String selectPhrase(Answer answer) {
        // TODO Use a random function that is not used for control flow
        // TODO intead of returning a fixed or completely random value,
        // - select one of the main phrases -> those that support the story line
        // - use all other phrases as alternatives to speak, but not to display
        return answer.text.get(0);
    }

    private List<Choice> showPrompt(Prompt prompt) {
        List<Choice> choice;
        try {
            choice = teaseLib.globals.get(Shower.class).show(prompt);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ScriptInterruptedException();
        }
        return choice;
    }

    private Prompt getPrompt(List<Answer> answers, Intention intention, ScriptFunction scriptFunction) {
        var choices = choices(answers, intention);
        var inputMethods = teaseLib.globals.get(InputMethods.class);
        var prompt = new Prompt(this, choices, inputMethods, scriptFunction, Prompt.Result.Accept.Distinct, uiEvents());
        logger.info("Prompt: {}", prompt);
        for (InputMethod inputMethod : inputMethods) {
            logger.info("{} {}", inputMethod.getClass().getSimpleName(), inputMethod);
        }
        return prompt;
    }

    protected Supplier<UiEvent> uiEvents() {
        // TODO also depends on camera input - possibly wrong state after camera surprise-removal
        if (Boolean.parseBoolean(teaseLib.config.get(Config.InputMethod.HeadGestures))) {
            return () -> new InputMethod.UiEvent(isFace2Face());
        } else {
            return Prompt.AlwaysEnabled;
        }
    }

    boolean isFace2Face() {
        HumanPoseScriptInteraction humanPoseInteraction = interaction(HumanPoseScriptInteraction.class);
        if (humanPoseInteraction != null && humanPoseInteraction.deviceInteraction.isActive()) {
            PoseAspects pose = humanPoseInteraction.getPose(Interest.Proximity);
            return pose.is(Proximity.FACE2FACE);
        } else {
            return true;
        }
    }

    public Replay getReplay() {
        return scriptRenderer.getReplay();
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

    /**
     * Build a list of resource strings from a {@link WildcardPattern}. Resources are searched relative to the script
     * class. If no resources are found, the class inheritance is traversed upwards up to the first TeaseLib class. As a
     * result, all user-defined super classes can provide images as well and inherit them to sub-classes. Sub.classes
     * can "override" images provided by the base class.
     * 
     * @param wildcardPattern
     *            The wildcard pattern ("?" replaces a single, "*" multiple characters).
     * @return A list of resources that match the wildcard pattern.
     */
    public Resources resources(String wildcardPattern) {
        List<String> items;
        int size;
        Class<?> scriptClass = getClass();
        do {
            items = resources.resources(wildcardPattern, scriptClass);
            size = items.size();
            if (size > 0) {
                logger.info("{}: '{}' yields {} resources", scriptClass.getSimpleName(), wildcardPattern, size);
            } else {
                logger.info("{}: '{}' doesn't yield any resources", scriptClass.getSimpleName(), wildcardPattern);
                scriptClass = scriptClass.getSuperclass();
            }
        } while (size == 0 && scriptClass != TeaseScript.class);
        return new Resources(this, items);
    }

}
