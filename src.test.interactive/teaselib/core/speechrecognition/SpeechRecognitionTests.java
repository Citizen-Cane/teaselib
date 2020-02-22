package teaselib.core.speechrecognition;

import java.util.Arrays;
import java.util.Locale;
import java.util.Optional;

import org.junit.Test;

import teaselib.core.ui.Choice;
import teaselib.core.ui.Choices;
import teaselib.core.ui.Prompt;
import teaselib.core.ui.SpeechRecognitionInputMethod;

public class SpeechRecognitionTests {
    private static Choice choice(String text) {
        return new Choice(text, text);
    }

    private static final Choices TestChoices = new Choices(
            Arrays.asList(choice("I've spurted off, Miss"), choice("I give up, Miss"), choice("I have a dream")));
    private static final Confidence TestConfidence = Confidence.High;

    @Test
    public void testSpeechRecognitionInputMethod() throws InterruptedException {
        SpeechRecognition sr = new SpeechRecognition(new Locale("en-us"));

        SpeechRecognitionInputMethod inputMethod = new SpeechRecognitionInputMethod(sr, TestConfidence,
                Optional.empty());
        Prompt prompt = new Prompt(TestChoices, Arrays.asList(inputMethod));

        prompt.lock.lockInterruptibly();
        try {
            inputMethod.show(prompt);
            prompt.click.await();
            inputMethod.dismiss(prompt);
        } finally {
            prompt.lock.unlock();
        }
    }
}
