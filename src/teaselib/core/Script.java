package teaselib.core;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import teaselib.Actor;
import teaselib.Answer;
import teaselib.Answer.Meaning;
import teaselib.Body;
import teaselib.Config;
import teaselib.Config.SpeechRecognition.Intention;
import teaselib.Gadgets;
import teaselib.Message;
import teaselib.Mood;
import teaselib.Replay;
import teaselib.ScriptFunction;
import teaselib.core.media.RenderedMessage.Decorator;
import teaselib.core.media.ScriptMessageDecorator;
import teaselib.core.speechrecognition.SpeechRecognitionResult.Confidence;
import teaselib.core.speechrecognition.SpeechRecognizer;
import teaselib.core.texttospeech.TextToSpeechPlayer;
import teaselib.core.ui.Choice;
import teaselib.core.ui.Choices;
import teaselib.core.ui.HeadGestureInputMethod;
import teaselib.core.ui.InputMethod;
import teaselib.core.ui.InputMethods;
import teaselib.core.ui.Prompt;
import teaselib.core.ui.Shower;
import teaselib.core.ui.SpeechRecognitionInputMethod;
import teaselib.core.util.ExceptionUtil;
import teaselib.core.util.ObjectMap;
import teaselib.core.util.QualifiedItem;
import teaselib.core.util.ReflectionUtils;
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
                getOrDefault(teaseLib, ScriptRenderer.class, ScriptRenderer::new),
                getOrDefault(teaseLib, TextToSpeechPlayer.class, () -> new TextToSpeechPlayer(teaseLib.config)));

        getOrDefault(teaseLib, Shower.class, () -> new Shower(teaseLib.host));
        getOrDefault(teaseLib, InputMethods.class, InputMethods::new);
        getOrDefault(teaseLib, SpeechRecognizer.class, () -> new SpeechRecognizer(teaseLib.config));

        try {
            teaseLib.config.addScriptSettings(namespace, namespace);
        } catch (IOException e) {
            ExceptionUtil.handleException(e, teaseLib.config, logger);
        }
    }

    private static <T> T getOrDefault(TeaseLib teaseLib, Class<T> clazz, Supplier<T> supplier) {
        return teaseLib.globals.getOrDefault(clazz, supplier);
    }

    /**
     * Construct a script with a different actor but with shared resources
     * 
     * @param script
     * @param actor
     */
    protected Script(Script script, Actor actor) {
        this(script.teaseLib, script.resources, actor, script.namespace, script.scriptRenderer,
                script.teaseLib.globals.get(TextToSpeechPlayer.class));
    }

    private Script(TeaseLib teaseLib, ResourceLoader resources, Actor actor, String namespace,
            ScriptRenderer scriptRenderer, TextToSpeechPlayer textToSpeech) {
        this.teaseLib = teaseLib;
        this.resources = resources;
        this.actor = actor;
        this.scriptRenderer = scriptRenderer;
        this.namespace = namespace.replace(" ", "_");

        textToSpeech.acquireVoice(actor, resources);
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
            for (Constructor<T> constructor : (Constructor<T>[]) scriptClass.getDeclaredConstructors()) {
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
            scriptRenderer.renderMessage(teaseLib, resources, message, decorators(textToSpeech), textToSpeech);
        } finally {
            displayImage = Message.ActorImage;
            mood = Mood.Neutral;
        }
    }

    private Optional<TextToSpeechPlayer> getTextToSpeech(boolean useTTS) {
        return useTTS ? Optional.ofNullable(teaseLib.globals.get(TextToSpeechPlayer.class)) : Optional.empty();
    }

    Decorator[] decorators(Optional<TextToSpeechPlayer> textToSpeech) {
        return new ScriptMessageDecorator(teaseLib.config, displayImage, actor, mood, resources,
                this::expandTextVariables, textToSpeech).messageModifiers();
    }

    protected void appendMessage(Message message) {
        Optional<TextToSpeechPlayer> textToSpeech = getTextToSpeech(true);
        scriptRenderer.appendMessage(teaseLib, resources, message, decorators(textToSpeech), textToSpeech);
    }

    protected void replaceMessage(Message message) {
        Optional<TextToSpeechPlayer> textToSpeech = getTextToSpeech(true);
        scriptRenderer.replaceMessage(teaseLib, resources, message, decorators(textToSpeech), textToSpeech);
    }

    protected final String showChoices(List<Answer> answers) {
        return showChoices(answers, null);
    }

    /**
     * {@code recognitionConfidence} defaults to {@link Confidence#Default}
     * 
     * @see Script#showChoices(List, ScriptFunction, Confidence)
     */
    protected final String showChoices(List<Answer> answers, ScriptFunction scriptFunction) {
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
    protected String showChoices(List<Answer> answers, ScriptFunction scriptFunction,
            Config.SpeechRecognition.Intention intention) {
        if (scriptRenderer.hasPrependedMessages()) {
            Optional<TextToSpeechPlayer> textToSpeech = getTextToSpeech(true);
            scriptRenderer.renderPrependedMessages(teaseLib, resources, actor, decorators(textToSpeech), textToSpeech);
        }

        QualifiedItem value = QualifiedItem.of(teaseLib.config.get(intention));
        Confidence recognitionConfidence = ReflectionUtils.getEnum(Confidence.class, value);

        Choices choices = choices(answers);
        InputMethods inputMethods = getInputMethods(scriptFunction, recognitionConfidence, choices);

        waitToStartScriptFunction(scriptFunction);
        if (scriptFunction == null || scriptFunction.relation != ScriptFunction.Relation.Autonomous) {
            scriptRenderer.stopBackgroundRenderers();
        }

        Prompt prompt = getPrompt(choices, inputMethods, scriptFunction);
        return showPrompt(prompt).text;
    }

    private Choices choices(List<Answer> answers) {
        List<Choice> choices = answers.stream()
                .map(answer -> new Choice(gesture(answer), answer.text, expandTextVariables(answer.text)))
                .collect(Collectors.toList());
        return new Choices(choices);
    }

    private static Gesture gesture(Answer answer) {
        if (answer.meaning == Meaning.YES)
            return Gesture.Nod;
        else if (answer.meaning == Meaning.NO)
            return Gesture.Shake;
        else
            return Gesture.None;
    }

    private Choice showPrompt(Prompt prompt) {
        Choice choice;
        try {
            choice = teaseLib.globals.get(Shower.class).show(prompt);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ScriptInterruptedException();
        }
        // TODO endAll() cancels renderers, but doesn't wait for completion
        // -> integrate this with endScene() which is just there to workaround the delay until the next say() command
        endAll();
        teaseLib.host.endScene();

        String chosen = "< " + (choice.gesture != Gesture.None ? choice.gesture + ": " : "") + choice.display;
        logger.info("{}", chosen);
        teaseLib.transcript.info(chosen);

        return choice;
    }

    private InputMethods getInputMethods(ScriptFunction scriptFunction, Confidence recognitionConfidence,
            Choices choices) {
        InputMethods inputMethods = new InputMethods(teaseLib.globals.get(InputMethods.class));
        inputMethods.add(teaseLib.host.inputMethod());

        if (Boolean.parseBoolean(teaseLib.config.get(Config.InputMethod.SpeechRecognition))) {
            inputMethods.add(
                    new SpeechRecognitionInputMethod(teaseLib.globals.get(SpeechRecognizer.class).get(actor.locale()),
                            recognitionConfidence, speechRecognitioneRejectedScript(scriptFunction)));
        }

        if (teaseLib.item(TeaseLib.DefaultDomain, Gadgets.Webcam).isAvailable()
                && teaseLib.state(TeaseLib.DefaultDomain, Body.InMouth).applied()
                && choices.toGestures().stream().filter(gesture -> gesture != Gesture.None).count() > 0) {
            inputMethods.add(new HeadGestureInputMethod(scriptRenderer.getInputMethodExecutorService(),
                    teaseLib.devices.get(MotionDetector.class)::getDefaultDevice));
        }

        return inputMethods;
    }

    private Prompt getPrompt(Choices choices, InputMethods inputMethods, ScriptFunction scriptFunction) {
        Prompt prompt = new Prompt(this, choices, inputMethods, scriptFunction);
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

    public List<String> expandTextVariables(List<String> prompts) {
        return allTextVariables().expand(prompts);
    }

    /**
     * Expand text variables. Expands all text variables({@link teaselib.util.TextVariables#Defaults},
     * {@link teaselib.core.TeaseLib#getTextVariables}, {@link teaselib.Actor#textVariables}.
     */
    public String expandTextVariables(String s) {
        return allTextVariables().expand(s);
    }

    private TextVariables allTextVariables() {
        return new TextVariables(teaseLib.getTextVariables(TeaseLib.DefaultDomain, actor.locale()),
                actor.textVariables);
    }

    ExecutorService getScriptFuntionExecutorService() {
        return scriptRenderer.getScriptFunctionExecutorService();
    }
}
