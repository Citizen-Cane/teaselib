package teaselib.core.speechrecognition;

import static org.junit.Assert.*;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import org.junit.Test;

import teaselib.core.ResourceLoader;
import teaselib.core.speechrecognition.implementation.TeaseLibSR;
import teaselib.core.speechrecognition.implementation.TeaseLibSRGS;
import teaselib.core.ui.Choice;
import teaselib.core.ui.Choices;
import teaselib.core.ui.Prompt;
import teaselib.core.ui.SpeechRecognitionInputMethod;
import teaselib.core.util.Stream;

public class SpeechRecognitionTest {
    private static final Choices Foobar = new Choices(
            Arrays.asList(new Choice("My name is Foo"), new Choice("My name is Bar")));
    private static final Confidence confidence = Confidence.High;

    private static void emulateSpeechRecognition(String resource, String emulatedRecognitionResult,
            int expectedResultIndex) throws IOException, InterruptedException {
        ResourceLoader resources = new ResourceLoader(SpeechRecognitionTest.class);
        String xml = Stream.toString(resources.get(resource));
        SpeechRecognition sr = new SpeechRecognition(Locale.ENGLISH, TeaseLibSRGS.class) {
            @Override
            String srgs(List<String> choices) {
                return xml;
            }
        };

        SpeechRecognitionInputMethod inputMethod = new SpeechRecognitionInputMethod(sr, confidence, Optional.empty());
        Prompt prompt = new Prompt(Foobar, Arrays.asList(inputMethod));

        prompt.lock.lockInterruptibly();
        try {
            inputMethod.show(prompt);
            awaitResult(sr, prompt, emulatedRecognitionResult, expectedResultIndex);
        } finally {
            prompt.lock.unlock();
        }
    }

    private static void awaitResult(SpeechRecognition sr, Prompt prompt, String emulatedText, int expectedResultIndex)
            throws InterruptedException {
        sr.emulateRecogntion(emulatedText);
        boolean dismissed = prompt.click.await(10, TimeUnit.SECONDS);
        if (!dismissed) {
            prompt.dismiss();
        }
        assertTrue("Prompt timed out - emulated speech recognition failed", dismissed);
        assertEquals(expectedResultIndex, prompt.result());
    }

    @Test
    public void testSR() throws InterruptedException {
        SpeechRecognition sr = new SpeechRecognition(Locale.ENGLISH, TeaseLibSR.class);
        SpeechRecognitionInputMethod inputMethod = new SpeechRecognitionInputMethod(sr, confidence, Optional.empty());
        Prompt prompt = new Prompt(Foobar, Arrays.asList(inputMethod));

        prompt.lock.lockInterruptibly();
        try {
            inputMethod.show(prompt);
            awaitResult(sr, prompt, "My name is Bar", 1);
        } finally {
            prompt.lock.unlock();
        }
    }

    @Test
    public void testMicrosoftSRGSExampleCities() throws InterruptedException, IOException {
        String resource = "cities_srg.xml";
        String emulatedRecognitionResult = "I would like to fly from Miami to Los Angeles";
        emulateSpeechRecognition(resource, emulatedRecognitionResult, 3);
        // TODO Check second rule (to) == 1
    }

    @Test
    public void testHandcraftedSRGSExample() throws InterruptedException, IOException {
        String resource = "handcrafted_srg.xml";
        String emulatedRecognitionResult = "Please Miss two more";
        emulateSpeechRecognition(resource, emulatedRecognitionResult, 2);
    }

    @Test
    public void testHandcraftedSRGSExampleRecognitionRejected() throws InterruptedException, IOException {
        String resource = "handcrafted_srg.xml";
        String emulatedRecognitionResult = "Please Miss some more";
        try {
            emulateSpeechRecognition(resource, emulatedRecognitionResult, -1);
            // TODO Needs 10s to complete - way too long
            fail("Speech recognized with wrong phrase");
        } catch (AssertionError e) {
            return;
        }
    }

    @Test
    public void testSRGSBuilder() throws InterruptedException {
        SpeechRecognition sr = new SpeechRecognition(Locale.ENGLISH, TeaseLibSRGS.class);

        SpeechRecognitionInputMethod inputMethod = new SpeechRecognitionInputMethod(sr, confidence, Optional.empty());
        Choices choices = new Choices(Arrays.asList(new Choice("Please Miss, one more"),
                new Choice("Please Miss, one less"), new Choice("Please Miss, two more")));
        Prompt prompt = new Prompt(choices, Arrays.asList(inputMethod));

        prompt.lock.lockInterruptibly();
        try {
            inputMethod.show(prompt);
            awaitResult(sr, prompt, "Please Miss one less", 1);
        } finally {
            prompt.lock.unlock();
        }

    }

}
