package teaselib.core.speechrecognition;

import static java.util.stream.Collectors.toList;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiFunction;
import java.util.function.IntUnaryOperator;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import teaselib.core.AudioSync;
import teaselib.core.configuration.Configuration;
import teaselib.core.events.Event;
import teaselib.core.speechrecognition.events.AudioLevelUpdatedEventArgs;
import teaselib.core.speechrecognition.events.AudioSignalProblemOccuredEventArgs;
import teaselib.core.speechrecognition.events.SpeechRecognitionStartedEventArgs;
import teaselib.core.speechrecognition.events.SpeechRecognizedEventArgs;
import teaselib.core.speechrecognition.srgs.PhraseString;
import teaselib.core.ui.Choices;
import teaselib.core.ui.InputMethod;
import teaselib.core.ui.Prompt;
import teaselib.core.ui.Prompt.Result;
import teaselib.util.math.WeightNormal;

/**
 * @author Citizen-Cane
 *
 */
public class SpeechRecognitionInputMethod implements InputMethod {
    private static final Logger logger = LoggerFactory.getLogger(SpeechRecognitionInputMethod.class);

    private static final float AUDIO_PROBLEM_PENALTY_WEIGHT = 0.005f;

    public enum Notification implements InputMethod.Notification {
        RecognitionRejected
    }

    protected final SpeechRecognizer speechRecognizer;
    private final Map<Locale, SpeechRecognition> usedRecognitionInstances = new HashMap<>();
    private final AudioSignalProblems audioSignalProblems;
    public final SpeechRecognitionEvents events;
    private final AudioSync audioSync;

    private final Event<SpeechRecognitionStartedEventArgs> speechRecognitionStartedEventHandler;
    private final Event<AudioLevelUpdatedEventArgs> audioLevelUpdatedEventHandler;
    private final Event<AudioSignalProblemOccuredEventArgs> audioSignalProblemEventHandler;
    private final Event<SpeechRecognizedEventArgs> speechDetectedEventHandler;
    private final Event<SpeechRecognizedEventArgs> recognitionRejected;
    private final Event<SpeechRecognizedEventArgs> recognitionCompleted;

    private final AtomicReference<Prompt> active = new AtomicReference<>();
    private final AtomicReference<SpeechRecognition> activeRecognizer = new AtomicReference<>();
    private Hypothesis hypothesis = null;

    public SpeechRecognitionInputMethod(Configuration config, AudioSync audioSync) {
        this.speechRecognizer = new SpeechRecognizer(config, audioSync);
        this.audioSignalProblems = new AudioSignalProblems();
        this.events = new SpeechRecognitionEvents();
        this.audioSync = speechRecognizer.audioSync;

        this.speechRecognitionStartedEventHandler = this::handleRecognitionStarted;
        this.audioLevelUpdatedEventHandler = this::handleAudioLevelUpdated;
        this.audioSignalProblemEventHandler = this::handleAudioSignalProblemDetected;
        this.speechDetectedEventHandler = this::handleSpeechDetected;
        this.recognitionRejected = this::handleRecognitionRejected;
        this.recognitionCompleted = this::handleRecogntionCompleted;
    }

    private void handleRecognitionStarted(SpeechRecognitionStartedEventArgs eventArgs) {
        active.updateAndGet(prompt -> {
            if (prompt == null) {
                return null;
            } else {
                try {
                    clearDetectedSpeech();
                    events.recognitionStarted.fire(eventArgs);
                    return prompt;
                } catch (Exception e) {
                    prompt.setException(e);
                    clearDetectedSpeech();
                    getRecognizer(prompt.choices.locale).endRecognition();
                    return null;
                }
            }
        });
    }

    private void clearDetectedSpeech() {
        audioSignalProblems.clear();
        hypothesis = null;
    }

    private void handleAudioLevelUpdated(AudioLevelUpdatedEventArgs audioLevelUpdatedEventArgs) {
        active.updateAndGet(prompt -> {
            try {
                events.audioLevelUpdated.fire(audioLevelUpdatedEventArgs);
                return prompt;
            } catch (Exception e) {
                prompt.setException(e);
                clearDetectedSpeech();
                getRecognizer(prompt.choices.locale).endRecognition();
                return null;
            }
        });
    }

