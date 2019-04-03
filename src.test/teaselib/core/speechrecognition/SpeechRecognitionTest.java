package teaselib.core.speechrecognition;

import static org.junit.Assert.*;

import java.util.Arrays;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.junit.Test;

import teaselib.core.speechrecognition.implementation.TeaseLibSR;
import teaselib.core.speechrecognition.implementation.TeaseLibSRGS;
import teaselib.core.speechrecognition.srgs.SequenceUtil;
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

    private static void assertRecognized(Choices choices, String emulatedRecognitionResult, Prompt.Result expectedRules)
            throws InterruptedException {
        emulateSpeechRecognition(choices, emulatedRecognitionResult, expectedRules);
    }

    private static void assertRejected(Choices choices, String emulatedRecognitionResult) throws InterruptedException {
        emulateSpeechRecognition(choices, emulatedRecognitionResult, null);
    }

    private static void emulateSpeechRecognition(Choices choices, String emulatedRecognitionResult,
            Prompt.Result expectedRules) throws InterruptedException {
        assertEquals("Emulated speech may not contain punctation: '" + emulatedRecognitionResult + "'",
                withoutPunctation(emulatedRecognitionResult), emulatedRecognitionResult);
        SpeechRecognition sr = new SpeechRecognition(Locale.ENGLISH, TeaseLibSRGS.class);
        SpeechRecognitionInputMethod inputMethod = new SpeechRecognitionInputMethod(sr, confidence, Optional.empty());
        Prompt prompt = new Prompt(choices, Arrays.asList(inputMethod));

        prompt.lock.lockInterruptibly();
        try {
            inputMethod.show(prompt);
            awaitResult(sr, prompt, emulatedRecognitionResult, expectedRules);
        } finally {
            prompt.lock.unlock();
            sr.close();
        }
    }

    private static String withoutPunctation(String text) {
        return Arrays.stream(SequenceUtil.splitWords(text)).collect(Collectors.joining(" "));
    }

    private static void awaitResult(SpeechRecognition sr, Prompt prompt, String emulatedText, Prompt.Result expectedRules)
            throws InterruptedException {
        sr.emulateRecogntion(emulatedText);
        boolean dismissed = prompt.click.await(3, TimeUnit.SECONDS);
        if (!dismissed) {
            prompt.dismiss();
        }
        if (expectedRules != null) {
            assertTrue("Expected recognition:: \"" + emulatedText + "\"", dismissed);
            assertEquals(expectedRules, prompt.result());
        } else {
            assertFalse("Expected rejected: \"" + emulatedText + "\"", dismissed);
        }
    }

    @Test
    public void testSimpleSR() throws InterruptedException {
        Choices foobar = new Choices(Arrays.asList(new Choice("My name is Foo"), new Choice("My name is Bar"),
                new Choice("My name is Foobar")));

        SpeechRecognition sr = new SpeechRecognition(Locale.ENGLISH, TeaseLibSR.class);
        SpeechRecognitionInputMethod inputMethod = new SpeechRecognitionInputMethod(sr, confidence, Optional.empty());
        Prompt prompt = new Prompt(foobar, Arrays.asList(inputMethod));

        prompt.lock.lockInterruptibly();
        try {
            inputMethod.show(prompt);
            awaitResult(sr, prompt, "My name is Bar", new Prompt.Result(1));
        } finally {
            prompt.lock.unlock();
        }
    }

    @Test
    public void testSimpleSR2() throws InterruptedException {
        Choices foobar = new Choices(Arrays.asList(new Choice("My name is Foo, Mam"), new Choice("My name is Bar, Mam"),
                new Choice("My name is Foobar, Mam")));

        SpeechRecognition sr = new SpeechRecognition(Locale.ENGLISH, TeaseLibSR.class);
        SpeechRecognitionInputMethod inputMethod = new SpeechRecognitionInputMethod(sr, confidence, Optional.empty());
        Prompt prompt = new Prompt(foobar, Arrays.asList(inputMethod));

        prompt.lock.lockInterruptibly();
        try {
            inputMethod.show(prompt);
            awaitResult(sr, prompt, "My name is Bar Mam", new Prompt.Result(1));
        } finally {
            prompt.lock.unlock();
        }
    }

    @Test
    public void testSimpleSRirregularPhrases() throws InterruptedException {
        String sorry = "No Miss, I'm sorry";
        String ready = "Yes Miss, I'm ready";
        String haveIt = "I have it, Miss";
        String ready2 = "Yes,it's ready, Miss";
        String ready3 = "It's ready, Miss";

        Choices choices = new Choices(Arrays.asList(new Choice(sorry), new Choice(ready), new Choice(haveIt),
                new Choice(ready2), new Choice(ready3)));

        SpeechRecognition sr = new SpeechRecognition(Locale.ENGLISH, TeaseLibSR.class);
        SpeechRecognitionInputMethod inputMethod = new SpeechRecognitionInputMethod(sr, confidence, Optional.empty());

        emulateRecognition(sr, inputMethod, choices, sorry);
        emulateRecognition(sr, inputMethod, choices, ready);
        emulateRecognition(sr, inputMethod, choices, haveIt);
        emulateRecognition(sr, inputMethod, choices, ready2);
        emulateRecognition(sr, inputMethod, choices, ready3);
    }

    private void emulateRecognition(SpeechRecognition sr, SpeechRecognitionInputMethod inputMethod, Choices choices,
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

    @Test
    public void testSRGSBuilderCommonMiddle() throws InterruptedException {
        Choices choices = new Choices(new Choice("Yes Miss, I've spurted off"),
                new Choice("No Miss, I didn't spurt off"));

        assertRecognized(choices, "Yes Miss I've spurted off", new Prompt.Result(0, 0));
        assertRecognized(choices, "No Miss I didn't spurt off", new Prompt.Result(1, 1));
        assertRejected(choices, "Yes Miss I didn't spurt off");
        assertRejected(choices, "No Miss I've spurted off");
        assertRejected(choices, "Miss I've spurted off");
        assertRejected(choices, "Miss I didn't spurt off");
    }

    @Test
    public void testSRGSBuilderCommonStartEnd() throws InterruptedException {
        Choices choices = new Choices(new Choice("Dear Mistress, I've spurted my load, Miss"),
                new Choice("Dear Mistress I didn't spurt off, Miss"));

        assertRecognized(choices, "Dear Mistress I've spurted my load Miss", new Prompt.Result(0));
        assertRecognized(choices, "Dear Mistress I didn't spurt off Miss", new Prompt.Result(1));
        assertRejected(choices, "I didn't spurt my load Miss");
        assertRejected(choices, "Dear Mistress I didn't spurt off");
    }

    @Test
    public void testSRGSBuilderMultiplePhrases() throws InterruptedException {
        String[] yes = { //
                "Yes Miss, of course", //
                "Yes, of course, Miss", //
                "Yes, of course", //
                "of course" };
        String[] no = { //
                "No Miss, of course not", //
                "No, of course not, Miss", //
                "No, of course not", //
                "of course not" };
        Choices choices = new Choices(new Choice("Yes #title, of course", "Yes Miss, of course", yes),
                new Choice("No #title, of course not", "No Miss, of course not", no));
        for (String phrase : yes) {
            assertRecognized(choices, String.join(" ", SequenceUtil.splitWords(phrase)), new Prompt.Result(0, 0));
        }

        for (String phrase : no) {
            assertRecognized(choices, String.join(" ", SequenceUtil.splitWords(phrase)), new Prompt.Result(1, 1));
        }

        assertRejected(choices, "Of not");
        assertRejected(choices, "Yes Miss");

        assertRejected(choices, "Of not");
        assertRejected(choices, "No Miss");
    }

}
