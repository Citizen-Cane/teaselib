package teaselib.core;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
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
import teaselib.core.ai.perception.HumanPose;
import teaselib.core.ai.perception.HumanPose.Interest;
import teaselib.core.ai.perception.HumanPose.Proximity;
import teaselib.core.ai.perception.HumanPoseDeviceInteraction;
import teaselib.core.ai.perception.HumanPoseScriptInteraction;
import teaselib.core.ai.perception.PoseAspects;
import teaselib.core.ai.perception.PoseEstimationEventArgs;
import teaselib.core.configuration.Configuration;
import teaselib.core.devices.release.KeyReleaseDeviceInteraction;
import teaselib.core.devices.remote.LocalNetworkDevice;
import teaselib.core.media.RenderedMessage.Decorator;
import teaselib.core.media.ScriptMessageDecorator;
import teaselib.core.speechrecognition.Confidence;
import teaselib.core.speechrecognition.SpeechRecognitionInputMethod;
import teaselib.core.speechrecognition.SpeechRecognitionInputMethodEventArgs;
import teaselib.core.speechrecognition.events.SpeechRecognizedEventArgs;
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

            if (teaseLib.globals.has(TeaseLibAI.class)) {
                HumanPoseScriptInteraction humanPoseInteraction = interaction(HumanPoseScriptInteraction.class);
                if (humanPoseInteraction != null) {
                    var speechRecognitionInputMethod = inputMethods.get(SpeechRecognitionInputMethod.class);
                    speechRecognitionInputMethod.events.recognitionStarted.add(ev -> humanPoseInteraction
                            .setPause(speechRecognitionInputMethod::completeSpeechRecognition));
                    // TODO Generalize - HeadGestures -> Perception
                    if (Boolean.parseBoolean(config.get(Config.InputMethod.HeadGestures))) {
                        inputMethods.add(new HeadGesturesV2InputMethod( //
                                interaction(HumanPoseScriptInteraction.class),
                                scriptRenderer.getInputMethodExecutorService()), () -> !canSpeak.getAsBoolean());
                    }
                }

            }
        }

        // TODO initializing in actorChanged-event breaks device handling
        deviceInteraction(KeyReleaseDeviceInteraction.class).setDefaults(actor);
    }

    // TODO move speech recognition-related part to sr input method, find out how to deal with absent camera

    protected void define(HumanPoseScriptInteraction humanPoseInteraction) {
        humanPoseInteraction.deviceInteraction.addEventListener(actor, proximitySensor);
    }

    protected void undefine(HumanPoseScriptInteraction humanPoseInteraction) {
        humanPoseInteraction.deviceInteraction.removeEventListener(actor, proximitySensor);
        teaseLib.host.setActorProximity(Proximity.FAR);
    }

    private final HumanPoseDeviceInteraction.EventListener proximitySensor = new ProximitySensor(
            Interest.asSet(Interest.Status, Interest.Proximity));

    private final class ProximitySensor extends HumanPoseDeviceInteraction.EventListener {
        private Proximity previous = Proximity.FACE2FACE;

        private ProximitySensor(Set<Interest> interests) {
            super(interests);
        }

        @Override
        public void run(PoseEstimationEventArgs eventArgs) throws Exception {
            var presence = presence(eventArgs);
            var proximity = proximity(eventArgs);
            boolean speechProximity = presence && proximity == Proximity.FACE2FACE;

            logger.info("User Presence: {}", presence);
            logger.info("User Proximity: {}", proximity);
            teaseLib.host.setActorProximity(proximity);
            teaseLib.globals.get(Shower.class).updateUI(new InputMethod.UiEvent(speechProximity));
            teaseLib.host.show();
        }

        private boolean presence(PoseEstimationEventArgs eventArgs) {
            Optional<HumanPose.Status> aspect = eventArgs.pose.aspect(HumanPose.Status.class);
            HumanPose.Status presence;
            if (aspect.isPresent()) {
                presence = aspect.get();
            } else {
                presence = HumanPose.Status.Available;
            }
            return presence != HumanPose.Status.None;
        }

        private Proximity proximity(PoseEstimationEventArgs eventArgs) {
            Optional<Proximity> aspect = eventArgs.pose.aspect(Proximity.class);
            Proximity proximity;
            if (aspect.isPresent()) {
                proximity = aspect.get();
                previous = proximity;
            } else {
                if (previous == Proximity.CLOSE || previous == Proximity.FACE2FACE) {
                    proximity = Proximity.NEAR;
                } else {
                    proximity = previous;
                }
            }
            return proximity;
        }
    }

    private DeviceInteractionImplementations initDeviceInteractions() {
        var deviceInteractionImplementations = new DeviceInteractionImplementations();
        deviceInteractionImplementations.add(KeyReleaseDeviceInteraction.class,
                () -> new KeyReleaseDeviceInteraction(teaseLib, scriptRenderer));
        if (teaseLib.globals.has(TeaseLibAI.class)) {
            deviceInteractionImplementations.add(HumanPoseDeviceInteraction.class,
                    () -> new HumanPoseDeviceInteraction(teaseLib.globals.get(TeaseLibAI.class), scriptRenderer));
        }
        return deviceInteractionImplementations;
    }

    private static final float UNTIL_REMOVE_LIMIT = 1.5f;
    private static final float UNTIL_EXPIRED_LIMIT = 1.0f;

    protected void handleAutoRemove() {
        long startupTimeSeconds = teaseLib.getTime(TimeUnit.SECONDS);
        var persistedDomains = teaseLib.state(TeaseLib.DefaultDomain, StateImpl.Internal.PERSISTED_DOMAINS_STATE);
        Collection<Object> domains = new ArrayList<>(((StateImpl) persistedDomains).peers());
        for (Object domain : domains) {
            if (domain.equals(StateImpl.Domain.LAST_USED)) {
                continue;
            } else
                domain = domain.equals(StateImpl.Internal.DEFAULT_DOMAIN_NAME) ? TeaseLib.DefaultDomain : domain;
            if (!handleUntilRemoved(domain.toString(), startupTimeSeconds).applied()
                    && !handleUntilExpired(domain.toString(), startupTimeSeconds).applied()) {
                persistedDomains.removeFrom(domain);
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
        var untilState = teaseLib.state(domain, until);
        Set<Object> peers = ((StateImpl) untilState).peers();
        for (Object peer : new ArrayList<>(peers)) {
            var state = teaseLib.state(domain, peer);
            if (!cleanupRemovedUserItemReferences(state)) {
                remove(state, startupTimeSeconds, limitFactor);
            }
        }
        return untilState;
    }

    private boolean cleanupRemovedUserItemReferences(State state) {
        var stateImpl = AbstractProxy.stateImpl(state);
        if (stateImpl.peers().stream().filter(Predicate.not(Item.class::isInstance)).map(peer -> {
            if (peer instanceof QualifiedString) {
                return (QualifiedString) peer;
            } else {
                return QualifiedString.of(peer);
            }
        }).filter(peer -> peer.guid().isPresent()).anyMatch(
                peer -> teaseLib.findItem(stateImpl.domain, stateImpl.item, peer.guid().get()) == Item.NotFound)) {
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
            completeSection();
        } else {
            completeSectionBeforeStarting(scriptFunction);
        }

        if (scriptFunction == null || scriptFunction.relation != ScriptFunction.Relation.Autonomous) {
            scriptRenderer.stopBackgroundRenderers();
        }

        Optional<SpeechRecognitionRejectedScript> speechRecognitionRejectedScript = speechRecognitioneRejectedScript(
                scriptFunction);
        if (speechRecognitionRejectedScript.isPresent()) {
            addRecognitionRejectedAction(prompt, speechRecognitionRejectedScript.get());
        }

        scriptRenderer.events.beforeChoices.fire(new ScriptEventArgs());
        var answer = anwser(prompt);
        endAll();
        teaseLib.host.endScene();
        scriptRenderer.events.afterChoices.fire(new ScriptEventArgs());
        return answer;
    }

    private void completeSection() {
        // If we don't have a script function,
        // then the mandatory part of the renderers
        // must be completed before displaying the ui choices
        if (scriptRenderer.isInterTitle()) {
            completeMandatory();
        } else {
            showAll(5.0);
        }
    }

    private void completeSectionBeforeStarting(ScriptFunction scriptFunction) {
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

    private Answer anwser(Prompt prompt) {
        Choice choice;
        if (teaseLib.globals.has(TeaseLibAI.class)) {
            HumanPoseScriptInteraction humanPoseInteraction = interaction(HumanPoseScriptInteraction.class);
            if (humanPoseInteraction != null) {
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
        if (teaseLib.globals.has(TeaseLibAI.class)) {
            return () -> new InputMethod.UiEvent(isFace2Face());
        } else {
            return Prompt.AlwaysEnabled;
        }
    }

    boolean isFace2Face() {
        HumanPoseScriptInteraction humanPoseInteraction = interaction(HumanPoseScriptInteraction.class);
        if (humanPoseInteraction != null) {
            PoseAspects pose = humanPoseInteraction.getPose(Interest.Proximity);
            return pose.is(Proximity.FACE2FACE);
        } else {
            return true;
        }
    }

    private Optional<SpeechRecognitionRejectedScript> speechRecognitioneRejectedScript(ScriptFunction scriptFunction) {
        return actor.speechRecognitionRejectedScript != null
                ? Optional.of(new SpeechRecognitionRejectedScriptAdapter(this, scriptFunction))
                : Optional.empty();
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
