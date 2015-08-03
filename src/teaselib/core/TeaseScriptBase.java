package teaselib.core;

import java.io.IOException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;

import teaselib.Actor;
import teaselib.Message;
import teaselib.Mood;
import teaselib.TeaseLib;
import teaselib.core.events.Delegate;
import teaselib.core.events.Event;
import teaselib.core.speechrecognition.SpeechRecognition;
import teaselib.core.speechrecognition.SpeechRecognitionImplementation;
import teaselib.core.speechrecognition.SpeechRecognitionResult;
import teaselib.core.speechrecognition.events.SpeechRecognitionStartedEventArgs;
import teaselib.core.speechrecognition.events.SpeechRecognizedEventArgs;
import teaselib.core.texttospeech.TextToSpeechPlayer;

public abstract class TeaseScriptBase {

    public final TeaseLib teaseLib;
    public final Actor actor;
    public final String namespace;

    public static final String Timeout = "Timeout";

    protected String mood = Mood.Neutral;
    protected String displayImage = Message.DominantImage;

    protected static final int NoTimeout = 0;

    private static final MediaRendererQueue renderQueue = new MediaRendererQueue();
    private final Deque<MediaRenderer> deferredRenderers = new ArrayDeque<MediaRenderer>();
    private ExecutorService choiceScriptFunctionExecutor = Executors
            .newFixedThreadPool(1);

    /**
     * Construct a new script instance
     * 
     * @param teaseLib
     * @param locale
     */
    protected TeaseScriptBase(TeaseLib teaseLib, Actor actor, String namespace) {
        this.teaseLib = teaseLib;
        this.actor = actor;
        this.namespace = namespace;
        acquireVoice(actor);
    }

    /**
     * Construct a script with a different actor but with shared resources
     * 
     * @param script
     * @param actor
     */
    protected TeaseScriptBase(TeaseScriptBase script, Actor actor) {
        this.teaseLib = script.teaseLib;
        this.actor = actor;
        this.namespace = script.namespace;
        acquireVoice(actor);
    }

    private void acquireVoice(Actor actor) {
        try {
            teaseLib.speechSynthesizer.selectVoice(new Message(actor));
        } catch (IOException e) {
            TeaseLib.log(this, e);
        }
    }

    protected static List<String> buildChoicesFromArray(String choice,
            String... more) {
        List<String> choices = new ArrayList<String>(1 + more.length);
        choices.add(choice);
        choices.addAll(Arrays.asList(more));
        return choices;
    }

    public void completeStarts() {
        renderQueue.completeStarts();
    }

    public void completeMandatory() {
        renderQueue.completeMandatories();
    }

    /**
     * Just wait for everything to be rendered (messages displayed, sounds
     * played, delay expired), and continue execution of the script. This won't
     * display a button, it just waits.
     */
    public void completeAll() {
        renderQueue.completeAll();
    }

    /**
     * Stop rendering and end all render threads
     */
    public void endAll() {
        renderQueue.endAll();
    }

    protected void renderMessage(Message message,
            TextToSpeechPlayer speechSynthesizer, String displayImage,
            String mood) {
        synchronized (renderQueue) {
            renderDeferred();
            // Clone the actor to prevent the wrong actor image to be displayed
            // when changing the actor images right after saying a message.
            // Without cloning one of the new actor images would be displayed
            // with the current message because the actor is shared between
            // script and message
            Message parsedMessage = new Message(new Actor(message.actor));
            for (Message.Part part : message.getParts()) {
                if (part.type == Message.Type.Text) {
                    parsedMessage.add(parsedMessage.new Part(part.type,
                            replaceVariables(part.value)));
                } else {
                    parsedMessage.add(part);
                }
            }
            Set<String> hints = getHints();
            hints.add(mood);
            RenderMessage renderMessage = new RenderMessage(parsedMessage,
                    speechSynthesizer, displayImage, hints);
            renderQueue.start(renderMessage, teaseLib);
            renderQueue.completeStarts();
        }
    }

    private static Set<String> getHints() {
        Set<String> hints = new HashSet<String>();
        // Within messages, images might change fast, and changing
        // the camera position, image size or aspect would be too distracting
        hints.add(Images.SameCameraPosition);
        hints.add(Images.SameResolution);
        return hints;
    }

    protected void addDeferred(MediaRenderer renderer) {
        synchronized (deferredRenderers) {
            deferredRenderers.add(renderer);
        }
    }

