package teaselib.core.ui;

import static java.util.Arrays.asList;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import teaselib.core.events.Event;
import teaselib.core.speechrecognition.AudioSignalProblems;
import teaselib.core.speechrecognition.Confidence;
import teaselib.core.speechrecognition.Rule;
import teaselib.core.speechrecognition.SpeechRecognition;
import teaselib.core.speechrecognition.events.AudioSignalProblemOccuredEventArgs;
import teaselib.core.speechrecognition.events.SpeechRecognitionStartedEventArgs;
import teaselib.core.speechrecognition.events.SpeechRecognizedEventArgs;
import teaselib.core.ui.Prompt.Result;
import teaselib.util.SpeechRecognitionRejectedScript;

/**
 * @author Citizen-Cane
 *
 */
public class SpeechRecognitionInputMethod implements InputMethod {
    private static final double AUDIO_PROBLEM_PENALTY_WEIGHT = 0.005;

    private static final Logger logger = LoggerFactory.getLogger(SpeechRecognitionInputMethod.class);

    private static final String RECOGNITION_REJECTED_HANDLER_KEY = "Recognition Rejected";

    final SpeechRecognition speechRecognizer;
    final Confidence expectedConfidence;
    final Optional<SpeechRecognitionRejectedScript> speechRecognitionRejectedScript;
    final AudioSignalProblems audioSignalProblems;

    private final Event<SpeechRecognitionStartedEventArgs> speechRecognitionStartedEventHandler;
    private final Event<AudioSignalProblemOccuredEventArgs> audioSignalProblemEventHandler;
    private final Event<SpeechRecognizedEventArgs> speechDetectedEventHandler;
    private final Event<SpeechRecognizedEventArgs> recognitionRejected;
    private final Event<SpeechRecognizedEventArgs> recognitionCompleted;

    private final AtomicReference<Prompt> active = new AtomicReference<>();
    private boolean speechRecognitionRejectedHandlerSignaled = false;

    public SpeechRecognitionInputMethod(SpeechRecognition speechRecognizer, Confidence expectedConfidence,
            Optional<SpeechRecognitionRejectedScript> speechRecognitionRejectedScript) {
        this.speechRecognizer = speechRecognizer;
        this.expectedConfidence = expectedConfidence;
        this.speechRecognitionRejectedScript = speechRecognitionRejectedScript;
        this.audioSignalProblems = new AudioSignalProblems();

        this.speechRecognitionStartedEventHandler = eventArgs -> audioSignalProblems.clear();

        this.audioSignalProblemEventHandler = audioSignal -> audioSignalProblems.add(audioSignal.problem);

        this.speechDetectedEventHandler = eventArgs -> {
            if (audioSignalProblems.occured() && speechRecognizer.isSpeechRecognitionInProgress()) {
                logAudioSignalProblem(eventArgs.result);
                speechRecognizer.restartRecognition();
            }
        };

        this.recognitionRejected = eventArgs -> {
            if (eventArgs.result != null && eventArgs.result.length == 1) {
                if (!speechRecognitionRejectedHandlerSignaled && speechRecognitionRejectedScript.isPresent()
                        && speechRecognitionRejectedScript.get().canRun()) {
                    speechRecognitionRejectedHandlerSignaled = true;
                    signalHandlerInvocation(RECOGNITION_REJECTED_HANDLER_KEY);
                }
            }
        };

        this.recognitionCompleted = eventArgs -> {
            active.updateAndGet(prompt -> {
                if (eventArgs.result.length > 1) {
                    logger.info("More than one result:");
                }
                Arrays.stream(eventArgs.result).forEach(result -> logger.info("rules \n{}", result.prettyPrint()));

                if (audioSignalProblems.occured()) {
                    logAudioSignalProblem(eventArgs.result);
                    return prompt;
                } else {
                    Rule result = eventArgs.result[0];
                    if (eventArgs.result.length > 1) {
                        if (prompt.acceptedResult == Result.Accept.AllSame) {
                            result = distinct(eventArgs.result).orElseThrow();
                        } else {
                            throw new UnsupportedOperationException("TODO Define best result for multiple choices");
                        }
                    } else if (eventArgs.result.length == 1) {
                        result = eventArgs.result[0];
                    } else {
                        throw new IllegalStateException("RecognitionCompleted-event without result");
                    }

                    try {
                        List<Set<Integer>> choices = result.gather();

                        double penalty = audioSignalProblems.penalty();
                        if (!confidenceIsHighEnough(result, expectedConfidence, penalty)) {
                            if (confidenceIsHighEnough(result, expectedConfidence, 0)) {
                                logAudioSignalProblemPenalty(result, penalty);
                            } else {
                                logLackOfConfidence(result);
                            }
                            reject(eventArgs);
                            return prompt;
                        } else {
                            if (choices.isEmpty()) {
                                handleNoChoices(eventArgs, result);
                                return prompt;
                            } else if (prompt.acceptedResult == Result.Accept.Multiple) {
                                return handleMultipleChoices(eventArgs, result, prompt, choices);
                            } else if (prompt.acceptedResult == Result.Accept.AllSame) {
                                return handleDistinctChoice(eventArgs, result, prompt, choices);
                            } else {
                                throw new UnsupportedOperationException(prompt.acceptedResult.toString());
                            }
                        }
                    } catch (Exception e) {
                        prompt.setException(e);
                        endSpeechRecognition();
                        return null;
                    }
                }
            });
        };
    }

