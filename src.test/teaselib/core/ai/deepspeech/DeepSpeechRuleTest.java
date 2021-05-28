package teaselib.core.ai.deepspeech;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static teaselib.core.speechrecognition.Confidence.High;

import java.util.Locale;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import teaselib.core.ai.TeaseLibAI;
import teaselib.core.speechrecognition.Rule;
import teaselib.core.speechrecognition.SpeechRecognitionEvents;
import teaselib.core.speechrecognition.events.SpeechRecognizedEventArgs;
import teaselib.core.speechrecognition.sapi.SpeechRecognitionTestUtils;
import teaselib.core.ui.Choice;
import teaselib.core.ui.Choices;
import teaselib.core.ui.Intention;

/**
 * @author Citien-Cane
 *
 */
class DeepSpeechRuleTest {

    @Test
    void testLanguageFallbackWorks() throws InterruptedException {
        try (TeaseLibAI teaseLibAI = new TeaseLibAI()) {
            SpeechRecognitionEvents events = new SpeechRecognitionEvents();

            AtomicReference<SpeechRecognizedEventArgs> speechRecognized = new AtomicReference<>();
            CountDownLatch signal = new CountDownLatch(1);
            events.recognitionCompleted.add(e -> {
                speechRecognized.set(e);
                signal.countDown();
            });
            Locale australianEnglish = new Locale("en", "au");
            try (DeepSpeechRecognizer deepSpeechRecognizer = new DeepSpeechRecognizer(australianEnglish)) {
                deepSpeechRecognizer.startEventLoop(events);
                Choices choices = new Choices(australianEnglish, Intention.Confirm, new Choice("foobar"));
                deepSpeechRecognizer.prepare(choices).accept(deepSpeechRecognizer);
                deepSpeechRecognizer.emulateRecognition("foobar");
                SpeechRecognitionTestUtils.await(0, deepSpeechRecognizer, speechRecognized, signal);
                assertTrue(deepSpeechRecognizer.getException().isEmpty());
            }
        }
    }

    static class EmulatedSpeech {
        final String expected;
        final float expectedProbability;
        final String actual;

        public EmulatedSpeech(String expected, float probability, String actual) {
            this.expected = expected;
            this.actual = actual;
            this.expectedProbability = probability;
        }

        @Override
        public String toString() {
            return expected;
        }

    }

    static Stream<EmulatedSpeech> emulatedSpeechTests() {
        return Stream.of(//
                new EmulatedSpeech("Yes Miss", 0.9113f, """
                        yes
                        yes
                        yes i
                        yes is
                        yes miss
                        yes it
                        yes m
                        yes a
                        yes as
                        yes s
                        """), //
                new EmulatedSpeech("Yes Miss", 0.7380f, """
                        yes
                        i guess
                        as
                        yet
                        guess
                        is
                        get
                        his
                        at
                        eyes
                        """), //
                new EmulatedSpeech("I have new shoes Miss", 0.7575f, """
                        i have now so is
                        i have now she is
                        i have new so as
                        i have now to ask
                        i have now to his
                        """));
    }

    @ParameterizedTest
    @MethodSource("emulatedSpeechTests")
    void testEmulatedSpeech(EmulatedSpeech test) throws InterruptedException {
        assertGreaterThan(test.expectedProbability, emulateSpeech(test.expected, test.actual).probability,
                () -> "Probability");
    }

    static void assertGreaterThan(float expected, float actual, Supplier<String> meessage) {
        if (actual < expected) {
            fail(meessage.get() + " " + expected + " expected, but got " + actual);
        }
    }

    @Test
    void testMissingWordAtEnd() throws InterruptedException {
        Rule rule = emulateSpeech("Yes Miss I have it", "yes miss i have");
        assertEquals(5, rule.children.size());
    }

    @Test
    void testMissingWordsAtEnd() throws InterruptedException {
        Rule rule = emulateSpeech("Yes Miss I have it", "yes miss i");
        assertEquals(5, rule.children.size());
    }

    @Test
    void testMissingWordInTheMiddle() throws InterruptedException {
        Rule rule = emulateSpeech("Yes dear Mistress I have it here", "yes i have");
        assertEquals(7, rule.children.size());
    }

