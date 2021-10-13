package teaselib.core.speechrecognition.sapi;

import static java.util.stream.Collectors.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static teaselib.core.util.ExceptionUtil.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import teaselib.core.AudioSync;
import teaselib.core.ai.deepspeech.DeepSpeechRecognizer;
import teaselib.core.configuration.Configuration;
import teaselib.core.configuration.DebugSetup;
import teaselib.core.events.Event;
import teaselib.core.speechrecognition.Rule;
import teaselib.core.speechrecognition.SpeechRecognitionEvents;
import teaselib.core.speechrecognition.SpeechRecognitionInputMethod;
import teaselib.core.speechrecognition.SpeechRecognitionNativeImplementation;
import teaselib.core.speechrecognition.SpeechRecognizer;
import teaselib.core.speechrecognition.events.SpeechRecognizedEventArgs;
import teaselib.core.speechrecognition.srgs.PhraseString;
import teaselib.core.ui.Choice;
import teaselib.core.ui.Choices;
import teaselib.core.ui.InputMethods;
import teaselib.core.ui.Intention;
import teaselib.core.ui.Prompt;
import teaselib.core.ui.Prompt.Result;

/**
 * @author Citizen-Cane
 *
 */
public class SpeechRecognitionTestUtils {
    private static final Logger logger = LoggerFactory.getLogger(SpeechRecognitionTestUtils.class);

    public static final int RECOGNITION_TIMEOUT_MILLIS = 5000;

    public static SpeechRecognizer getRecognizer(Class<? extends SpeechRecognitionNativeImplementation> srClass) {
        DebugSetup setup = new DebugSetup().withInput();
        Configuration config = new Configuration();
        setup.applyTo(config);
        config.set(SpeechRecognizer.Config.Default, srClass.getName());
        return new SpeechRecognizer(config, new AudioSync());
    }

    public static SpeechRecognitionInputMethod getInputMethod() {
        return getInputMethod(TestableTeaseLibSR.class);
    }

    public static SpeechRecognitionInputMethod getInputMethod(
            Class<? extends SpeechRecognitionNativeImplementation> srClass) {
        Configuration config = getConfig(srClass);
        return new SpeechRecognitionInputMethod(config, new AudioSync());
    }

    public static Configuration getConfig(Class<? extends SpeechRecognitionNativeImplementation> srClass) {
        DebugSetup setup = new DebugSetup().withInput();
        Configuration config = new Configuration();
        setup.applyTo(config);
        config.set(SpeechRecognizer.Config.Default, srClass.getName());
        return config;
    }

    private SpeechRecognitionTestUtils() {
    }

    public static List<Rule> assertRecognized(Choices choices, String phrase, Prompt.Result expected)
            throws InterruptedException {
        return emulateSpeechRecognition(choices, phrase, expected);
    }

    public static List<Rule> assertRecognized(SpeechRecognitionInputMethod inputMethod, Choices choices, String phrase,
            Prompt.Result expected) throws InterruptedException {
        return emulateSpeechRecognition(inputMethod, choices, phrase, expected);
    }

    public static List<Rule> assertRecognizedAsHypothesis(Choices choices, String phrase, Prompt.Result expected)
            throws InterruptedException {
        try (SpeechRecognitionInputMethod inputMethod = getInputMethod()) {
            return assertRecognizedAsHypothesis(inputMethod, choices, phrase, expected);
        }
    }

    public static List<Rule> assertRecognizedAsHypothesis(SpeechRecognitionInputMethod inputMethod, Choices choices,
            String phrase, Prompt.Result expected) throws InterruptedException {
        return emulateSpeechRecognition(inputMethod, choices, phrase, expected);
    }

    public static Choices as(Choices choices, Intention intention) {
        return new Choices(choices.locale, intention, choices);
    }

    static List<Rule> assertRejected(Choices choices, String phrase) throws InterruptedException {
        return emulateSpeechRecognition(choices, phrase, null);
    }

    public static List<Rule> emulateSpeechRecognition(Choices choices, String phrase, Prompt.Result expected)
            throws InterruptedException {
        try (SpeechRecognitionInputMethod inputMethod = getInputMethod()) {
            return emulateSpeechRecognition(inputMethod, choices, phrase, expected);
        }
    }

