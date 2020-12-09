package teaselib.core.speechrecognition.sapi;

import static java.util.stream.Collectors.*;
import static org.junit.Assert.*;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import teaselib.core.AudioSync;
import teaselib.core.configuration.Configuration;
import teaselib.core.configuration.DebugSetup;
import teaselib.core.events.Event;
import teaselib.core.speechrecognition.Confidence;
import teaselib.core.speechrecognition.Rule;
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

    private static final int RECOGNITION_TIMEOUT_MILLIS = 5000;
    static final Confidence confidence = Confidence.High;

    public static SpeechRecognizer getRecognizer() {
        return getRecognizer(TeaseLibSRGS.class);
    }

    public static SpeechRecognizer getRecognizer(Class<? extends SpeechRecognitionNativeImplementation> srClass) {
        DebugSetup setup = new DebugSetup().withInput();
        Configuration config = new Configuration();
        setup.applyTo(config);
        config.set(SpeechRecognizer.Config.SpeechRecognitionImplementation, srClass.getName());
        return new SpeechRecognizer(config, new AudioSync());
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
        try (SpeechRecognizer recognizer = getRecognizer();
                SpeechRecognitionInputMethod inputMethod = new SpeechRecognitionInputMethod(recognizer)) {
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
        try (SpeechRecognizer recognizer = getRecognizer();
                SpeechRecognitionInputMethod inputMethod = new SpeechRecognitionInputMethod(recognizer)) {
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
        boolean isAudioFile = Files.exists(Path.of(phrase));
        if (!isAudioFile) {
            assertEquals("Phrase may not contain punctation: '" + phrase + "'", withoutPunctation(phrase), phrase);
        }

        List<Rule> results = new ArrayList<>();

        Event<SpeechRecognizedEventArgs> completedHandler = eventArgs -> {
            results.addAll(eventArgs.result);
            logger.info("Recognized '{}'", eventArgs.result.get(0).text);
        };
        inputMethod.events.recognitionCompleted.add(completedHandler);

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
        };
        inputMethod.events.recognitionRejected.add(rejectedHandler);

        try {
            boolean dismissed;
            Result result;
            prompt.lock.lockInterruptibly();
            try {
                assertTrue(inputMethod.getActivePrompt() == null);
                inputMethod.show(prompt);
                inputMethod.emulateRecogntion(phrase);
                dismissed = prompt.click.await(RECOGNITION_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS);

                prompt.dismiss();
                assertTrue(inputMethod.getActivePrompt() == null);

                result = prompt.result();
                if (dismissed) {
                    assertNotEquals("Result expected" + prompt, Result.UNDEFINED, result);
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
            } else {
                assertFalse("Expected rejected: \"" + phrase + "\"", dismissed);
                assertEquals("Undefined result", Result.UNDEFINED, result);
            }
        } finally {
            inputMethod.events.recognitionRejected.remove(rejectedHandler);
            inputMethod.events.recognitionCompleted.remove(completedHandler);
        }

        return results;
    }

    private static void assertAllTheSameChoices(Prompt.Result expectedRules, Prompt.Result result) {
        List<Integer> choices = result.elements.stream().distinct().collect(Collectors.toList());
        assertTrue("Result contains different choices: " + result, choices.size() == 1);
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

}