    private static Optional<Rule> distinct(Rule... result) {
        return distinct(asList(result));
    }

    public static Optional<Rule> distinct(List<Rule> result) {
        return result.stream().filter(r -> getCommonDistinctValue(r.gather()).isPresent()).reduce(Rule::maxProbability);
    }

    private void handleNoChoices(SpeechRecognizedEventArgs eventArgs, Rule result) {
        logger.warn("No choice rules in: {} - rejecting ", result);
        eventArgs.consumed = true;
        fireRecognitionRejectedEvent(eventArgs);
    }

    private Prompt handleMultipleChoices(SpeechRecognizedEventArgs eventArgs, Rule result, Prompt prompt,
            List<Set<Integer>> choices) {
        List<Integer> distinctChoices = choices.stream().map(indices -> indices.size() == 1 ? indices.iterator().next()
                : Prompt.Result.UNDEFINED.elements.iterator().next()).collect(Collectors.toList());
        Prompt.Result promptResult = new Prompt.Result(distinctChoices);
        if (promptResult.valid(prompt.choices)) {
            accept(prompt, promptResult);
            return null;
        } else {
            logger.warn("Undefined result index in {} : {} - rejecting", choices, result);
            reject(eventArgs);
            return prompt;
        }
    }

    private Prompt handleDistinctChoice(SpeechRecognizedEventArgs eventArgs, Rule result, Prompt prompt,
            List<Set<Integer>> choices) {
        Optional<Integer> distinctChoice = getCommonDistinctValue(choices);
        if (distinctChoice.isPresent()) {
            accept(prompt, new Prompt.Result(speechRecognizer.mapPhraseToChoice(distinctChoice.get())));
            return null;
        } else {
            logger.warn("No distinct choice {} in {} - rejecting", choices, result);
            reject(eventArgs);
            return prompt;
        }
    }

    private Prompt accept(Prompt prompt, Prompt.Result promptResult) {
        endSpeechRecognition();
        signal(prompt, promptResult);
        return null;
    }

    private void reject(SpeechRecognizedEventArgs eventArgs) {
        eventArgs.consumed = true;
        fireRecognitionRejectedEvent(eventArgs);
    }

    static Optional<Integer> getCommonDistinctValue(List<Set<Integer>> indicesSets) {
        if (indicesSets.isEmpty())
            return Optional.empty();

        Set<Integer> candidates = new HashSet<>(indicesSets.get(0));
        for (Integer candidate : new ArrayList<>(candidates)) {
            for (int i = 1; i < indicesSets.size(); i++) {
                if (!indicesSets.get(i).contains(candidate)) {
                    candidates.remove(candidate);
                    if (candidates.isEmpty())
                        return Optional.empty();
                    else
                        break;
                }
            }
        }

        return candidates.size() == 1 ? Optional.of(candidates.iterator().next()) : Optional.empty();
    }