    // Testdata taken from failing test when audio stream buffers weren't cleared after recognition
    // "paris" decomposes to "par is" - contains the missing word, better than nothing

    // When all tests are run without resetting the audio stream, "your paris efficient i said" is recognized
    // [[your, paris, efficient, i, said] confidence=1.0]
    // [[your, paris, sufficient, i, said] confidence=0.9432924]
    // [[you, paris, efficient, i, said] confidence=0.84364516]
    // [[your, parents, efficient, i, said] confidence=0.8344438]
    // [[your, paris, efficient, y, said] confidence=0.82584846]

    // // When one test are run, "your PART is sufficient i said" is recognized - expected
    // [[your, part, is, sufficient, i, said] confidence=1.0]
    // [[your, paris, sufficient, i, said] confidence=0.82849437]
    // [[your, part, is, sufficient, i, said] confidence=0.76291585]
    // [[your, part, as, sufficient, i, said] confidence=0.7604105]
    // [[your, part, is, sufficient, said] confidence=0.75178033]

    // test catches samples from the previous recognition -> clear buffers, when to call clear() - impl coreect?
    static final String detectedSpeech = """
            your paris efficient i said
            your paris sufficient i said
            you paris efficient i said
            your parents efficient i said
            your paris efficient y said
            """;

    @Test
    void testMissingWordAndWrongRecognitionGroundTruth() throws InterruptedException {
        Rule rule = emulateSpeech(DeepSpeechTestData.AUDIO_8455_210777_0068_RAW.groundTruth, detectedSpeech);
        assertEquals(6, rule.children.size());
        assertNotNull(rule.children.get(3).text, "\"is\" should be recognized as null rule");
        assertNotNull(rule.children.get(4).text, "\"sufficient\" should be recognized as child 4");
        assertNotNull(rule.children.get(5).text, "\"I\" should be recognized as child 5");
        SpeechRecognitionTestUtils.assertConfidence(rule, High.probability);
    }

    @Test
    void testMissingWordAndWrongRecognitionActual() throws InterruptedException {
        Rule rule = emulateSpeech(DeepSpeechTestData.AUDIO_8455_210777_0068_RAW.actual, detectedSpeech);
        assertEquals(6, rule.children.size());
        assertNotNull(rule.children.get(3).text, "\"is\" should be recognized as null rule");
        assertNotNull(rule.children.get(4).text, "\"sufficient\" should be recognized as child 4");
        assertNotNull(rule.children.get(5).text, "\"I\" should be recognized as child 5");
        SpeechRecognitionTestUtils.assertConfidence(rule, High.probability);
    }

    @Test
    void testExperienceProovesThisAsText() throws InterruptedException {
        emulateSpeech("experience prooves this",
                "experience prooves this\nexperience prooves that\nthe experience proofs it");
    }

    private static Rule emulateSpeech(String choice, String speech) throws InterruptedException, UnsatisfiedLinkError {
        try (TeaseLibAI teaseLibAI = new TeaseLibAI()) {
            SpeechRecognitionEvents events = new SpeechRecognitionEvents();

            AtomicReference<SpeechRecognizedEventArgs> speechRecognized = new AtomicReference<>();
            CountDownLatch signal = new CountDownLatch(1);
            events.recognitionCompleted.add(e -> {
                speechRecognized.set(e);
                signal.countDown();
            });

            Rule best;
            try (DeepSpeechRecognizer deepSpeechRecognizer = new DeepSpeechRecognizer(Locale.ENGLISH)) {
                deepSpeechRecognizer.startEventLoop(events);
                Choices choices = new Choices(Locale.ENGLISH, Intention.Confirm, new Choice(choice));
                deepSpeechRecognizer.prepare(choices).accept(deepSpeechRecognizer);
                deepSpeechRecognizer.emulateRecognition(speech);
                best = SpeechRecognitionTestUtils.await(0, deepSpeechRecognizer, speechRecognized, signal);
            }

            assertNotNull(speechRecognized.get());
            return best;
        }
    }

}