    private void handleAudioSignalProblemDetected(
            AudioSignalProblemOccuredEventArgs audioSignalProblemOccuredEventArgs) {
        active.updateAndGet(prompt -> {
            try {
                audioSignalProblems.add(audioSignalProblemOccuredEventArgs.problem);
                events.audioSignalProblemOccured.fire(audioSignalProblemOccuredEventArgs);
                return prompt;
            } catch (Exception e) {
                prompt.setException(e);
                clearDetectedSpeech();
                getRecognizer(prompt.choices.locale).endRecognition();
                return null;
            }
        });
    }

    /**
     * Select the hypothesis with the highest probability.
     * 
     * @param eventArgs
     */
    private void handleSpeechDetected(SpeechRecognizedEventArgs eventArgs) {
        // DONE
        // For single rules or long rules, confidence decreases with length
        // -> forward earlier accepted hypothesis - this is already handled by the existing code
        // + for single confirmations, confidence is normal anyway
        // - longer rules within multiple choices may cause recognition problems
        // -> remember earlier hypothesis

        // DONE
        // in the logs aborting recognition on audio problems looks suspicious in the log - multiple recognitions?
        // -> only check for audio problems when rejecting speech - should be okay if recognized
        // + make it as optional as possible

        // RESOLVED by confidence function
        // first child rule may have very low confidence value, second very high, how to measure minimal length
        // -> number of distinct rules, words, or vowels -> words or vowels

        // RESOLVED by confidence function
        // - confidence decreases at the end of the hypothesis
        // (distinct [1] = 0.79, common [0,1] = 0.76, distinct NUL_RULE [0]= 0.58;
        // -> cut off hypothesis when probability falls below accepted threshold,
        // or when average falls below threshold
        //
        // unrealized NULL rules seem to have confidence == 1.0 -> all NULL rules have C=1.0

        // DONE
        // map phrase to choice indices before evaluating best phrase
        // -> better match on large phrase sets, supports hypothesis as well

        // TODO: map rule indices back to choice indices when building srgs nodes
        // - much simpler then processing them here
        // - allows for optimizing rules, especially optional ones

        // TODO remember multiple hypotheses
        // + keep all hypothesis results
        // + keep those who continue in the next event
        // + copy over confidence from previous hypothesis

        active.updateAndGet(prompt -> {
            if (prompt == null) {
                return null;
            } else {
                SpeechRecognition recognizer = getRecognizer(prompt);
                try {
                    if (audioSignalProblems.exceedLimits() && recognizer.audioSync.inProgress()) {
                        logTooManyAudioSignalProblems();
                        recognizer.restartRecognition();
                    } else {
                        if (prompt.acceptedResult == Result.Accept.Distinct) {
                            var preparedChoices = recognizer.preparedChoices();
                            // TODO accept indistinct results reject them in recognition completed/rejected
                            // -> better feedback for hypothesis building
                            Optional<Hypothesis> best = bestHypothesis(eventArgs.result, prompt.choices,
                                    preparedChoices, hypothesis);
                            if (best.isPresent()) {
                                hypothesis = best.get();
                                events.speechDetected.fire(new SpeechRecognizedEventArgs(hypothesis));
                            } else {
                                logger.info("Ignoring detected speech without distinct alternates");
                                hypothesis = null;
                            }
                        } else if (prompt.acceptedResult == Result.Accept.Multiple) {
                            hypothesis = null;
                        } else {
                            throw new UnsupportedOperationException(prompt.acceptedResult.toString());
                        }
                    }
                    return prompt;
                } catch (Exception e) {
                    prompt.setException(e);
                    clearDetectedSpeech();
                    recognizer.endRecognition();
                    return null;
                }
            }
        });
    }

