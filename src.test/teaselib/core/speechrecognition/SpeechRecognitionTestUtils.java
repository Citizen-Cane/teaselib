package teaselib.core.speechrecognition;

import static java.util.Arrays.*;
import static java.util.stream.Collectors.*;
import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.junit.Assume;

import teaselib.core.events.Event;
import teaselib.core.speechrecognition.events.SpeechRecognizedEventArgs;
import teaselib.core.speechrecognition.implementation.TeaseLibSRGS;
import teaselib.core.speechrecognition.srgs.Phrases;
import teaselib.core.speechrecognition.srgs.Sequences;
import teaselib.core.speechrecognition.srgs.StringSequence;
import teaselib.core.ui.Choices;
import teaselib.core.ui.Prompt;
import teaselib.core.ui.SpeechRecognitionInputMethod;

/**
 * @author Citizen-Cane
 *
 */
public class SpeechRecognitionTestUtils {

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
        Prompt prompt = new Prompt(choices, Arrays.asList(inputMethod));

        prompt.lock.lockInterruptibly();
        try {
            inputMethod.show(prompt);
            return awaitResult(sr, prompt, phrase, expected);
        } finally {
            prompt.lock.unlock();
            sr.close();
        }
    }

    public static String withoutPunctation(String text) {
        return StringSequence.splitWords(text).stream().collect(Collectors.joining(" "));
    }

    static List<Rule> awaitResult(SpeechRecognition sr, Prompt prompt, String emulatedSpeech,
            Prompt.Result expectedRules) throws InterruptedException {
        List<Rule> results = new ArrayList<>();
        Event<SpeechRecognizedEventArgs> completedHandler = eventArgs -> {
            results.addAll(asList(eventArgs.result));
        };
        sr.events.recognitionCompleted.add(completedHandler);

        Event<SpeechRecognizedEventArgs> rejectedHandler = eventArgs -> {
            prompt.setTimedOut();
            results.addAll(asList(eventArgs.result));
        };
        sr.events.recognitionRejected.add(rejectedHandler);

        try {
            sr.emulateRecogntion(emulatedSpeech);
            // TODO rejected event must occur - but doesn't - to avoid timeout work-around
            boolean dismissed = prompt.click.await(RECOGNITION_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS);
            if (!dismissed) {
                prompt.dismiss();
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
        Prompt prompt = new Prompt(choices, Arrays.asList(inputMethod));
        prompt.lock.lockInterruptibly();
        try {
            inputMethod.show(prompt);
            awaitResult(sr, prompt, withoutPunctation(phrase), new Prompt.Result(choices.toText().indexOf(phrase)));
        } finally {
            prompt.lock.unlock();
        }
    }

    public static void assertEqualsFlattened(Choices choices, Phrases phrases) {
        Sequences<String> flattened = flatten(phrases);
        assertEquals(choices.size(), flattened.size());

        List<String> allChoices = firstOfEach(choices).stream().map(SpeechRecognitionTestUtils::withoutPunctation)
                .collect(toList());
        assertEquals(allChoices, flattened.toStrings());
    }

    public static void assertChoicesAndPhrasesMatch(Choices choices, Phrases phrases) {
        Sequences<String> flattened = flatten(phrases);
        assertEquals(choices.size(), flattened.size());

        List<String> allChoices = all(choices).stream().map(SpeechRecognitionTestUtils::withoutPunctation)
                .collect(toList());
        flattened.toStrings().stream().forEach(phrase -> {
            assertTrue("'" + phrase + "' not found in: " + allChoices, allChoices.contains(phrase));
        });
    }

    private static List<String> all(Choices choices) {
        return choices.stream().flatMap(p -> p.phrases.stream()).collect(toList());
    }

    private static List<String> firstOfEach(Choices choices) {
        return choices.stream().map(p -> p.phrases.get(0)).collect(toList());
    }

    /**
     * Flattens phrases to input strings.
     * 
     * @param phrases
     * 
     * @return A list containing the first phrase of each choice.
     */
    public static Sequences<String> flatten(Phrases phrases) {
        Assume.assumeTrue("Phrases are deprecated", false);
        return null;
        // int choices = phrases.choices();
        // Sequences<String> flattened = StringSequences.of(choices);
        // for (int i = 0; i < choices; i++) {
        // StringSequence sequence = StringSequence.ignoreCase();
        // flattened.add(sequence);
        // }
        //
        // int rules = phrases.rules();
        // int groups = phrases.groups();
        // Set<Integer> processed = new HashSet<>();
        //
        // for (int group = 0; group < groups; group++) {
        // for (int choiceIndex = 0; choiceIndex < choices; choiceIndex++) {
        // if (!processed.contains(choiceIndex)) {
        // boolean choiceProcessed = false;
        // for (int ruleIndex = 0; ruleIndex < rules; ruleIndex++) {
        // for (Rule rule : phrases) {
        // String word = "";
        // if (rule.group == group && rule.index == ruleIndex) {
        // for (OneOf items : rule) {
        // // The sequence of the items in OneOf matters
        // if (items.choices.contains(choiceIndex)) {
        // word = items.iterator().next();
        // choiceProcessed = true;
        // break;
        // }
        // }
        // flattened.get(choiceIndex).add(word);
        // }
        // }
        // }
        // if (choiceProcessed) {
        // processed.add(choiceIndex);
        // }
        // }
        // }
        // }
        // return flattened;
    }

}
