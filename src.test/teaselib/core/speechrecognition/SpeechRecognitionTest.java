package teaselib.core.speechrecognition;

import static org.junit.Assert.*;

import java.util.Arrays;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import org.junit.Test;

import teaselib.core.speechrecognition.implementation.TeaseLibSRGS;
import teaselib.core.ui.Choice;
import teaselib.core.ui.Choices;
import teaselib.core.ui.Prompt;
import teaselib.core.ui.SpeechRecognitionInputMethod;

/**
 * @author Citizen-Cane
 *
 */
public class SpeechRecognitionTest {

    private static final Confidence confidence = Confidence.High;

    private static void assertRecognized(Choices choices, String emulatedRecognitionResult, Prompt.Result expected)
            throws InterruptedException {
        emulateSpeechRecognition(choices, emulatedRecognitionResult, expected);
    }

    private static void assertRejected(Choices choices, String emulatedRecognitionResult) throws InterruptedException {
        emulateSpeechRecognition(choices, emulatedRecognitionResult, null);
    }

    private static void emulateSpeechRecognition(Choices choices, String emulatedRecognitionResult,
            Prompt.Result expected) throws InterruptedException {
        SpeechRecognition sr = new SpeechRecognition(Locale.ENGLISH, TeaseLibSRGS.class);
        SpeechRecognitionInputMethod inputMethod = new SpeechRecognitionInputMethod(sr, confidence, Optional.empty());
        Prompt prompt = new Prompt(choices, Arrays.asList(inputMethod));

        prompt.lock.lockInterruptibly();
        try {
            inputMethod.show(prompt);
            awaitResult(sr, prompt, emulatedRecognitionResult, expected);
        } finally {
            prompt.lock.unlock();
            sr.close();
        }
    }

    private static void awaitResult(SpeechRecognition sr, Prompt prompt, String emulatedText, Prompt.Result expected)
            throws InterruptedException {
        sr.emulateRecogntion(emulatedText);
        boolean dismissed = prompt.click.await(5, TimeUnit.SECONDS);
        if (!dismissed) {
            prompt.dismiss();
        }
        if (expected != null) {
            assertTrue("Expected recognition:: \"" + emulatedText + "\"", dismissed);
            assertEquals(expected, prompt.result());
        } else {
            assertFalse("Expected rejected: \"" + emulatedText + "\"", dismissed);
        }
    }

    @Test
    public void testSRGSBuilderCommonStart() throws InterruptedException {
        Choices choices = new Choices(new Choice("Please Miss, one more"), new Choice("Please Miss, one less"),
                new Choice("Please Miss, two more"));

        assertRecognized(choices, "Please Miss one more", new Prompt.Result(0));
        assertRecognized(choices, "Please Miss one less", new Prompt.Result(1));
        assertRecognized(choices, "Please Miss two more", new Prompt.Result(2));
        assertRejected(choices, "Please Miss three more May I");
        assertRejected(choices, "Please Miss one");
    }

    @Test
    public void testSRGSBuilderCommonEnd() throws InterruptedException {
        Choices choices = new Choices(new Choice("I've spurted my load, Dear Mistress"),
                new Choice("I didn't spurt off, Dear Mistress"));

        assertRecognized(choices, "I've spurted my load Dear Mistress", new Prompt.Result(0));
        assertRecognized(choices, "I didn't spurt off Dear Mistress", new Prompt.Result(1));
        assertRejected(choices, "I didn't spurt my load Dear Mistress");
        assertRejected(choices, "I didn't spurt off Dear");
    }

}