    private void clearDeferred() {
        synchronized (deferredRenderers) {
            deferredRenderers.clear();
        }
    }

    private void renderDeferred() {
        synchronized (deferredRenderers) {
            completeAll();
            renderQueue.start(deferredRenderers, teaseLib);
            deferredRenderers.clear();
        }
    }

    private final class TimeoutClick {
        public boolean clicked = false;
    }

    private class ScriptFutureTask extends FutureTask<String> {
        private final TimeoutClick timeout;

        public ScriptFutureTask(final Runnable scriptFunction,
                final List<String> derivedChoices, final TimeoutClick timeout) {
            super(new Callable<String>() {
                @Override
                public String call() throws Exception {
                    try {
                        scriptFunction.run();
                        // Keep choices available until the last part of
                        // the script function has finished rendering
                        completeAll();
                    } catch (ScriptInterruptedException e) {
                        // At this point the script function may have added
                        // deferred renderers to the queue.
                        // Avoid executing these renderers with the next
                        // call to renderMessage()
                        clearDeferred();
                        return null;
                    }
                    // Script function finished
                    List<Delegate> clickables = teaseLib.host
                            .getClickableChoices(derivedChoices);
                    if (!clickables.isEmpty()) {
                        Delegate clickable = clickables.get(0);
                        if (clickable != null) {
                            // Signal timeout and click any button
                            timeout.clicked = true;
                            // Click any delegate
                            clickables.get(0).run();
                        } else {
                            // Host implementation is incomplete
                            throw new IllegalStateException(
                                    "Host didn't return clickables for choices: "
                                            + derivedChoices.toString());
                        }
                    }
                    // Now if the script function is interrupted, there may
                    // still be deferred renderers set for the next call to
                    // renderMessage()
                    // These must be cleared, or they will be run with the
                    // next renderMessage() call in the main script thread
                    return null;
                }
            });
            this.timeout = timeout;
        }
    }

    private class SpeechRecognitionHypothesisEventHandler {
        /**
         * Hypothesis speech recognition is used for longer sentences, as short
         * sentences or single word recognitions are prone to error. In fact,
         * for single word phrases, the recognizer may recognize anything.
         */
        final static int HypothesisMinimumNumberOfWords = 3;

        /**
         * This adjusts the sensibility of the hypothesis rating. The better the
         * microphone, the higher this value should be. For a standard webcam,
         * 1/2 seems to be a good start, however low values may lead to wrong
         * recognitions
         */
        final static double HypothesisMinimumAccumulatedWeight = 0.5;

        public ScriptFutureTask scriptTask = null;

        // private final SpeechRecognition speechRecognizer;
        private final Event<SpeechRecognitionImplementation, SpeechRecognitionStartedEventArgs> recognitionStarted;
        private final Event<SpeechRecognitionImplementation, SpeechRecognizedEventArgs> speechDetected;
        private final Event<SpeechRecognitionImplementation, SpeechRecognizedEventArgs> recognitionRejected;
        private final Event<SpeechRecognitionImplementation, SpeechRecognizedEventArgs> recognitionCompleted;

        private double[] hypothesisAccumulatedWeights;
        private String[] hypothesisProgress;

        public SpeechRecognitionHypothesisEventHandler(
                final SpeechRecognition speechRecognizer,
                final List<String> derivedChoices,
                final List<Integer> srChoiceIndices) {
            super();
            // this.speechRecognizer = speechRecognizer;
            this.recognitionStarted = recognitionStarted(derivedChoices);
            this.speechDetected = speechDetected(derivedChoices);
            this.recognitionRejected = recognitionRejected(derivedChoices,
                    srChoiceIndices);
            this.recognitionCompleted = recognitionCompleted(derivedChoices,
                    srChoiceIndices);
            speechRecognizer.events.recognitionStarted.add(recognitionStarted);
            speechRecognizer.events.speechDetected.add(speechDetected);
            speechRecognizer.events.recognitionRejected
                    .add(recognitionRejected);
            speechRecognizer.events.recognitionCompleted
                    .add(recognitionCompleted);
        }

        private Event<SpeechRecognitionImplementation, SpeechRecognitionStartedEventArgs> recognitionStarted(
                final List<String> derivedChoices) {
            return new Event<SpeechRecognitionImplementation, SpeechRecognitionStartedEventArgs>() {
                @Override
                public void run(SpeechRecognitionImplementation sender,
                        SpeechRecognitionStartedEventArgs eventArgs) {
                    final int size = derivedChoices.size();
                    hypothesisAccumulatedWeights = new double[size];
                    hypothesisProgress = new String[size];
                    for (int i = 0; i < hypothesisAccumulatedWeights.length; i++) {
                        hypothesisAccumulatedWeights[i] = 0;
                        hypothesisProgress[i] = "";
                    }
                }
            };
        }