    private Optional<Hypothesis> bestHypothesis(List<Rule> rules, Choices choices, PreparedChoices preparedChoices,
            Hypothesis currentHypothesis) {
        Optional<Rule> result = SpeechRecognitionInputMethod.bestSingleResult(rules, preparedChoices.mapper());
        if (result.isPresent()) {
            var rule = result.get();
            if (rule.indices.equals(Rule.NoIndices)) {
                logger.info("Ignoring hypothesis {} since it contains results from multiple phrases", rule);
                return Optional.empty();
            } else {
                double expectedConfidence = expectedConfidence(choices, rule);
                // Weighted probability is very low in tests with short hypotheses even with optimal probability of 1.0
                // This is intended because probability/confidence values of short short hypotheses cannot be trusted
                // -> confidence moves towards ground truth when more speech is detected
                // The legal way to have such results accepted is to provide them as alternative phrases
                float weightedProbability = WeightNormal.square(preparedChoices.hypothesisWeight(rule))
                        * rule.probability;
                Rule.Precoditions.checkProbability(weightedProbability);

                if (weightedProbability >= expectedConfidence) {
                    if (currentHypothesis == null) {
                        logger.info("Considering as new hypothesis");
                        return Optional.of(new Hypothesis(rule, weightedProbability));
                    } else if (currentHypothesis.indices.containsAll(rule.indices)) {
                        float hypothesisProbability = preparedChoices.hypothesisWeight(currentHypothesis);
                        Rule.Precoditions.checkProbability(hypothesisProbability);
                        float h = currentHypothesis.children.size();
                        float r = rule.children.size();
                        float average = (hypothesisProbability * h + rule.probability * r) / (h + r);
                        logger.info("Considering as hypothesis");
                        return Optional.of(new Hypothesis(rule, Math.max(average, rule.probability)));
                    } else {
                        logger.info("Choice indices changed - reconsidering hypothesis");
                        return Optional.of(new Hypothesis(rule, weightedProbability));
                    }
                } else {
                    logger.info("Weighted hypothesis confidence {} < expected hypothesis confidence {}", //
                            weightedProbability, expectedConfidence);
                    return Optional.of(new Hypothesis(rule, weightedProbability));
                }
            }
        } else {
            return Optional.empty();
        }
    }

    private float expectedConfidence(Choices choices, Rule rule) {
        SpeechRecognition recognizer = getRecognizer(choices.locale);
        HearingAbility required = recognizer.implementation.required;
        float p = required.confidence(choices.intention);
        return Confidence.weighted( //
                PhraseString.words(rule.text).length, //
                p, //
                Confidence.slightlyReducedProbability(p));
    }

    private void handleRecognitionRejected(SpeechRecognizedEventArgs eventArgs) {
        active.updateAndGet(prompt -> {
            if (prompt == null) {
                return null;
            } else {
                try {
                    if (audioSignalProblems.exceedLimits()) {
                        logTooManyAudioSignalProblems();
                        reject(prompt, eventArgs);
                        return prompt;
                    } else {
                        if (hypothesis != null) {
                            // rejectedResult may contain better result than hypothesis
                            // TODO accept if hypothesis and recognitionRejected result have the same indices
                            float expectedConfidence = expectedConfidence(prompt.choices, hypothesis);
                            if (hypothesis.probability >= expectedConfidence) {
                                logger.info("Considering hypothesis");
                                return handle(prompt, this::singleResult, hypothesis, expectedConfidence);
                            } else {
                                reject(prompt, hypothesis, "Weighted hypothesis probability=" + hypothesis.probability
                                        + " too low - ignoring");
                                // Only if rejected by the speech recognition implementation
                                // - would result in too may events otherwise
                                signalHandlerInvocation(Notification.RecognitionRejected, eventArgs);
                                return prompt;
                            }
                        } else {
                            reject(prompt, eventArgs);
                            // Only if rejected by the speech recognition implementation - it's too may events otherwise
                            signalHandlerInvocation(Notification.RecognitionRejected, eventArgs);
                            return prompt;
                        }
                    }
                } catch (Exception e) {
                    prompt.setException(e);
                    getRecognizer(prompt.choices.locale).endRecognition();
                    return null;
                } finally {
                    clearDetectedSpeech();
                }
            }
        });
    }

