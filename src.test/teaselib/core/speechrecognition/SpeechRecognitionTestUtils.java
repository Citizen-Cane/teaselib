package teaselib.core.speechrecognition;

import static java.util.Arrays.asList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static teaselib.core.speechrecognition.SpeechRecognition.withoutPunctation;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

    public static SpeechRecognizer getRecognizers() {
        return getRecognizers(TeaseLibSRGS.class);
    }

    public static SpeechRecognizer getRecognizers(Class<? extends SpeechRecognitionImplementation> srClass) {
        DebugSetup setup = new DebugSetup().withInput();
        Configuration config = new Configuration();
        setup.applyTo(config);
        config.set(SpeechRecognizer.Config.SpeechRecognitionImplementation, srClass.getName());
        return new SpeechRecognizer(config);
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

    public static void assertRecognizedAsHypothesis(SpeechRecognizer recognizers,
            SpeechRecognitionInputMethod inputMethod, Choices choices, String phrase, Prompt.Result expected)
            throws InterruptedException {
        emulateSpeechRecognition(recognizers, inputMethod, choices, phrase, expected);
    }

    static List<Rule> assertRejected(Choices choices, String phrase) throws InterruptedException {
        return emulateSpeechRecognition(choices, phrase, null);
    }

    public static List<Rule> emulateSpeechRecognition(Choices choices, String phrase, Prompt.Result expected)
            throws InterruptedException {
        try (SpeechRecognizer recognizers = getRecognizers();
                SpeechRecognitionInputMethod inputMethod = new SpeechRecognitionInputMethod(recognizers)) {
            return emulateSpeechRecognition(recognizers, inputMethod, choices, phrase, expected);
        }
    }

    private static List<Rule> emulateSpeechRecognition(SpeechRecognizer recognizers,
            SpeechRecognitionInputMethod inputMethod, Choices choices, String phrase, Prompt.Result expected)
            throws InterruptedException {
        Prompt prompt = new Prompt(choices, new InputMethods(inputMethod));
        SpeechRecognition sr = recognizers.get(choices.locale);
        return awaitResult(inputMethod, sr, prompt, withoutPunctation(phrase), expected);
    }

    public static void assertRecognized(SpeechRecognizer recognizers, SpeechRecognitionInputMethod inputMethod,
            Choices choices) throws InterruptedException {
        for (Choice choice : choices) {
            Prompt prompt = new Prompt(choices, new InputMethods(inputMethod));
            String emulatedSpeech = choice.phrases.get(0);
            awaitResult(inputMethod, recognizers.get(choices.locale), prompt, withoutPunctation(emulatedSpeech),
                    new Prompt.Result(choices.indexOf(choice)));
        }
    }

    public static void assertRejected(SpeechRecognizer recognizers, SpeechRecognitionInputMethod inputMethod,
            Choices choices, String... rejected) throws InterruptedException {
        for (String speech : rejected) {
            Prompt prompt = new Prompt(choices, new InputMethods(inputMethod));
            awaitResult(inputMethod, recognizers.get(choices.locale), prompt, withoutPunctation(speech), null);
        }
    }

    public static List<Rule> awaitResult(SpeechRecognitionInputMethod inputMethod, SpeechRecognition sr, Prompt prompt,
            String phrase, Prompt.Result expectedRules) throws InterruptedException {
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
            prompt.lock.lockInterruptibly();
            try {
                inputMethod.show(prompt);
                sr.emulateRecogntion(phrase);
                dismissed = prompt.click.await(RECOGNITION_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS);
            } finally {
                prompt.lock.unlock();
            }

            if (!dismissed) {
                inputMethod.dismiss(prompt);
                Result result = prompt.result();
                assertEquals("Rejected prompt expected" + prompt, Result.UNDEFINED, result);
            }

            Result result = prompt.result();

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

    static void emulateRecognition(SpeechRecognition sr, SpeechRecognitionInputMethod inputMethod, Choices choices,
            String phrase) throws InterruptedException {
        Prompt prompt = new Prompt(choices, new InputMethods(inputMethod));
        awaitResult(inputMethod, sr, prompt, withoutPunctation(phrase),
                new Prompt.Result(choices.toText().indexOf(phrase)));
    }

}