        private Event<SpeechRecognitionImplementation, SpeechRecognizedEventArgs> speechDetected(
                final List<String> derivedChoices) {
            return new Event<SpeechRecognitionImplementation, SpeechRecognizedEventArgs>() {
                @Override
                public void run(SpeechRecognitionImplementation sender,
                        SpeechRecognizedEventArgs eventArgs) {
                    // The Microsoft SAPI based SR is supposed to return
                    // multiple results, however usually only one entry is
                    // returned when hypothesizing a recognition result and
                    // multiple choices start with the same words
                    final SpeechRecognitionResult[] recognitionResults = eventArgs.result;
                    if (recognitionResults.length == 1) {
                        // Manually search for all choices that start with the
                        // hypothesis, and add the probability weight for each
                        final SpeechRecognitionResult hypothesis = eventArgs.result[0];
                        String hypothesisText = hypothesis.text;
                        final double propabilityWeight = propabilityWeight(hypothesis);
                        for (int index = 0; index < derivedChoices.size(); index++) {
                            String choice = derivedChoices.get(index)
                                    .toLowerCase();
                            if (choice.startsWith(hypothesisText.toLowerCase())) {
                                updateHypothesisProgress(index, hypothesisText,
                                        propabilityWeight);
                            }
                        }
                    } else {
                        for (SpeechRecognitionResult hypothesis : recognitionResults) {
                            // The first word(s) are usually incorrect,
                            // whereas later hypothesis usually match better
                            final double propabilityWeight = propabilityWeight(hypothesis);
                            final int index = hypothesis.index;
                            updateHypothesisProgress(index, hypothesis.text,
                                    propabilityWeight);
                        }
                    }
                }

                private void updateHypothesisProgress(int index,
                        final String hypothesisText, double propabilityWeight) {
                    // Only update if the hypothesis is progressing,
                    // e.g. sort out detection duplicates
                    if (hypothesisText.startsWith(hypothesisProgress[index])
                            && hypothesisText.length() > hypothesisProgress[index]
                                    .length()) {
                        hypothesisAccumulatedWeights[index] += propabilityWeight;
                        hypothesisProgress[index] = hypothesisText;
                        TeaseLib.log("'" + hypothesisText + "' + "
                                + propabilityWeight);
                    } else {
                        TeaseLib.log("Ignoring hypothesis '" + hypothesisText);
                    }
                }

                private double propabilityWeight(SpeechRecognitionResult result) {
                    return result.propability * wordCount(result.text);
                }
            };
        }