    private void handleRecogntionCompleted(SpeechRecognizedEventArgs eventArgs) {
        active.updateAndGet(prompt -> {
            if (prompt == null) {
                return null;
            } else {
                try {
                    if (audioSignalProblems.exceedLimits()) {
                        logTooManyAudioSignalProblems();
                        return prompt;
                    } else {
                        if (prompt.acceptedResult == Result.Accept.Distinct) {
                            return handle(eventArgs, prompt, SpeechRecognitionInputMethod::bestSingleResult,
                                    this::singleResult);
                        } else if (prompt.acceptedResult == Result.Accept.Multiple) {
                            return handle(eventArgs, prompt, SpeechRecognitionInputMethod::bestMultipleChoices,
                                    this::multipleChoiceResults);
                        } else {
                            throw new UnsupportedOperationException(prompt.acceptedResult.toString());
                        }
                    }
                } catch (Exception e) {
                    prompt.setException(e);
                    getRecognizer(prompt.choices.locale).endRecognition();
                    return null;
                } finally {
                    clearDetectedSpeech();
                }
            }
        });
    }

    private Prompt.Result singleResult(Rule rule, IntUnaryOperator toChoices) {
        Set<Integer> choices = choices(rule, toChoices);
        if (choices.isEmpty()) {
            throw new NoSuchElementException("No choice indices: " + rule);
        } else if (choices.size() > 1) {
            throw new NoSuchElementException("No distinct choice indices: " + rule);
        }
        Integer distinctChoice = choices.iterator().next();
        return new Prompt.Result(distinctChoice);
    }

    private Prompt handle(SpeechRecognizedEventArgs eventArgs, Prompt prompt,
            BiFunction<List<Rule>, IntUnaryOperator, Optional<Rule>> matcher,
            BiFunction<Rule, IntUnaryOperator, Prompt.Result> resultor) {
        validate(eventArgs.result);

        IntUnaryOperator mapping = phraseToChoice(prompt);
        Optional<Rule> result = matcher.apply(eventArgs.result, mapping);
        if (result.isPresent()) {
            var rule = result.get();
            float expectedConfidence = expectedConfidence(prompt.choices, rule);
            boolean useHypothesis = hypothesis != null && //
                    hypothesis.probability > rule.probability && //
                    resultor.apply(hypothesis, mapping).equals(resultor.apply(rule, mapping));
            return handle(prompt, resultor, useHypothesis ? hypothesis : rule, expectedConfidence);
        } else {
            reject(prompt, eventArgs, "No matching choice");
            return prompt;
        }
    }

    private static void validate(List<Rule> candidates) {
        candidates.stream().forEach(Rule::isValid);
    }

    private Prompt handle(Prompt prompt, BiFunction<Rule, IntUnaryOperator, Prompt.Result> resultor, Rule rule,
            float expectedConfidence) {
        float audioProblemPenalty = audioSignalProblems.penalty() * AUDIO_PROBLEM_PENALTY_WEIGHT;

        if (rule.probability - audioProblemPenalty >= expectedConfidence) {
            var promptResult = resultor.apply(rule, phraseToChoice(prompt));
            if (promptResult.elements.isEmpty()) {
                reject(prompt, rule, "Empty result");
                return prompt;
            } else if (promptResult.valid(prompt.choices)) {
                events.recognitionCompleted.fire(new SpeechRecognizedEventArgs(rule));
                return accept(prompt, promptResult);
            } else {
                reject(prompt, rule, "Undefined result index");
                return prompt;
            }
        } else if (rule.probability >= expectedConfidence) {
            logAudioSignalProblemPenalty(expectedConfidence, audioProblemPenalty);
            reject(prompt, rule);
            return prompt;
        } else {
            logLackOfConfidence(rule.probability, expectedConfidence);
            reject(prompt, rule);
            return prompt;
        }
    }

    private void reject(Prompt prompt, Rule rule, String reason) {
        logger.info("{}: {}", reason, rule);
        reject(prompt, rule);
    }

    private void reject(Prompt prompt, SpeechRecognizedEventArgs eventArgs, String reason) {
        logger.info("{} in {}", reason, eventArgs);
        reject(prompt, eventArgs);
    }

    private void reject(Prompt prompt, Rule rule) {
        reject(prompt, new SpeechRecognizedEventArgs(rule));
    }

    private void reject(Prompt prompt, SpeechRecognizedEventArgs eventArgs) {
        Optional<Rule> bestSingleResult = bestSingleResult(eventArgs.result, phraseToChoice(prompt));
        if (bestSingleResult.isPresent()) {
            events.recognitionRejected.fire(new SpeechRecognizedEventArgs(bestSingleResult.get()));
        } else {
            events.recognitionRejected.fire(eventArgs);
        }
    }

