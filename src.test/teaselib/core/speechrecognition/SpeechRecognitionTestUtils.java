package teaselib.core.speechrecognition;

import static java.util.Arrays.*;
import static org.junit.Assert.*;
import static teaselib.core.speechrecognition.SpeechRecognition.*;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import teaselib.core.AudioSync;
import teaselib.core.configuration.Configuration;
import teaselib.core.configuration.DebugSetup;
import teaselib.core.events.Event;
import teaselib.core.speechrecognition.events.SpeechRecognizedEventArgs;
import teaselib.core.speechrecognition.implementation.TeaseLibSRGS;
import teaselib.core.ui.Choice;
import teaselib.core.ui.Choices;
import teaselib.core.ui.InputMethods;
import teaselib.core.ui.Prompt;
import teaselib.core.ui.Prompt.Result;
import teaselib.core.ui.SpeechRecognitionInputMethod;

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

    public static SpeechRecognizer getRecognizer(Class<? extends SpeechRecognitionImplementation> srClass) {
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

    public static List<Rule> assertRecognizedAsHypothesis(Choices choices, String phrase, Prompt.Result expected)
            throws InterruptedException {
        return emulateSpeechRecognition(choices, phrase, expected);
    }

    public static void assertRecognizedAsHypothesis(SpeechRecognitionInputMethod inputMethod, Choices choices,
            String phrase, Prompt.Result expected) throws InterruptedException {
        emulateSpeechRecognition(inputMethod, choices, phrase, expected);
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
        return awaitResult(inputMethod, prompt, withoutPunctation(phrase), expected);
    }

    public static void assertRecognized(SpeechRecognitionInputMethod inputMethod, Choices choices)
            throws InterruptedException {
        for (Choice choice : choices) {
            Prompt prompt = new Prompt(choices, new InputMethods(inputMethod));
            String emulatedSpeech = choice.phrases.get(0);
            awaitResult(inputMethod, prompt, withoutPunctation(emulatedSpeech),
                    new Prompt.Result(choices.indexOf(choice)));
        }
    }

    public static void assertRejected(SpeechRecognitionInputMethod inputMethod, Choices choices, String... rejected)
            throws InterruptedException {
        for (String speech : rejected) {
            Prompt prompt = new Prompt(choices, new InputMethods(inputMethod));
            awaitResult(inputMethod, prompt, withoutPunctation(speech), null);
        }
    }

    public static List<Rule> awaitResult(SpeechRecognitionInputMethod inputMethod, Prompt prompt, String phrase,
            Prompt.Result expectedRules) throws InterruptedException {
        assertEquals("Phrase may not contain punctation: '" + phrase + "'", withoutPunctation(phrase), phrase);

        List<Rule> results = new ArrayList<>();

        Event<SpeechRecognizedEventArgs> completedHandler = eventArgs -> {
            results.addAll(asList(eventArgs.result));
            logger.info("Recognized '{}'", eventArgs.result[0].text);
        };
        inputMethod.events.recognitionCompleted.add(completedHandler);

        Event<SpeechRecognizedEventArgs> rejectedHandler = eventArgs -> {
            results.addAll(asList(eventArgs.result));
            Rule result = eventArgs.result[0];
            logger.info("Rejected '{}'", result.text.isBlank() ? result.name : result.text);
        };
        inputMethod.events.recognitionRejected.add(rejectedHandler);

        try {
            boolean dismissed;
            Result result;
            prompt.lock.lockInterruptibly();
            try {
                inputMethod.show(prompt);
                inputMethod.emulateRecogntion(phrase);
                dismissed = prompt.click.await(RECOGNITION_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS);

                if (!dismissed) {
                    inputMethod.dismiss(prompt);
                    result = prompt.result();
                    assertEquals("Rejected prompt expected" + prompt, Result.UNDEFINED, result);
                } else {
                    result = prompt.result();
                }
            } finally {
                prompt.lock.unlock();
            }

            if (expectedRules != null) {
                assertTrue("Expected recognition:: \"" + phrase + "\"", dismissed);

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
        awaitResult(inputMethod, prompt, withoutPunctation(phrase),
                new Prompt.Result(choices.toText().indexOf(phrase)));
    }

}