        private Event<SpeechRecognitionImplementation, SpeechRecognizedEventArgs> recognitionRejected(
                final List<String> derivedChoices,
                final List<Integer> srChoiceIndices) {
            return new Event<SpeechRecognitionImplementation, SpeechRecognizedEventArgs>() {
                @Override
                public void run(SpeechRecognitionImplementation sender,
                        SpeechRecognizedEventArgs eventArgs) {
                    // choose the choice with the highest hypothesis weight
                    double maxValue = 0;
                    int maxChoiceIndex = 0;
                    for (int i = 0; i < hypothesisAccumulatedWeights.length; i++) {
                        double value = hypothesisAccumulatedWeights[i];
                        TeaseLib.log("Result " + i + ": '"
                                + derivedChoices.get(i) + "' hypothesisCount="
                                + value);
                        if (value > maxValue) {
                            maxValue = value;
                            maxChoiceIndex = i;
                        }
                    }
                    // sort out the case where two or more recognition
                    // results have the same weight.
                    // This happens when they all start with the same text
                    int numberOfCandidates = 0;
                    for (double weight : hypothesisAccumulatedWeights) {
                        if (weight == maxValue) {
                            numberOfCandidates++;
                        }
                    }
                    if (numberOfCandidates == 1) {
                        final String choice = derivedChoices
                                .get(maxChoiceIndex);
                        int wordCount = wordCount(choice);
                        // prompts with few words need a higher weight to be
                        // accepted
                        double hypothesisAccumulatedWeight = wordCount >= HypothesisMinimumNumberOfWords ? HypothesisMinimumAccumulatedWeight
                                : HypothesisMinimumAccumulatedWeight
                                        * (HypothesisMinimumNumberOfWords
                                                - wordCount + 1);
                        boolean choiceWeightAccepted = maxValue >= hypothesisAccumulatedWeight;
                        int choiceHypothesisCount = wordCount(hypothesisProgress[maxChoiceIndex]);
                        // Prompts with few words need more consistent speech
                        // detection events (doesn't alternate between different
                        // choices)
                        boolean choiceDetectionCountAccepted = choiceHypothesisCount >= HypothesisMinimumNumberOfWords
                                || choiceHypothesisCount >= wordCount;
                        if (choiceWeightAccepted
                                && choiceDetectionCountAccepted) {
                            clickChoiceElement(derivedChoices, srChoiceIndices,
                                    maxChoiceIndex, choice);
                        }
                        if (!choiceWeightAccepted) {
                            TeaseLib.log("Phrase '"
                                    + choice
                                    + "' accumulated weight="
                                    + maxValue
                                    + " < "
                                    + hypothesisAccumulatedWeight
                                    + " is too low to accept hypothesis-based recognition");
                        }
                        if (!choiceDetectionCountAccepted) {
                            TeaseLib.log("Phrase '"
                                    + choice
                                    + "' detection count="
                                    + choiceHypothesisCount
                                    + " < "
                                    + " is too low to accept hypothesis-based recognition");
                        }
                    } else {
                        TeaseLib.log("Speech recognition hypothesis dropped - several recognition results share the same accumulated weight - can't decide");
                    }
                }
            };
        }

        private int wordCount(String text) {
            String preparatedText = text;
            preparatedText = preparatedText.replace(",", " ");
            preparatedText = preparatedText.replace(".", " ");
            preparatedText = preparatedText.replace("!", " ");
            preparatedText.trim();
            return preparatedText.split(" ").length;
        }

        private Event<SpeechRecognitionImplementation, SpeechRecognizedEventArgs> recognitionCompleted(
                final List<String> derivedChoices,
                final List<Integer> srChoiceIndices) {
            return new Event<SpeechRecognitionImplementation, SpeechRecognizedEventArgs>() {
                @Override
                public void run(SpeechRecognitionImplementation sender,
                        SpeechRecognizedEventArgs eventArgs) {
                    if (eventArgs.result.length == 1) {
                        // Find the button to click
                        SpeechRecognitionResult speechRecognitionResult = eventArgs.result[0];
                        if (!speechRecognitionResult.isChoice(derivedChoices)) {
                            throw new IllegalArgumentException(
                                    speechRecognitionResult.toString());
                        }
                        clickChoiceElement(derivedChoices, srChoiceIndices,
                                speechRecognitionResult.index,
                                speechRecognitionResult.text);
                    } else {
                        // none or more than one result means incorrect
                        // recognition
                    }
                }

            };
        }

        private void clickChoiceElement(final List<String> derivedChoices,
                final List<Integer> srChoiceIndices, int choice, String text) {
            // Assign the result even if the buttons have been unrealized
            srChoiceIndices.add(choice);
            List<Delegate> uiElements = teaseLib.host
                    .getClickableChoices(derivedChoices);
            try {
                Delegate delegate = uiElements.get(choice);
                if (delegate != null) {
                    if (scriptTask != null) {
                        scriptTask.cancel(true);
                    }
                    // Click the button
                    delegate.run();
                    TeaseLib.log("Clicked delegate for '" + text + "' index="
                            + choice);
                } else {
                    TeaseLib.log("Button gone for choice " + choice + ": "
                            + text);
                }
            } catch (Throwable t) {
                TeaseLib.log(this, t);
            }
        }

        private void dispose() {
            SpeechRecognition speechRecognizer = teaseLib.speechRecognizer
                    .get(actor.locale);
            if (recognitionStarted != null)
                speechRecognizer.events.recognitionStarted
                        .remove(recognitionStarted);
            if (speechDetected != null)
                speechRecognizer.events.speechDetected.remove(speechDetected);
            if (recognitionRejected != null)
                speechRecognizer.events.recognitionRejected
                        .remove(recognitionRejected);
            if (recognitionCompleted != null)
                speechRecognizer.events.recognitionCompleted
                        .remove(recognitionCompleted);
        }
    }