    private static List<Rule> emulateSpeechRecognition(SpeechRecognitionInputMethod inputMethod, Choices choices,
            String phrase, Prompt.Result expected) throws InterruptedException {
        Prompt prompt = new Prompt(choices, new InputMethods(inputMethod));
        return awaitResult(prompt, inputMethod, phrase, expected);
    }

    public static void assertRecognized(SpeechRecognitionInputMethod inputMethod, Choices choices)
            throws InterruptedException {
        for (Choice choice : choices) {
            Prompt prompt = new Prompt(choices, new InputMethods(inputMethod));
            String emulatedSpeech = choice.phrases.get(0);
            awaitResult(prompt, inputMethod, withoutPunctation(emulatedSpeech),
                    new Prompt.Result(choices.indexOf(choice)));
        }
    }

    public static void assertRejected(SpeechRecognitionInputMethod inputMethod, Choices choices, String... rejected)
            throws InterruptedException {
        for (String speech : rejected) {
            Prompt prompt = new Prompt(choices, new InputMethods(inputMethod));
            awaitResult(prompt, inputMethod, withoutPunctation(speech), null);
        }
    }

    public static List<Rule> awaitResult(Prompt prompt, SpeechRecognitionInputMethod inputMethod, String phrase,
            Prompt.Result expectedRules) throws InterruptedException {
        boolean isAudioFile = phrase.toLowerCase().endsWith(".raw");
        if (!isAudioFile) {
            assertEquals("Phrase may not contain punctation: '" + phrase + "'", withoutPunctation(phrase), phrase);
        }

        List<Rule> results = new ArrayList<>();

        var speechDetected = new AtomicInteger(0);
        Event<SpeechRecognizedEventArgs> detectedHandler = eventArgs -> {
            logger.info("Speech detected '{}'", eventArgs.result.get(0).text);
            speechDetected.incrementAndGet();
        };
        inputMethod.events.speechDetected.add(detectedHandler);

        Event<SpeechRecognizedEventArgs> completedHandler = eventArgs -> {
            results.addAll(eventArgs.result);
            logger.info("Recognized '{}'", eventArgs.result.get(0).text);
        };
        inputMethod.events.recognitionCompleted.add(completedHandler);

        var speechRejected = new AtomicBoolean(false);
        Event<SpeechRecognizedEventArgs> rejectedHandler = eventArgs -> {
            results.addAll(eventArgs.result);
            Optional<Rule> result = eventArgs.result.isEmpty() ? Optional.empty()
                    : Optional.of(eventArgs.result.get(0));
            if (result.isPresent()) {
                Rule rule = result.get();
                logger.info("Rejected '{}'", rule.text.isBlank() ? rule.name : rule.text);
            } else {
                logger.info("Rejected without result");
            }
            prompt.lock.tryLock();
            try {
                prompt.click.signalAll();
            } finally {
                prompt.lock.unlock();
            }
        };
        speechRejected.set(true);
        inputMethod.events.recognitionRejected.add(rejectedHandler);

        try {
            boolean dismissed;
            Result result;
            prompt.lock.lockInterruptibly();
            try {
                assertNull(inputMethod.getActivePrompt());
                prompt.show();
                inputMethod.emulateRecogntion(phrase);

                int speechDetectedCount;
                do {
                    speechDetectedCount = speechDetected.get();
                    dismissed = prompt.click.await(RECOGNITION_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS);
                } while (!dismissed && speechDetectedCount < speechDetected.get());

                prompt.dismiss();
                assertNull(inputMethod.getActivePrompt());

                result = prompt.result();
                if (dismissed) {
                    if (expectedRules != null) {
                        assertNotEquals("Result expected" + prompt, Result.UNDEFINED, result);
                    } else {
                        assertEquals("Result unexpected" + prompt, Result.UNDEFINED, result);
                    }
                } else {
                    assertEquals("Rejected prompt expected" + prompt, Result.UNDEFINED, result);
                }
            } finally {
                prompt.lock.unlock();
            }

            if (expectedRules != null) {
                if (isAudioFile) {
                    String expected = prompt.choices.get(expectedRules.elements.get(0)).phrases.get(0);
                    assertTrue("Expected recognition:: \"" + expected + "\"", dismissed);
                } else {
                    assertTrue("Expected recognition:: \"" + phrase + "\"", dismissed);
                }

                if (prompt.acceptedResult == Prompt.Result.Accept.Multiple) {
                    assertEquals(expectedRules, result);
                } else {
                    assertAllTheSameChoices(expectedRules, result);
                }
            } else if (speechRejected.get()) {
                assertTrue("Expected rejected and dismissed: \"" + phrase + "\" but got " + result, dismissed);
            } else {
                assertFalse("Expected rejected: \"" + phrase + "\" but got " + result, dismissed);
                assertNotEquals("Undefined result", Result.UNDEFINED, result);
            }
        } finally

        {
            inputMethod.events.recognitionRejected.remove(rejectedHandler);
            inputMethod.events.recognitionCompleted.remove(completedHandler);
            inputMethod.events.speechDetected.remove(detectedHandler);
        }

        return results;
    }

