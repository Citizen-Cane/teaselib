package teaselib.core.speechrecognition;

import static teaselib.core.speechrecognition.SpeechRecogntionTestUtils.*;

import java.util.Arrays;
import java.util.Locale;
import java.util.Optional;

import org.junit.Test;

import teaselib.core.speechrecognition.implementation.TeaseLibSR;
import teaselib.core.ui.Choice;
import teaselib.core.ui.Choices;
import teaselib.core.ui.Prompt;
import teaselib.core.ui.SpeechRecognitionInputMethod;

/**
 * @author Citizen-Cane
 *
 */
public class SpeechRecognitionSimpleTest {

    @Test
    public void testResourceHandling() {
        SpeechRecognition sr = new SpeechRecognition(Locale.ENGLISH, TeaseLibSR.class);
        sr.close();
    }

    @Test
    public void testSimpleSR() throws InterruptedException {
        Choices choices = new Choices(Arrays.asList(new Choice("My name is Foo"), new Choice("My name is Bar"),
                new Choice("My name is Foobar")));

        SpeechRecognition sr = new SpeechRecognition(Locale.ENGLISH, TeaseLibSR.class);

        for (Choice choice : choices) {
            SpeechRecognitionInputMethod inputMethod = new SpeechRecognitionInputMethod(sr, confidence,
                    Optional.empty());
            Prompt prompt = new Prompt(choices, Arrays.asList(inputMethod));
            prompt.lock.lockInterruptibly();
            try {
                inputMethod.show(prompt);
                awaitResult(sr, prompt, choice.phrases.get(0), new Prompt.Result(choices.indexOf(choice)));
            } finally {
                prompt.lock.unlock();
            }
        }

        String[] rejected = { "My name is", "FooBar" };
        for (String speech : rejected) {
            SpeechRecognitionInputMethod inputMethod = new SpeechRecognitionInputMethod(sr, confidence,
                    Optional.empty());
            Prompt prompt = new Prompt(choices, Arrays.asList(inputMethod));
            prompt.lock.lockInterruptibly();
            try {
                inputMethod.show(prompt);
                awaitResult(sr, prompt, speech, null);
            } finally {
                prompt.lock.unlock();
            }
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

}