    private IntUnaryOperator phraseToChoice(Prompt prompt) {
        return getRecognizer(prompt).preparedChoices().mapper();
    }

    private Prompt.Result multipleChoiceResults(Rule rule, @SuppressWarnings("unused") IntUnaryOperator toChoices) {
        RuleIndicesList choices = rule.indices();
        List<Integer> multipleChoices = choices.stream().filter(indices -> indices.size() == 1)
                .map(indices -> indices.iterator().next()).collect(toList());
        return new Prompt.Result(multipleChoices);
    }

    public static Optional<Rule> bestSingleResult(List<Rule> rules, IntUnaryOperator toChoices) {
        Optional<Rule> best = reduceToSingleChoice(rules, toChoices);
        if (best.isEmpty()) {
            List<Rule> withoutTrailingDisjunctNullRules = rules.stream().filter(Rule::hasTrailingNullRule)
                    .map(rule -> rule.withoutDisjunctTrailingNullRules(toChoices)).collect(Collectors.toList());
            best = reduceToSingleChoice(withoutTrailingDisjunctNullRules, toChoices);
        }
        return best;
    }

    private static Optional<Rule> reduceToSingleChoice(List<Rule> rules, IntUnaryOperator toChoices) {
        return rules.stream().filter(rule -> {
            Set<Integer> choices = choices(rule.indices(), toChoices);
            return choices.size() == 1;
        }).reduce(Rule::maxProbability);
    }

    public static Set<Integer> choices(Rule rule, IntUnaryOperator toChoices) {
        return choices(rule.indices(), toChoices);
    }

    public static Set<Integer> choices(RuleIndicesList indices, IntUnaryOperator toChoices) {
        return indices.intersection().stream().map(toChoices::applyAsInt).collect(Collectors.toSet());
    }

    public static Optional<Rule> bestMultipleChoices(List<Rule> rules,
            @SuppressWarnings("unused") IntUnaryOperator toChoices) {
        return rules.stream().reduce(Rule::maxProbability);
    }

    private SpeechRecognition getRecognizer(Prompt prompt) {
        return getRecognizer(prompt.choices.locale);
    }

    private SpeechRecognition getRecognizer(Locale locale) {
        SpeechRecognition recognizer = speechRecognizer.get(locale);
        if (usedRecognitionInstances.put(locale, recognizer) == null) {
            add(recognizer.events);
        }
        return recognizer;
    }

    private Prompt accept(Prompt prompt, Prompt.Result promptResult) {
        getRecognizer(prompt).endRecognition();
        signal(prompt, promptResult);
        return null;
    }

    private static void logLackOfConfidence(float confidence, float expectedConfidence) {
        logger.info("Lack of confidence {} < expected {})", confidence, expectedConfidence);
    }

    private void logTooManyAudioSignalProblems() {
        logger.info("Audio signal problems: {}", audioSignalProblems);
    }

    private static void logAudioSignalProblemPenalty(double confidence, double penalty) {
        logger.info("Audio signal problem penalty (required {} + penalty {} + = {})", confidence, penalty,
                confidence + penalty);
    }

    private void signal(Prompt prompt, Prompt.Result result) {
        prompt.lock.lock();
        try {
            prompt.signal(this, result);
        } finally {
            prompt.lock.unlock();
        }
    }

    private void signalHandlerInvocation(InputMethod.Notification eventType, SpeechRecognizedEventArgs eventArgs) {
        var prompt = active.get();
        if (prompt != null) {
            prompt.lock.lock();
            try {
                prompt.signal(new SpeechRecognitionInputMethodEventArgs(eventType, eventArgs));
            } finally {
                prompt.lock.unlock();
            }
        }
    }

    @Override
    public Setup getSetup(Choices choices) {
        SpeechRecognition recognizer = getRecognizer(choices.locale);
        PreparedChoices prepared = recognizer.prepare(choices);
        return () -> {
            this.activeRecognizer.set(recognizer);
            recognizer.apply(prepared);
        };
    }

