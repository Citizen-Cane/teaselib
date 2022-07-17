package teaselib.core;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
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
import teaselib.Mood;
import teaselib.Replay;
import teaselib.Resources;
import teaselib.ScriptFunction;
import teaselib.State;
import teaselib.TeaseScript;
import teaselib.core.ai.TeaseLibAI;
import teaselib.core.ai.perception.HeadGesturesV2InputMethod;
import teaselib.core.ai.perception.HumanPose.Proximity;
import teaselib.core.ai.perception.HumanPoseDeviceInteraction;
import teaselib.core.ai.perception.HumanPoseScriptInteraction;
import teaselib.core.ai.perception.PoseAspects;
import teaselib.core.ai.perception.ProximitySensor;
import teaselib.core.configuration.Configuration;
import teaselib.core.devices.release.KeyReleaseDeviceInteraction;
import teaselib.core.devices.remote.LocalNetworkDevice;
import teaselib.core.media.RenderedMessage.Decorator;
import teaselib.core.media.ScriptMessageDecorator;
import teaselib.core.speechrecognition.Confidence;
import teaselib.core.speechrecognition.SpeechRecognitionInputMethod;
import teaselib.core.state.AbstractProxy;
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

        boolean callOnce = teaseLib.globals.get(ScriptCache.class) == null;
        if (callOnce) {
            handleAutoRemove();
            bindNetworkProperties();

            var inputMethods = teaseLib.globals.get(InputMethods.class);
            Function<Choices, Boolean> canSpeak = choices -> !teaseLib.state(TeaseLib.DefaultDomain, Body.InMouth)
                    .applied();
            inputMethods.add(new SpeechRecognitionInputMethod(config, scriptRenderer.audioSync), canSpeak);
            inputMethods.add(teaseLib.host.inputMethod());
            inputMethods.add(scriptRenderer.scriptEventInputMethod);

            if (teaseLib.globals.has(TeaseLibAI.class)
                    && Boolean.parseBoolean(config.get(Config.InputMethod.HeadGestures))) {
                HumanPoseScriptInteraction humanPoseInteraction = interaction(HumanPoseScriptInteraction.class);
                if (humanPoseInteraction != null) {
                    var speechRecognitionInputMethod = inputMethods.get(SpeechRecognitionInputMethod.class);
                    speechRecognitionInputMethod.events.recognitionStarted.add(ev -> humanPoseInteraction
                            .setPause(speechRecognitionInputMethod::completeSpeechRecognition));
                    speechRecognitionInputMethod.events.recognitionRejected
                            .add(ev -> humanPoseInteraction.clearPause());
                    speechRecognitionInputMethod.events.recognitionCompleted
                            .add(ev -> humanPoseInteraction.clearPause());

                    if (Boolean.parseBoolean(config.get(Config.InputMethod.HeadGestures))) {
                        inputMethods.add(new HeadGesturesV2InputMethod( //
                                humanPoseInteraction, scriptRenderer.getInputMethodExecutorService()),
                                choices -> !canSpeak.apply(choices)
                                        && HeadGesturesV2InputMethod.distinctGestures(choices));
                    }
                }
            }
        }

        // TODO initializing in actorChanged-event breaks device handling
        deviceInteraction(KeyReleaseDeviceInteraction.class).setDefaults(actor);
    }

    protected void startProximitySensor(HumanPoseScriptInteraction humanPoseInteraction) {
        humanPoseInteraction.deviceInteraction.addEventListener(actor,
                humanPoseInteraction.deviceInteraction.proximitySensor);
    }

    protected void stopProimitySensor(HumanPoseScriptInteraction humanPoseInteraction) {
        humanPoseInteraction.deviceInteraction.removeEventListener(actor,
                humanPoseInteraction.deviceInteraction.proximitySensor);
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

    private static final long UNTIL_REMOVE_LIMIT_MINIMUM_SECONDS = TimeUnit.SECONDS.convert(2, TimeUnit.DAYS);
    private static final float UNTIL_REMOVE_LIMIT_FACTOR = 1.5f;
    private static final long UNTIL_EXPIRED_LIMIT_MINIMUM_SECONDS = TimeUnit.SECONDS.convert(0, TimeUnit.DAYS);
    private static final float UNTIL_EXPIRED_LIMIT_FACTOR = 1.0f;

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
                boolean allRemoved = !handleUntilRemoved(domain, startupTimeSeconds).applied();
                boolean allExpired = !handleUntilExpired(domain, startupTimeSeconds).applied();
                if (allRemoved && allExpired) {
                    persistedDomains.removeFrom(domain);
                }
            }
        }
    }

    private State handleUntilRemoved(String domain, long startupTimeSeconds) {
        return handle(domain, State.Persistence.Until.Removed, startupTimeSeconds, UNTIL_REMOVE_LIMIT_MINIMUM_SECONDS,
                UNTIL_REMOVE_LIMIT_FACTOR);
    }

    private State handleUntilExpired(String domain, long startupTimeSeconds) {
        return handle(domain, State.Persistence.Until.Expired, startupTimeSeconds, UNTIL_EXPIRED_LIMIT_MINIMUM_SECONDS,
                UNTIL_EXPIRED_LIMIT_FACTOR);
    }

    private State handle(String domain, State.Persistence.Until until, long startupTimeSeconds, long minimumDuration,
            float expirationFactor) {
        var untilState = (StateImpl) teaseLib.state(domain, until);
        Set<QualifiedString> peers = untilState.peers();
        for (QualifiedString peer : new ArrayList<>(peers)) {
            if (peer.isItem()) {
                Item candidate = teaseLib.item(domain, peer);
                if (candidate == Item.NotFound) {
                    continue;
                } else {
                    var item = AbstractProxy.removeProxy(candidate);
                    if (allUserItemReferencesRemoved(item)) {
                        remove(item);
                    } else if (autoRemovalLimitReached(item, startupTimeSeconds, minimumDuration, expirationFactor)) {
                        remove(item);
                    }
                }
            } else {
                var state = (StateImpl) teaseLib.state(domain, peer);
                if (allUserItemReferencesRemoved(state)) {
                    remove(state);
                } else if (autoRemovalLimitReached(state, startupTimeSeconds, minimumDuration, expirationFactor)) {
                    remove(state);
                }
            }
        }
        return untilState;
    }

    private void remove(ItemImpl item) {
        item.remove(elapsedDuration(item));
    }

    private void remove(StateImpl state) {
        state.remove(elapsedDuration(state));
    }

    private FrozenDuration elapsedDuration(State state) {
        Duration duration = state.duration();
        long start = duration.start(TeaseLib.DURATION_TIME_UNIT);
        long limit = duration.limit(TeaseLib.DURATION_TIME_UNIT);
        long elapsed = limit;
        return new FrozenDuration(teaseLib, start, limit, elapsed, TeaseLib.DURATION_TIME_UNIT);
    }

    private boolean allUserItemReferencesRemoved(ItemImpl item) {
        return allUserItemReferencesRemoved(item.state());
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

    private static boolean autoRemovalLimitReached(State state, long startupTimeSeconds,
            long minimumDuration, float limitFactor) {
        var duration = state.duration();
        // Very implicit way of testing for unavailable items
        if (state.applied() && duration.expired()) {
            long limit = duration.limit(TimeUnit.SECONDS);
            if (limit >= State.TEMPORARY && limit < Duration.INFINITE) {
                long expirationDuration = Math.max(minimumDuration, (long) (limit * limitFactor));
                long autoRemovalTime = duration.start(TimeUnit.SECONDS) + expirationDuration;
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

    protected void startMessage(Message message, boolean useTTS) {
        Optional<TextToSpeechPlayer> textToSpeech = getTextToSpeech(useTTS);
        try {
            scriptRenderer.startMessage(teaseLib, resources, message, decorators(textToSpeech));
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
            awaitMandatoryCompleted();
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
                    choice = getDistinctChoice(prompt);
                } finally {
                    stopProimitySensor(humanPoseInteraction);
                }
            } else {
                teaseLib.host.setActorZoom(ProximitySensor.zoom.get(Proximity.FACE2FACE));
                choice = getDistinctChoice(prompt);
                teaseLib.host.setActorZoom(1.0);
            }
        } else {
            teaseLib.host.setActorZoom(ProximitySensor.zoom.get(Proximity.FACE2FACE));
            choice = getDistinctChoice(prompt);
            teaseLib.host.setActorZoom(1.0);
        }

        String answer = "< " + choice.display;
        logger.info("{}", answer);
        teaseLib.transcript.info(answer);

        return choice.answer;
    }

    private Choice getDistinctChoice(Prompt prompt) throws InterruptedException {
        return showPrompt(prompt).get(0);
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
        var prompt = new Prompt(this, choices, inputMethods, scriptFunction, Prompt.Result.Accept.Distinct,
                initialUiEvent());
        logger.info("Prompt: {}", prompt);
        for (InputMethod inputMethod : inputMethods) {
            logger.info("{} {}", inputMethod.getClass().getSimpleName(), inputMethod);
        }
        return prompt;
    }

    protected Supplier<UiEvent> initialUiEvent() {
        // TODO also depends on camera input
        // - possibly wrong state after camera surprise-removal
        // - camera is only recognized on the next prompt - but UI would be always active which is good
        if (Boolean.parseBoolean(teaseLib.config.get(Config.InputMethod.HeadGestures))) {
            return () -> {
                HumanPoseScriptInteraction humanPoseInteraction = interaction(HumanPoseScriptInteraction.class);
                if (humanPoseInteraction != null && humanPoseInteraction.deviceInteraction.isActive()) {
                    var proximitySensor = humanPoseInteraction.deviceInteraction.proximitySensor;
                    if (!humanPoseInteraction.deviceInteraction.containsEventListener(actor, proximitySensor)) {
                        // Starting the sensor includes fires event with all relevant updates for the new listener
                        startProximitySensor(humanPoseInteraction);
                        // -> current pose available
                    }
                    return new InputMethod.UiEvent(face2face(proximitySensor.pose()));
                } else {
                    return new InputMethod.UiEvent(true);
                }
            };
        } else {
            return Prompt.AlwaysEnabled;
        }
    }

    private static boolean face2face(PoseAspects pose) {
        return pose.is(Proximity.FACE2FACE);
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
     * Fetch resources with pre-defined regex-patterns
     * 
     * @param folder
     * @param wildcardPattern
     * @return
     */
    public Resources resources(String folder, String regex) {
        String path = folder + (folder.endsWith("/") ? "" : "/") + regex;
        return resources(path);
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
        return new Resources(teaseLib, resources, scriptRenderer.getPrefetchExecutorService(), wildcardPattern,
                paths.elements, paths.mapping);
    }

}
