package teaselib.core.speechrecognition;

import java.util.Locale;

import org.junit.Test;

import teaselib.core.ui.Choice;
import teaselib.core.ui.Choices;
import teaselib.core.ui.InputMethods;
import teaselib.core.ui.Intention;
import teaselib.core.ui.Prompt;
import teaselib.core.ui.SpeechRecognitionInputMethod;

public class SpeechRecognitionTests {
    private static Choice choice(String text) {
        return new Choice(text, text);
    }

    private static final Choices TestChoices = new Choices(Locale.US, Intention.Decide, //
            choice("I've spurted off, Miss"), choice("I give up, Miss"), choice("I have a dream"));

    @Test
    public void testSpeechRecognitionInputMethod() throws InterruptedException {
        try (SpeechRecognizer sR = SpeechRecognitionTestUtils.getRecognizer();
                SpeechRecognitionInputMethod inputMethod = new SpeechRecognitionInputMethod(sR);) {
            Prompt prompt = new Prompt(TestChoices, new InputMethods(inputMethod));
            prompt.lock.lockInterruptibly();
            try {
                inputMethod.show(prompt);
                prompt.click.await();
            } finally {
                prompt.lock.unlock();
            }
        }
    }
}