    @Override
    public void show(Prompt prompt) {
        Objects.requireNonNull(prompt);

        active.updateAndGet(previousPrompt -> {
            if (previousPrompt != null) {
                throw new IllegalStateException("Trying to show prompt " + prompt + "when already showing another");
            }

            SpeechRecognition recognizer = getRecognizer(prompt);
            if (recognizer.isActive()) {
                // TODO triggered in the middle of the script
                // throw new IllegalStateException("Speech recognizer already active");
                logger.warn("Speech recognizer already active");
            }

            prompt.inputMethodInitializers.setup(this);
            recognizer.startRecognition();
            return prompt;
        });
    }

    public Prompt getActivePrompt() {
        return active.get();
    }

    @Override
    public void updateUI(UiEvent event) {
        active.updateAndGet(prompt -> {
            SpeechRecognition recognizer = activeRecognizer.get();
            if (event.enabled) {
                if (recognizer.isActive()) {
                    recognizer.resumeRecognition();
                } else {
                    recognizer.startRecognition();
                }
            } else {
                if (recognizer.isActive()) {
                    completeSpeechRecognition();
                    recognizer.pauseRecognition();
                }
            }
            return prompt;
        });
    }

    @Override
    public void dismiss(Prompt prompt) {
        Objects.requireNonNull(prompt);

        try {
            active.updateAndGet(activePrompt -> {
                if (activePrompt == null) {
                    return null;
                } else {
                    getRecognizer(activePrompt).endRecognition();
                }

                if (activePrompt != prompt) {
                    throw new IllegalStateException("Trying to dismiss wrong prompt: " + prompt);
                } else {
                    return null;
                }
            });
        } catch (Throwable t) {
            active.set(null);
            throw t;
        }
    }

    public static final class ResumeRecognition implements teaselib.core.Closeable {
        private final Runnable runnable;

        public ResumeRecognition(Runnable resumeRecognition) {
            this.runnable = resumeRecognition;
        }

        @Override
        public void close() {
            runnable.run();
        }

    }

    public ResumeRecognition pauseRecognition() {
        return new ResumeRecognition(speechRecognizer.pauseRecognition());
    }

    public void emulateRecogntion(String phrase) {
        var activePrompt = active.get();
        if (activePrompt == null) {
            throw new NoSuchElementException("Active prompt");
        } else {
            getRecognizer(activePrompt).emulateRecogntion(phrase);
        }
    }

    private void add(SpeechRecognitionEvents speechRecognitionEvents) {
        speechRecognitionEvents.recognitionStarted.add(speechRecognitionStartedEventHandler);
        speechRecognitionEvents.audioLevelUpdated.add(audioLevelUpdatedEventHandler);
        speechRecognitionEvents.audioSignalProblemOccured.add(audioSignalProblemEventHandler);
        speechRecognitionEvents.speechDetected.add(speechDetectedEventHandler);
        speechRecognitionEvents.recognitionRejected.add(recognitionRejected);
        speechRecognitionEvents.recognitionCompleted.add(recognitionCompleted);
    }

    private void remove(SpeechRecognitionEvents speechRecognitionEvents) {
        speechRecognitionEvents.recognitionStarted.remove(speechRecognitionStartedEventHandler);
        speechRecognitionEvents.audioLevelUpdated.remove(audioLevelUpdatedEventHandler);
        speechRecognitionEvents.audioSignalProblemOccured.remove(audioSignalProblemEventHandler);
        speechRecognitionEvents.speechDetected.remove(speechDetectedEventHandler);
        speechRecognitionEvents.recognitionRejected.remove(recognitionRejected);
        speechRecognitionEvents.recognitionCompleted.remove(recognitionCompleted);
    }

    @Override
    public String toString() {
        var prompt = active.get();
        if (prompt == null) {
            return "SpeechRecognizer <inactive>";
        } else {
            SpeechRecognition recognizer = getRecognizer(prompt.choices.locale);
            float expectedConfidence = recognizer.implementation.required.confidence(prompt.choices.intention);
            return "SpeechRecognizer=" + recognizer + " required confidence=" + expectedConfidence;
        }
    }

    @Override
    public void close() {
        usedRecognitionInstances.values().stream().map(recognizer -> recognizer.events).forEach(this::remove);
        speechRecognizer.close();
    }

    public void completeSpeechRecognition() {
        audioSync.completeSpeechRecognition();
    }

}
