package teaselib.core;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.BooleanSupplier;
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
import teaselib.Mood;
import teaselib.Replay;
import teaselib.Resources;
import teaselib.ScriptFunction;
import teaselib.State;
import teaselib.TeaseScript;
import teaselib.core.ScriptEventArgs.ActorChanged;
import teaselib.core.ai.TeaseLibAI;
import teaselib.core.ai.perception.HeadGesturesV2InputMethod;
import teaselib.core.ai.perception.HumanPose;
import teaselib.core.ai.perception.HumanPose.Proximity;
import teaselib.core.ai.perception.HumanPoseDeviceInteraction;
import teaselib.core.ai.perception.PoseEstimationEventArgs;
import teaselib.core.devices.release.KeyReleaseDeviceInteraction;
import teaselib.core.devices.remote.LocalNetworkDevice;
import teaselib.core.media.RenderedMessage.Decorator;
import teaselib.core.media.ScriptMessageDecorator;
import teaselib.core.speechrecognition.Confidence;
import teaselib.core.speechrecognition.SpeechRecognitionInputMethod;
import teaselib.core.speechrecognition.SpeechRecognitionInputMethodEventArgs;
import teaselib.core.speechrecognition.SpeechRecognizer;
import teaselib.core.speechrecognition.events.SpeechRecognizedEventArgs;
import teaselib.core.state.AbstractProxy;
import teaselib.core.texttospeech.TextToSpeechPlayer;
import teaselib.core.ui.Choice;
import teaselib.core.ui.Choices;
import teaselib.core.ui.InputMethod;
import teaselib.core.ui.InputMethodEventArgs;
import teaselib.core.ui.InputMethods;
import teaselib.core.ui.Intention;
import teaselib.core.ui.Prompt;
import teaselib.core.ui.Prompt.Action;
import teaselib.core.ui.Shower;
import teaselib.core.util.ExceptionUtil;
import teaselib.core.util.WildcardPattern;
import teaselib.util.Item;
import teaselib.util.ItemGuid;
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

        getOrDefault(teaseLib, Shower.class, () -> new Shower(teaseLib.host));
        getOrDefault(teaseLib, InputMethods.class, InputMethods::new);
        getOrDefault(teaseLib, DeviceInteractionImplementations.class, this::initScriptInteractions);
        getOrDefault(teaseLib, TeaseLibAI.class, TeaseLibAI::new);

        try {
            teaseLib.config.addScriptSettings(this.namespace);
        } catch (IOException e) {
            ExceptionUtil.handleException(e, teaseLib.config, logger);
        }

        boolean startOnce = teaseLib.globals.get(ScriptCache.class) == null;
        if (startOnce) {
            handleAutoRemove();
            bindNetworkProperties();

            InputMethods inputMethods = teaseLib.globals.get(InputMethods.class);
            BooleanSupplier canSpeak = () -> !teaseLib.state(TeaseLib.DefaultDomain, Body.InMouth).applied();
            SpeechRecognizer speechRecognizer = new SpeechRecognizer(teaseLib.config, scriptRenderer.audioSync);
            inputMethods.add(new SpeechRecognitionInputMethod(speechRecognizer), canSpeak);
            inputMethods.add(teaseLib.host.inputMethod());
            inputMethods.add(scriptRenderer.scriptEventInputMethod);

            HumanPoseDeviceInteraction humanPoseInteraction = deviceInteraction(HumanPoseDeviceInteraction.class);
            SpeechRecognitionInputMethod speechRecognitionInputMethod = inputMethods
                    .get(SpeechRecognitionInputMethod.class);
            speechRecognitionInputMethod.events.recognitionStarted
                    .add(ev -> humanPoseInteraction.setPause(speechRecognitionInputMethod::completeSpeechRecognition));

            // TODO Generalize - HeadGestures -> Perception
            if (Boolean.parseBoolean(teaseLib.config.get(Config.InputMethod.HeadGestures))) {
                inputMethods.add(new HeadGesturesV2InputMethod( //
                        deviceInteraction(HumanPoseDeviceInteraction.class),
                        scriptRenderer.getInputMethodExecutorService()), () -> !canSpeak.getAsBoolean());

                scriptRenderer.events.when().actorChanged().then(e -> {
                    if (!humanPoseInteraction.contains(e.actor)) {
                        define(humanPoseInteraction, e);
                    }
                });
            }

        }

        // TODO initializing in actorChanged-event breaks device handling
        deviceInteraction(KeyReleaseDeviceInteraction.class).setDefaults(actor);
    }

    // TODO move speech recognition-related part to sr input method, find out how to deal with absent camera

    protected void define(HumanPoseDeviceInteraction humanPoseInteraction, ActorChanged e) {
        SpeechRecognitionInputMethod speechRecognitionInputMethod = teaseLib.globals.get(InputMethods.class)
                .get(SpeechRecognitionInputMethod.class);

        humanPoseInteraction.addEventListener(e.actor,
                new HumanPoseDeviceInteraction.EventListener(HumanPose.Interest.Proximity) {

                    private Proximity previous = Proximity.FACE2FACE;

                    @Override
                    public void run(PoseEstimationEventArgs eventArgs) throws Exception {
                        Optional<Proximity> aspect = eventArgs.pose.aspect(Proximity.class);
                        boolean speechProximity;
                        if (aspect.isPresent()) {
                            Proximity proximity = aspect.get();
                            teaseLib.host.setUserProximity(proximity);
                            speechProximity = proximity == Proximity.FACE2FACE;
                            previous = proximity;
                        } else {
                            speechProximity = false;
                            if (previous == Proximity.CLOSE || previous == Proximity.FACE2FACE) {
                                teaseLib.host.setUserProximity(Proximity.NEAR);
                            } else {
                                teaseLib.host.setUserProximity(previous);
                            }
                        }
                        speechRecognitionInputMethod.setFaceToFace(speechProximity);
                    }
                });
    }

    private DeviceInteractionImplementations initScriptInteractions() {
        DeviceInteractionImplementations scriptInteractionImplementations = new DeviceInteractionImplementations();
        scriptInteractionImplementations.add(KeyReleaseDeviceInteraction.class,
                () -> new KeyReleaseDeviceInteraction(teaseLib, scriptRenderer));
        scriptInteractionImplementations.add(HumanPoseDeviceInteraction.class,
                () -> new HumanPoseDeviceInteraction(teaseLib.globals.get(TeaseLibAI.class), scriptRenderer));
        return scriptInteractionImplementations;
    }

    private static final float UNTIL_REMOVE_LIMIT = 1.5f;
    private static final float UNTIL_EXPIRED_LIMIT = 1.0f;

    protected void handleAutoRemove() {
        long startupTimeSeconds = teaseLib.getTime(TimeUnit.SECONDS);
        State persistedDomains = teaseLib.state(TeaseLib.DefaultDomain, StateImpl.Internal.PERSISTED_DOMAINS_STATE);
        Collection<Object> domains = new ArrayList<>(((StateImpl) persistedDomains).peers());
        for (Object domain : domains) {
            if (domain.equals(StateImpl.Domain.LAST_USED)) {
                continue;
            } else
                domain = domain.equals(StateImpl.Internal.DEFAULT_DOMAIN_NAME) ? TeaseLib.DefaultDomain : domain;
            if (!handleUntilRemoved((String) domain, startupTimeSeconds).applied()
                    && !handleUntilExpired((String) domain, startupTimeSeconds).applied()) {
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
        State untilState = teaseLib.state(domain, until);
        Set<Object> peers = ((StateImpl) untilState).peers();
        for (Object peer : new ArrayList<>(peers)) {
            State state = teaseLib.state(domain, peer);
            if (!cleanupRemovedUserItemReferences(state)) {
                remove(state, startupTimeSeconds, limitFactor);
            }
        }
        return untilState;
    }

    private boolean cleanupRemovedUserItemReferences(State state) {
        StateImpl stateImpl = AbstractProxy.stateImpl(state);
        if (stateImpl.peers().stream().filter(ItemGuid::isGuid).anyMatch(
                guid -> teaseLib.findItem(stateImpl.domain, stateImpl.item, (ItemGuid) guid) == Item.NotFound)) {
            state.remove();
            return true;
        } else {
            return false;
        }
    }

    private static void remove(State state, long startupTimeSeconds, float limitFactor) {
        // Very implicit way of testing for unavailable items
        Duration duration = state.duration();
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

        InputMethods inputMethods = teaseLib.globals.get(InputMethods.class);
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
        return answer.text.get(random.value(0, answer.text.size() - 1));
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
        int size = 0;
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