    private void fireRecognitionRejectedEvent(SpeechRecognizedEventArgs eventArgs) {
        speechRecognizer.events.recognitionRejected.run(new SpeechRecognizedEventArgs(eventArgs.result));
    }

    private void logLackOfConfidence(Rule result) {
        logger.info("Rejecting result '{}' due to lack of confidence (expected {})", result, expectedConfidence);
    }

    private void logAudioSignalProblem(Rule[] result) {
        logger.info("Rejecting result '{}' due to audio signal problems {}", result, audioSignalProblems);
    }

    private void logAudioSignalProblemPenalty(Rule result, double penalty) {
        logger.info("Rejecting result '{}' due to audio signal problem penalty  (required  {} + {} + = {})", result,
                expectedConfidence, penalty, expectedConfidence.probability + penalty);
    }

    private static boolean confidenceIsHighEnough(Rule result, Confidence expected, double penalty) {
        return result.probability - penalty * AUDIO_PROBLEM_PENALTY_WEIGHT >= expected.probability
                && result.confidence.isAsHighAs(expected);
    }

    private void signal(Prompt prompt, Prompt.Result result) {
        prompt.lock.lock();
        try {
            prompt.signalResult(this, result);
        } finally {
            prompt.lock.unlock();
        }
    }

    private void signalHandlerInvocation(String handlerKey) {
        Prompt prompt = active.get();
        prompt.lock.lock();
        try {
            prompt.signalHandlerInvocation(handlerKey);
        } finally {
            prompt.lock.unlock();
        }
    }

    @Override
    public void show(Prompt prompt) {
        Objects.requireNonNull(prompt);
        active.set(prompt);
        startSpeechRecognition(prompt);
    }

    @Override
    public boolean dismiss(Prompt prompt) throws InterruptedException {
        Prompt activePrompt = active.getAndSet(null);
        if (activePrompt == null) {
            return false;
        } else if (activePrompt != prompt) {
            throw new IllegalStateException("Trying to dismiss wrong prompt");
        } else {
            endSpeechRecognition();
            return true;
        }
    }

    // TODO Resolve race condition: lazy initialization versus dismiss in main thread on timeout
    // -> recognition not started yet but main thread dismisses prompt

    private void startSpeechRecognition(Prompt prompt) {
        if (speechRecognizer.isActive()) {
            throw new IllegalStateException("Speech recognizer already active");
        }

        speechRecognizer.events.recognitionStarted.add(speechRecognitionStartedEventHandler);
        speechRecognizer.events.audioSignalProblemOccured.add(audioSignalProblemEventHandler);
        speechRecognizer.events.speechDetected.add(speechDetectedEventHandler);
        speechRecognizer.events.recognitionRejected.add(recognitionRejected);
        speechRecognizer.events.recognitionCompleted.add(recognitionCompleted);

        speechRecognizer.startRecognition(prompt.choices, expectedConfidence);
    }

    private void endSpeechRecognition() {
        try {
            speechRecognizer.endRecognition();
        } finally {
            speechRecognizer.events.recognitionStarted.remove(speechRecognitionStartedEventHandler);
            speechRecognizer.events.audioSignalProblemOccured.remove(audioSignalProblemEventHandler);
            speechRecognizer.events.speechDetected.remove(speechDetectedEventHandler);
            speechRecognizer.events.recognitionRejected.remove(recognitionRejected);
            speechRecognizer.events.recognitionCompleted.remove(recognitionCompleted);
        }
    }

    @Override
    public Map<String, Runnable> getHandlers() {
        HashMap<String, Runnable> handlers = new HashMap<>();
        if (speechRecognitionRejectedScript.isPresent()) {
            SpeechRecognitionRejectedScript script = speechRecognitionRejectedScript.get();
            handlers.put(RECOGNITION_REJECTED_HANDLER_KEY, script::run);
        }
        return handlers;
    }

    @Override
    public String toString() {
        return "SpeechRecognizer=" + speechRecognizer + " confidence=" + expectedConfidence;
    }
}