    private static void assertAllTheSameChoices(Prompt.Result expectedRules, Prompt.Result result) {
        List<Integer> choices = result.elements.stream().distinct().collect(toList());
        assertEquals("Result contains different choices: " + result, 1, choices.size());
        assertEquals("Expected choice " + expectedRules, expectedRules.elements.get(0), choices.get(0));
    }

    static void emulateRecognition(SpeechRecognitionInputMethod inputMethod, Choices choices, String phrase)
            throws InterruptedException {
        Prompt prompt = new Prompt(choices, new InputMethods(inputMethod));
        awaitResult(prompt, inputMethod, withoutPunctation(phrase),
                new Prompt.Result(choices.toText().indexOf(phrase)));
    }

    static String withoutPunctation(String text) {
        return Arrays.stream(PhraseString.words(text)).collect(joining(" "));
    }

    public static Rule await(int choice, DeepSpeechRecognizer deepSpeechRecognizer, SpeechRecognitionEvents events)
            throws InterruptedException {

        var speechDetected = new AtomicInteger(0);
        Event<SpeechRecognizedEventArgs> detectedHandler = eventArgs -> {
            logger.info("Speech detected '{}'", eventArgs.result.get(0).text);
            speechDetected.incrementAndGet();
        };
        events.speechDetected.add(detectedHandler);

        AtomicReference<SpeechRecognizedEventArgs> speechRecognized = new AtomicReference<>();
        CountDownLatch signal = new CountDownLatch(1);
        Event<SpeechRecognizedEventArgs> speechCompleted = e -> {
            speechRecognized.set(e);
            signal.countDown();
        };
        events.recognitionCompleted.add(speechCompleted);

        var recognitionRejected = new AtomicBoolean(false);
        Event<SpeechRecognizedEventArgs> speechRejected = e -> {
            recognitionRejected.set(true);
            signal.countDown();
        };
        events.recognitionRejected.add(speechRejected);

        try {
            int speechDetectedCount;
            boolean dismissed = false;
            do {
                speechDetectedCount = speechDetected.get();
                dismissed = signal.await(RECOGNITION_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS);
            } while (!dismissed && speechDetectedCount < speechDetected.get());

            if (recognitionRejected.get()) {
                assertNull("Speech rejected but got result: " + speechRecognized.get(), speechRecognized.get());
                return Rule.Nothing;
            } else if (dismissed) {
                assertNotNull(speechRecognized.get());
                List<Rule> rules = speechRecognized.get().result;
                assertFalse("Audio file result expected - path correct?", rules.isEmpty());
                Rule rule = rules.get(0);
                assertTrue(rule.isValid());
                assertEquals("Distinct rule", 1, rule.indices.size());
                assertEquals(choice, rule.indices.iterator().next().intValue());
                return rule;
            } else {
                Optional<Throwable> failure = deepSpeechRecognizer.getException();
                if (failure.isPresent()) {
                    throw asRuntimeException(failure.get());
                } else {
                    throw new AssertionError("Speech detection timed out");
                }
            }
        } finally {
            events.speechDetected.remove(detectedHandler);
            events.recognitionCompleted.remove(speechCompleted);
            events.recognitionRejected.remove(speechRejected);
        }
    }

    public static void assertConfidence(SpeechRecognitionNativeImplementation recognizer, Rule rule,
            Intention intention) {
        assertConfidence(rule, recognizer.required.confidence(intention));
    }

    public static void assertConfidence(Rule rule, float confidence) {
        assertTrue("confidence=" + rule.probability + " too low for required confidence=" + confidence + " in rule "
                + rule, rule.probability > confidence);
    }

}
