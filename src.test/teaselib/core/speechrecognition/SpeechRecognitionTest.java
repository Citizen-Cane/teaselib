package teaselib.core.speechrecognition;

import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import org.junit.Test;

import teaselib.core.ResourceLoader;
import teaselib.core.speechrecognition.SpeechRecognitionResult.Confidence;
import teaselib.core.speechrecognition.implementation.TeaseLibSR;
import teaselib.core.speechrecognition.implementation.TeaseLibSRGS;
import teaselib.core.ui.Choice;
import teaselib.core.ui.Choices;
import teaselib.core.ui.Prompt;
import teaselib.core.ui.SpeechRecognitionInputMethod;
import teaselib.core.util.Stream;

public class SpeechRecognitionTest {
    private static final Choices Choices = new Choices(Arrays.asList(new Choice("Foobar")));
    private static final Confidence confidence = Confidence.High;

    @Test
    public void testSR() throws InterruptedException, IOException {
        SpeechRecognition sr = new SpeechRecognition(Locale.ENGLISH, TeaseLibSR.class);
        SpeechRecognitionInputMethod inputMethod = new SpeechRecognitionInputMethod(sr, confidence, Optional.empty());
        Prompt prompt = new Prompt(Choices, Arrays.asList(inputMethod));

        prompt.lock.lockInterruptibly();
        try {
            inputMethod.show(prompt);
            sr.emulateRecogntion("Foobar");
            assertTrue("Prompt timed out - emulated speech recognition failed",
                    prompt.click.await(10, TimeUnit.SECONDS));
        } finally {
            prompt.lock.unlock();
        }

    }

    @Test
    public void testSRGS() throws InterruptedException, IOException {
        ResourceLoader resources = new ResourceLoader(SpeechRecognitionTest.class);
        String cityTravel = Stream.toString(resources.get("cities_srg.xml"));
        SpeechRecognition sr = new SpeechRecognition(Locale.ENGLISH, TeaseLibSRGS.class) {
            @Override
            String srgs(List<String> choices) {
                return cityTravel;
            }
        };
        SpeechRecognitionInputMethod inputMethod = new SpeechRecognitionInputMethod(sr, confidence, Optional.empty());
        Prompt prompt = new Prompt(Choices, Arrays.asList(inputMethod));

        prompt.lock.lockInterruptibly();
        try {
            inputMethod.show(prompt);
            sr.emulateRecogntion("I would like to fly from Seattle to New York");
            assertTrue("Prompt timed out - emulated speech recognition failed",
                    prompt.click.await(10, TimeUnit.SECONDS));
        } finally {
            prompt.lock.unlock();
        }

    }

}
