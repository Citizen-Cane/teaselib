package teaselib.core.ui;

import java.util.Arrays;
import java.util.Locale;
import java.util.Optional;

import org.junit.Test;

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
    private final Confidence confidence = Confidence.High;

    @Test
    public void testSpeechRecognitionInputMethod() throws InterruptedException {
        SpeechRecognition sr = new SpeechRecognizer(DebugSetup.getConfiguration()).get(new Locale("en-us"));

        SpeechRecognitionInputMethod inputMethod = new SpeechRecognitionInputMethod(sr, confidence, Optional.empty());
        Prompt prompt = new Prompt(Choices, null, Arrays.asList(inputMethod));

        prompt.lock.lockInterruptibly();
        try {
            inputMethod.show(prompt);
            sr.emulateRecogntion("I have a dream");
            prompt.click.await();
            inputMethod.dismiss(prompt);
        } finally {
            prompt.lock.unlock();
        }
    }
}
