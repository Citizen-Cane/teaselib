package teaselib.core.speechrecognition.sapi;

import java.io.IOException;
import java.util.Locale;
import java.util.concurrent.CountDownLatch;

import org.junit.Test;

import teaselib.core.configuration.Configuration;
import teaselib.core.configuration.DebugSetup;
import teaselib.core.events.Event;
import teaselib.core.speechrecognition.SpeechRecognition;
import teaselib.core.speechrecognition.SpeechRecognitionInputMethod;
import teaselib.core.speechrecognition.SpeechRecognizer;
import teaselib.core.speechrecognition.events.SpeechRecognizedEventArgs;
import teaselib.core.ui.Choice;
import teaselib.core.ui.Choices;
import teaselib.core.ui.InputMethods;
import teaselib.core.ui.Intention;
import teaselib.core.ui.Prompt;

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

    @Test
    public void testPronunciationSAPIPronTagSpeechRecognitionHelloWorld() throws InterruptedException, IOException {
        try (SpeechRecognizer speechRecognizer = new SpeechRecognizer(
                new Configuration(new DebugSetup().withInput()))) {
            SpeechRecognition speechRecognition = speechRecognizer.get(Locale.US);
            CountDownLatch completed = new CountDownLatch(1);
            // "P DISP=\"replace\" PRON=\"H EH 1 L OW W ER 1 L D\"> replace </P>"
            Choices choices = new Choices(Locale.ENGLISH, Intention.Decide,
                    new Choice("<P>/Display/Word/H EH 1 L OW;</P>"));
            Event<SpeechRecognizedEventArgs> event = speechRecognition.events.recognitionCompleted
                    .add(events -> completed.countDown());
            try {
                speechRecognition.apply(speechRecognition.prepare(choices));
                speechRecognition.startRecognition();
                completed.await();
            } finally {
                speechRecognition.events.recognitionCompleted.remove(event);
            }
        }
    }

}
