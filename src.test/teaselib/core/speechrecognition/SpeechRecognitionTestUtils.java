package teaselib.core.speechrecognition;

import static java.util.Arrays.asList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import teaselib.core.events.Event;
import teaselib.core.speechrecognition.events.SpeechRecognizedEventArgs;
import teaselib.core.speechrecognition.implementation.TeaseLibSRGS;
import teaselib.core.speechrecognition.srgs.StringSequence;
import teaselib.core.ui.Choices;
import teaselib.core.ui.InputMethod;
import teaselib.core.ui.InputMethods;
import teaselib.core.ui.Prompt;
import teaselib.core.ui.SpeechRecognitionInputMethod;

/**
 * @author Citizen-Cane
 *
 */
public class SpeechRecognitionTestUtils {
    private static final Logger logger = LoggerFactory.getLogger(SpeechRecognitionTestUtils.class);

    private static final int RECOGNITION_TIMEOUT_MILLIS = 500;
    static final Confidence confidence = Confidence.High;

    private SpeechRecognitionTestUtils() {
    }

    static List<Rule> assertRecognized(Choices choices, String phrase, Prompt.Result expected)
            throws InterruptedException {
        return emulateSpeechRecognition(choices, withoutPunctation(phrase), expected);
    }

    static List<Rule> assertRejected(Choices choices, String phrase) throws InterruptedException {
        return emulateSpeechRecognition(choices, withoutPunctation(phrase), null);
    }

    static List<Rule> emulateSpeechRecognition(Choices choices, String phrase, Prompt.Result expected)
            throws InterruptedException {
        assertEquals("Emulated speech may not contain punctation: '" + phrase + "'", withoutPunctation(phrase), phrase);

        SpeechRecognition sr = new SpeechRecognition(Locale.ENGLISH, TeaseLibSRGS.class);
        SpeechRecognitionInputMethod inputMethod = new SpeechRecognitionInputMethod(sr, confidence, Optional.empty());
        Prompt prompt = new Prompt(choices, new InputMethods(inputMethod));

        return awaitResult(inputMethod, sr, prompt, phrase, expected);
    }

    public static String withoutPunctation(String text) {
        return StringSequence.splitWords(text).stream().collect(Collectors.joining(" "));
    }

    static List<Rule> awaitResult(InputMethod inputMethod, SpeechRecognition sr, Prompt prompt, String emulatedSpeech,
            Prompt.Result expectedRules) throws InterruptedException {
        List<Rule> results = new ArrayList<>();
        Event<SpeechRecognizedEventArgs> completedHandler = null;
        Event<SpeechRecognizedEventArgs> rejectedHandler = null;

        // TODO test fails when events are added before the input method starts
        // because input method events are added last,
        // and the input method changes the flow of events
        // - completed but not distinct -> consume, fireRejected
        // This doens't work with debug code, but also wouldn't in production
        // -> add events at init, keep until disposed

        boolean dismissed;
        try {
            prompt.lock.lockInterruptibly();
            try {
                inputMethod.show(prompt);

                completedHandler = eventArgs -> {
                    results.addAll(asList(eventArgs.result));
                    logger.info("Recognized '{}'", eventArgs.result[0].text);
                };
                sr.events.recognitionCompleted.add(completedHandler);

                rejectedHandler = eventArgs -> {
                    prompt.setTimedOut();
                    results.addAll(asList(eventArgs.result));
                    logger.info("Rejected '{}'", eventArgs.result[0].text);
                };
                sr.events.recognitionRejected.add(rejectedHandler);

                sr.emulateRecogntion(emulatedSpeech);
                // TODO rejected event must occur - but doesn't - to avoid timeout work-around
                dismissed = prompt.click.await(RECOGNITION_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS);
                if (!dismissed) {
                    prompt.dismiss();
                }
            } finally {
                prompt.lock.unlock();
            }

            if (expectedRules != null) {
                assertTrue("Expected recognition:: \"" + emulatedSpeech + "\"", dismissed);

                if (prompt.acceptedResult == Prompt.Result.Accept.Multiple) {
                    assertEquals(expectedRules, prompt.result());
                } else {
                    assertAllTheSameChoices(expectedRules, prompt.result());
                }
            } else {
                assertFalse("Expected rejected: \"" + emulatedSpeech + "\"", dismissed);
            }
        } finally {
            sr.events.recognitionRejected.remove(rejectedHandler);
            sr.events.recognitionCompleted.remove(completedHandler);
        }

        return results;
    }

    private static void assertAllTheSameChoices(Prompt.Result expectedRules, Prompt.Result result) {
        List<Integer> choices = result.elements.stream().distinct().collect(Collectors.toList());
        assertTrue("Result contains different choices: " + result, choices.size() == 1);
        assertEquals("Expected choice " + expectedRules, expectedRules.elements.get(0), choices.get(0));
    }

    static void emulateRecognition(SpeechRecognition sr, SpeechRecognitionInputMethod inputMethod, Choices choices,
            String phrase) throws InterruptedException {
        Prompt prompt = new Prompt(choices, new InputMethods(inputMethod));
        awaitResult(inputMethod, sr, prompt, withoutPunctation(phrase),
                new Prompt.Result(choices.toText().indexOf(phrase)));
    }

}