    /**
     * @param scriptFunction
     * @param choice
     *            The first choice. This function doesn't make sense without
     *            showing at least one item, so one choice is mandatory
     * @param moreChoices
     *            More choices
     * @return
     */
    protected String showChoices(final Runnable scriptFunction,
            List<String> choices) {
        // argument checking and text variable replacement
        final List<String> derivedChoices = replaceTextVariables(choices);
        TeaseLib.log("showChoices: " + derivedChoices.toString());
        // The result of this future task is never queried for,
        // instead a timeout is signaled via the TimeoutClick class
        // Run the script function while displaying the button
        // Speech recognition
        final List<Integer> srChoiceIndices = new ArrayList<Integer>(1);
        SpeechRecognitionHypothesisEventHandler eventHandler;
        SpeechRecognition speechRecognizer = teaseLib.speechRecognizer
                .get(actor.locale);
        final boolean recognizeSpeech = speechRecognizer.isReady();
        eventHandler = new SpeechRecognitionHypothesisEventHandler(
                speechRecognizer, derivedChoices, srChoiceIndices);
        if (recognizeSpeech) {
            speechRecognizer.startRecognition(derivedChoices);
        } else {
            // eventHandler = null;
        }
        final ScriptFutureTask scriptTask;
        if (scriptFunction != null) {
            scriptTask = new ScriptFutureTask(scriptFunction, derivedChoices,
                    new TimeoutClick());
        } else {
            scriptTask = null;
        }
        eventHandler.scriptTask = scriptTask;
        // Get the user's choice
        int choiceIndex;
        try {
            if (scriptTask != null) {
                choiceScriptFunctionExecutor.execute(scriptTask);
                renderQueue.completeStarts();
                // TODO completeStarts() doesn't work because first we need to
                // wait for render threads that can be waited for completing
                // their starts
                // Workaround: A bit unsatisfying, but otherwise the choice
                // buttons would appear too early
                teaseLib.sleep(300, TimeUnit.MILLISECONDS);
            }
            choiceIndex = teaseLib.host.reply(derivedChoices);
            if (scriptTask != null) {
                TeaseLib.logDetail("choose: Cancelling script task");
                scriptTask.cancel(true);
            }
            if (recognizeSpeech) {
                TeaseLib.logDetail("choose: completing speech recognition");
                speechRecognizer.completeSpeechRecognitionInProgress();
            }
        } finally {
            TeaseLib.logDetail("choose: stopping speech recognition");
            speechRecognizer.stopRecognition();
            eventHandler.dispose();
        }
        // Assign result from speech recognition
        // script task timeout or button click
        // supporting object identity by
        // returning an item of the original choices list
        String chosen = null;
        if (!srChoiceIndices.isEmpty()) {
            // Use the first speech recognition result
            choiceIndex = srChoiceIndices.get(0);
            chosen = choices.get(choiceIndex);
        } else if (scriptTask != null && scriptTask.timeout.clicked) {
            chosen = Timeout;
        } else {
            chosen = choices.get(choiceIndex);
        }
        TeaseLib.logDetail("showChoices: ending render queue");
        renderQueue.endAll();
        return chosen;
    }

    protected String get(Persistence.TextVariable variable) {
        String value = teaseLib.persistence.get(variable, actor.locale);
        if (value != null) {
            return value;
        } else if (variable.fallback != null) {
            return get(variable.fallback);
        }
        return variable.toString();
    }

    private String replaceVariables(String text) {
        String parsedText = text;
        for (Persistence.TextVariable name : Persistence.TextVariable.values()) {
            parsedText = replaceTextVariable(parsedText, name);
        }
        return parsedText;
    }

    private String replaceTextVariable(String text, Persistence.TextVariable var) {
        final String value = var.toString();
        text = replaceTextVariable(text, var, "#" + value);
        text = replaceTextVariable(text, var, "#" + value.toLowerCase());
        return text;
    }

    private String replaceTextVariable(String text,
            Persistence.TextVariable var, String match) {
        if (text.contains(match)) {
            String value = get(var);
            text = text.replace(match, value);
        }
        return text;
    }

    private List<String> replaceTextVariables(List<String> choices) {
        final List<String> derivedChoices = new ArrayList<String>(
                choices.size());
        for (String derivedChoice : choices) {
            if (derivedChoice != null) {
                derivedChoices.add(replaceVariables(derivedChoice));
            } else {
                throw new IllegalArgumentException("Choice may not be null");
            }
        }
        return derivedChoices;
    }

    protected String makePropertyName(String name) {
        return namespace + "." + name;
    }
}