package teaselib.core.ui;

import java.io.IOException;
import java.util.Arrays;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import org.junit.Test;

import teaselib.core.configuration.Configuration;
import teaselib.core.configuration.DebugSetup;
import teaselib.core.speechrecognition.SpeechRecognition;
import teaselib.core.speechrecognition.SpeechRecognitionResult.Confidence;
import teaselib.core.speechrecognition.SpeechRecognizer;

public class SpeechRecognitionInputTest {
    private static Choice choice(String text) {
        return new Choice(text, text);
    }

    private static final Choices Choices = new Choices(
            Arrays.asList(choice("I've spurted off, Miss"), choice("I give up, Miss"), choice("I have a dream")));
    private static final Confidence confidence = Confidence.High;

    @Test
    public void testSpeechRecognitionInputMethod() throws InterruptedException, IOException {
        DebugSetup setup = new DebugSetup().withInput();
        Configuration config = new Configuration(setup);
        SpeechRecognition sr = new SpeechRecognizer(config).get(new Locale("en-us"));

        SpeechRecognitionInputMethod inputMethod = new SpeechRecognitionInputMethod(sr, confidence, Optional.empty());
        Prompt prompt = new Prompt(Choices, Arrays.asList(inputMethod));

        prompt.lock.lockInterruptibly();
        try {
            inputMethod.show(prompt);
            sr.emulateRecogntion("I have a dream");
            prompt.click.await(10, TimeUnit.SECONDS);
            inputMethod.dismiss(prompt);
        } finally {
            prompt.lock.unlock();
        }
    }
}
