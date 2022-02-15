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
import teaselib.ActorImages.Next;
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

    private static final Logger logger = LoggerFactory.getLogger(Script.class);

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
            throw ExceptionUtil.asRuntimeException(e);
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
            deviceInteractionImplementations.add(HumanPoseDeviceInteraction.class, () -> {
                try {
                    return new HumanPoseDeviceInteraction(teaseLib, teaseLib.globals.get(TeaseLibAI.class),
                            scriptRenderer);
                } catch (InterruptedException e) {
                    throw new ScriptInterruptedException(e);
                }
            });
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
                if (allUserItemReferencesRemoved(state)) {
                    state.remove();
                } else if (autoRemovalLimitReached(state, startupTimeSeconds, limitFactor)) {
                    state.remove();
                }
            }
        }
        return untilState;
    }

    private boolean allUserItemReferencesRemoved(StateImpl state) {
        List<QualifiedString> peers = state.peers().stream().filter(QualifiedString::isItem).toList();
        if (peers.isEmpty()) {
            return false;
        } else {
            List<Item> items = peers.stream().map(peer -> {
                return teaseLib.findItem(state.domain, peer.kind(), peer.guid().get());
            }).filter(Predicate.not(Item.NotFound::equals)).toList();
            return items.isEmpty();
        }

    }

    private static boolean autoRemovalLimitReached(State state, long startupTimeSeconds, float limitFactor) {
        // Very implicit way of testing for unavailable items
        var duration = state.duration();
        if (state.applied() && duration.expired()) {
            long limit = duration.limit(TimeUnit.SECONDS);
            if (limit >= State.TEMPORARY && limit < Duration.INFINITE) {
                long autoRemovalTime = duration.start(TimeUnit.SECONDS) + (long) (limit * limitFactor);
                if (autoRemovalTime <= startupTimeSeconds) {
                    return true;
                }
            }
        }
        return false;
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
        return teaseLib.deviceInteraction(deviceInteraction);
    }

    public void awaitStartCompleted() {
        try {
            scriptRenderer.awaitStartCompleted();
        } catch (InterruptedException e) {
            throw new ScriptInterruptedException(e);
        }
    }

    public void awaitMandatoryCompleted() {
        try {
            scriptRenderer.awaitMandatoryCompleted();
        } catch (InterruptedException e) {
            throw new ScriptInterruptedException(e);
        }
    }

    /**
     * Just wait for everything to be rendered (messages displayed, sounds played, delay expired), and continue
     * execution of the script.
     * <p>
     * This won't display a button, it just waits. Background threads will continue to run.
     */
    public void awaitAllCompleted() {
        try {
            scriptRenderer.awaitAllCompleted();
        } catch (InterruptedException e) {
            throw new ScriptInterruptedException(e);
        }
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
        } catch (InterruptedException e) {
            throw new ScriptInterruptedException(e);
        } finally {
            displayImage = Message.ActorImage;
            mood = Mood.Neutral;
        }
        teaseLib.config.flushSettings();
    }

    protected void prependMessage(Message message) {
        scriptRenderer.prependMessage(message);
    }

    protected void renderMessage(Message message, boolean useTTS) {
        actor.images.advance(Next.Message);
        Optional<TextToSpeechPlayer> textToSpeech = getTextToSpeech(useTTS);
        try {
            scriptRenderer.renderMessage(teaseLib, resources, message, decorators(textToSpeech));
        } catch (InterruptedException e) {
            throw new ScriptInterruptedException(e);
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
        try {
            scriptRenderer.appendMessage(teaseLib, resources, actor, message, decorators(getTextToSpeech()));
        } catch (InterruptedException e) {
            throw new ScriptInterruptedException(e);
        }
    }

    protected void replaceMessage(Message message) {
        try {
            scriptRenderer.replaceMessage(teaseLib, resources, actor, message, decorators(getTextToSpeech()));
        } catch (InterruptedException e) {
            throw new ScriptInterruptedException(e);
        }
    }

    void showAll(double delaySeconds) {
        var showAll = new Message(actor);
        addMatchingImage(showAll);
        if (scriptRenderer.showsMultipleParagraphs()) {
            showAll.add(Type.Keyword, Message.ShowChoices);
            showAll.add(Type.Delay, delaySeconds);
            addMatchingImage(showAll);
        }
        try {
            scriptRenderer.showAll(teaseLib, resources, actor, showAll, withoutSpeech());
        } catch (InterruptedException e) {
            throw new ScriptInterruptedException(e);
        }
    }

    private void addMatchingImage(Message message) {
        if (scriptRenderer.showsActorImage()) {
            message.add(Type.Image, displayImage);
        } else if (!scriptRenderer.showsInstructionalImage()) {
            message.add(Type.Image, Message.NoImage);
        }
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
            try {
                scriptRenderer.renderPrependedMessages(teaseLib, resources, actor, decorators(textToSpeech));
            } catch (InterruptedException e) {
                throw new ScriptInterruptedException(e);
            }
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
        Answer answer;
        try {
            answer = anwser(prompt);
        } catch (InterruptedException e) {
            throw new ScriptInterruptedException(e);
        } finally {
            endAll();
            teaseLib.host.endScene();
        }
        scriptRenderer.events.afterPrompt.fire(new ScriptEventArgs());
        actor.images.advance(Next.Section);
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

    private Answer anwser(Prompt prompt) throws InterruptedException {
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

    private static String selectPhrase(Answer answer) {
        // TODO instead of returning a fixed or completely random value,
        // - select one of the main phrases -> those that support the story line
        // - use all other phrases as alternatives to speak, but not displayed
        return answer.text.get(0);
    }

    private List<Choice> showPrompt(Prompt prompt) throws InterruptedException {
        return teaseLib.globals.get(Shower.class).show(prompt);
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
        ResourceLoader.Paths paths;
        int size;
        Class<?> scriptClass = getClass();
        do {
            paths = resources.resources(wildcardPattern, scriptClass);
            size = paths.elements.size();
            if (size > 0) {
                logger.info("{}: '{}' yields {} resources", scriptClass.getSimpleName(), wildcardPattern, size);
            } else {
                logger.info("{}: '{}' doesn't yield any resources", scriptClass.getSimpleName(), wildcardPattern);
                scriptClass = scriptClass.getSuperclass();
            }
        } while (size == 0 && scriptClass != TeaseScript.class);
        ExceptionUtil.handleAssetNotFound(wildcardPattern, paths.elements, teaseLib.config, logger);
        return new Resources(teaseLib, resources, scriptRenderer.getPrefetchExecutorService(), paths.elements,
                paths.mapping);
    }

}
