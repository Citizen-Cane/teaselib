package teaselib.core.speechrecognition;

import java.util.Arrays;
import java.util.Locale;
import java.util.Optional;

import org.junit.Before;
import org.junit.Test;

import teaselib.core.speechrecognition.SpeechRecognitionResult.Confidence;
import teaselib.core.ui.Choices;
import teaselib.core.ui.Prompt;
import teaselib.core.ui.SpeechRecognitionInputMethod;

public class SpeechRecognitionTests {
    private static final String[] choices = { "I've spurted off, Miss", "I give up, Miss", "I have a dream" };
    private final Confidence confidence = Confidence.High;

    private boolean enableSpeechHypothesisHandler = false;

    @Before
    public void init() {
        if (enableSpeechHypothesisHandler) {
            System.setProperty(SpeechRecognition.EnableSpeechHypothesisHandlerGlobally, Boolean.toString(true));
        }
    }

    @Test
    public void testSpeechRecognitionInputMethod() throws InterruptedException {
        SpeechRecognition sr = new SpeechRecognition(new Locale("en-us"));

        SpeechRecognitionInputMethod inputMethod = new SpeechRecognitionInputMethod(sr, confidence, Optional.empty());
        Prompt prompt = new Prompt(new Choices(Arrays.asList(choices)), new Choices(Arrays.asList(choices)), null,
                Arrays.asList(inputMethod));

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
