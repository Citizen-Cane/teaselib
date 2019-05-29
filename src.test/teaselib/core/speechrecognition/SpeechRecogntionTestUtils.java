package teaselib.core.speechrecognition;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import teaselib.core.speechrecognition.implementation.TeaseLibSRGS;
import teaselib.core.speechrecognition.srgs.StringSequence;
import teaselib.core.ui.Choices;
import teaselib.core.ui.Prompt;
import teaselib.core.ui.SpeechRecognitionInputMethod;

/**
 * @author Citizen-Cane
 *
 */
public class SpeechRecogntionTestUtils {

    private static final int RECOGNITION_TIMEOUT = 1;
    static final Confidence confidence = Confidence.High;

    static void assertRecognized(Choices choices, String phrase, Prompt.Result expected) throws InterruptedException {
        emulateSpeechRecognition(choices, withoutPunctation(phrase), expected);
    }

    static void assertRejected(Choices choices, String phrase) throws InterruptedException {
        emulateSpeechRecognition(choices, withoutPunctation(phrase), null);
    }

    static void emulateSpeechRecognition(Choices choices, String phrase, Prompt.Result expected)
            throws InterruptedException {
        assertEquals("Emulated speech may not contain punctation: '" + phrase + "'", withoutPunctation(phrase), phrase);
        SpeechRecognition sr = new SpeechRecognition(Locale.ENGLISH, TeaseLibSRGS.class);
        SpeechRecognitionInputMethod inputMethod = new SpeechRecognitionInputMethod(sr, confidence, Optional.empty());
        Prompt prompt = new Prompt(choices, Arrays.asList(inputMethod));

        prompt.lock.lockInterruptibly();
        try {
            inputMethod.show(prompt);
            awaitResult(sr, prompt, phrase, expected);
        } finally {
            prompt.lock.unlock();
            sr.close();
        }
    }

    public static String withoutPunctation(String text) {
        return StringSequence.splitWords(text).stream().collect(Collectors.joining(" "));
    }

    static void awaitResult(SpeechRecognition sr, Prompt prompt, String emulatedSpeech, Prompt.Result expectedRules)
            throws InterruptedException {
        sr.emulateRecogntion(emulatedSpeech);
        boolean dismissed = prompt.click.await(RECOGNITION_TIMEOUT, TimeUnit.SECONDS);
        if (!dismissed) {
            prompt.dismiss();
        }
        if (expectedRules != null) {
            assertTrue("Expected recognition:: \"" + emulatedSpeech + "\"", dismissed);

            // assertEquals(expectedRules, prompt.result());
            assertAllTheSameChoices(expectedRules, prompt.result());
        } else {
            assertFalse("Expected rejected: \"" + emulatedSpeech + "\"", dismissed);
        }
    }

    // TODO Remove checking for any result when the phrase to rule parser is stable and optimized
    private static void assertAllTheSameChoices(Prompt.Result expectedRules, Prompt.Result result) {
        List<Integer> choices = result.elements.stream().distinct().collect(Collectors.toList());
        assertTrue("Result contains different choices: " + result, choices.size() == 1);
        assertEquals("Expected choice " + expectedRules, expectedRules.elements.get(0), choices.get(0));
    }

    static void emulateRecognition(SpeechRecognition sr, SpeechRecognitionInputMethod inputMethod, Choices choices,
            String phrase) throws InterruptedException {
        Prompt prompt = new Prompt(choices, Arrays.asList(inputMethod));
        prompt.lock.lockInterruptibly();
        try {
            inputMethod.show(prompt);
            awaitResult(sr, prompt, withoutPunctation(phrase), new Prompt.Result(choices.toText().indexOf(phrase)));
        } finally {
            prompt.lock.unlock();
        }
    }

}
