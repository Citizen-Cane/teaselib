package teaselib.core.speechrecognition.sapi;

import static teaselib.core.speechrecognition.sapi.SpeechRecognitionTestUtils.*;

import java.util.Locale;

import org.junit.Test;

import teaselib.core.speechrecognition.SpeechRecognitionInputMethod;
import teaselib.core.speechrecognition.SpeechRecognizer;
import teaselib.core.ui.Choice;
import teaselib.core.ui.Choices;
import teaselib.core.ui.Intention;
import teaselib.core.ui.Prompt;

/**
 * @author Citizen-Cane
 *
 */
public class SpeechRecognitionComplexTest {

    @Test
    public void testOptionalStart() throws InterruptedException {
        Choices choices = new Choices(Locale.ENGLISH, Intention.Decide, //
                new Choice("Of course"), new Choice("Yes Of course"));

        assertRecognized(choices, "Of course", new Prompt.Result(0));
        assertRecognized(choices, "Yes Of course", new Prompt.Result(1));
        // Recognized because of leading null rule with choice index == 0
        assertRecognized(choices, "Of", new Prompt.Result(0));
        assertRecognized(choices, "Yes", new Prompt.Result(1));

        assertRejected(choices, "Foo bar");
    }

    @Test
    public void testOptionalStartOptionalRule() throws InterruptedException {
        Choices choices = new Choices(Locale.ENGLISH, Intention.Decide, //
                new Choice("Yes Of course", "Yes Of course", "Of course", "Yes Of course"));

        assertRecognized(choices, "Of course", new Prompt.Result(0));
        assertRecognized(choices, "Yes Of course", new Prompt.Result(0));
        // TODO accepted as hypothesis but word count isn't weighted - too short
        assertRecognizedAsHypothesis(choices, "Of", new Prompt.Result(0));
        assertRecognizedAsHypothesis(choices, "Yes", new Prompt.Result(0));

        assertRejected(choices, "Foo bar");
    }

    @Test
    public void testOptionalEnd() throws InterruptedException {
        Choices choices = new Choices(Locale.ENGLISH, Intention.Decide, //
                new Choice("Of course"), new Choice("Of course not"));

        assertRecognized(choices, "Of course", new Prompt.Result(0));
        assertRecognized(choices, "Of course not", new Prompt.Result(1));

        assertRejected(choices, "Of");
        assertRejected(choices, "Not");
    }

    @Test
    public void testOptionalMiddle() throws InterruptedException {
        Choices choices = new Choices(Locale.ENGLISH, Intention.Decide, //
                new Choice("Of course Miss"), new Choice("Of course not Miss"));

        assertRecognized(choices, "Of course Miss", new Prompt.Result(0));
        assertRecognized(choices, "Of course not Miss", new Prompt.Result(1));

        assertRejected(choices, "Of");
        assertRejected(choices, "Not");
    }

    @Test
    public void testPunctationCharacters() throws InterruptedException {
        Choices choices = new Choices(Locale.ENGLISH, Intention.Decide, //
                new Choice("Oh Really?"), new Choice("?THese;_:are+a/lOT@(of)puncTAtion-maRkS!"),
                new Choice("No I haven't"));

        assertRecognized(choices, "Oh really", new Prompt.Result(0));
        assertRecognized(choices, "theSE aRe A Lot Of PUnctaTion mArks", new Prompt.Result(1));
        assertRecognized(choices, "No I haven't", new Prompt.Result(2));
    }

    @Test
    public void testRejected() throws InterruptedException {
        Choices choices = new Choices(Locale.ENGLISH, Intention.Decide, //
                new Choice("Foo"));
        assertRejected(choices, "Bar");
    }

    @Test
    public void testIDontHaveIt() throws InterruptedException {
        Choices choices = new Choices(Locale.ENGLISH, Intention.Decide, //
                new Choice("I have it"), new Choice("I don't have it"));

        assertRecognized(choices, "I have it", new Prompt.Result(0));
        assertRecognized(choices, "I don't have it", new Prompt.Result(1));
        assertRejected(choices, "have it");
    }

    @Test
    public void testOptionalEnd2() throws InterruptedException {
        String[] yes = { "Of course", "of course Miss" };
        String[] no = { "Of course not", "of course not Miss" };
        Choices choices = new Choices(Locale.ENGLISH, Intention.Decide, //
                new Choice("of course", "of course", yes), new Choice("of course not", "of course not", no));

        assertRecognized(choices, "Of course", new Prompt.Result(0));
        assertRecognized(choices, "of course Miss", new Prompt.Result(0));
        assertRecognized(choices, "Of course not Miss", new Prompt.Result(1));

        assertRecognizedAsHypothesis(choices, "of course", new Prompt.Result(0));
        assertRecognizedAsHypothesis(choices, "Of course not", new Prompt.Result(1));

        assertRejected(choices, "Miss");
    }

    private static Choices singleChoiceMultiplePhrasesAreDistinct() {
        String[] yes = { "Yes Miss, of course", "Of course, Miss" };
        return new Choices(Locale.ENGLISH, Intention.Decide, //
                new Choice("Yes #title, of course", "Yes Miss, of course", yes));
    }

    @Test
    public void testSRGSBuilderSingleChoiceMultiplePhrasesAreDistinct() throws InterruptedException {
        Choices choices = singleChoiceMultiplePhrasesAreDistinct();
        assertRecognized(choices, withoutPunctation("Yes Miss, of course"), new Prompt.Result(0));
        assertRecognized(choices, withoutPunctation("Of course, Miss"), new Prompt.Result(0));

        // Accepted as Intention.Confirm because SAPI emulation provides wrong confidence values == 1.0
        Choices hypothized = as(choices, Intention.Confirm);
        assertRecognizedAsHypothesis(hypothized, "Yes Miss", new Prompt.Result(0));

        try (SpeechRecognizer recognizer = getRecognizer(TeaseLibSRGS.Strict.class);
                SpeechRecognitionInputMethod inputMethod = new SpeechRecognitionInputMethod(recognizer)) {
            assertRejected(inputMethod, choices, "Of course");
        }

        try (SpeechRecognizer recognizer = getRecognizer(TeaseLibSRGS.Relaxed.class);
                SpeechRecognitionInputMethod inputMethod = new SpeechRecognitionInputMethod(recognizer)) {
            assertRecognizedAsHypothesis(inputMethod, choices, "Of course", new Prompt.Result(0));
        }

        assertRejected(choices, withoutPunctation("Miss"));
    }

    private static Choices multipleChoicesAlternativePhrases() {
        String[] yes = { "Yes Miss, of course", "Yes, of course, Miss", "Yes, of course", "Of course" };
        String[] no = { "No Miss, of course not", "No, of course not, Miss", "No, of course not", "Of course not" };
        return new Choices(Locale.ENGLISH, Intention.Decide, //
                new Choice("Yes #title, of course", "Yes Miss, of course", yes),
                new Choice("No #title, of course not", "No Miss, of course not", no));
    }

    @Test
    public void testSRGSBuilderAlternativePhrases() throws InterruptedException {
        Choices choices = multipleChoicesAlternativePhrases();

        assertRecognized(choices, withoutPunctation("Yes Miss, of course"), new Prompt.Result(0, 0));
        assertRecognized(choices, withoutPunctation("Yes, of course, Miss"), new Prompt.Result(0, 0));
        assertRecognized(choices, withoutPunctation("Yes, of course"), new Prompt.Result(0, 0));
        assertRecognized(choices, withoutPunctation("of course"), new Prompt.Result(0));

        assertRecognized(choices, withoutPunctation("No Miss, of course not"), new Prompt.Result(1, 1));
        assertRecognized(choices, withoutPunctation("No, of course not, Miss"), new Prompt.Result(1, 1));
        assertRecognized(choices, withoutPunctation("No, of course not"), new Prompt.Result(1, 1));
        assertRecognized(choices, withoutPunctation("of course not"), new Prompt.Result(1, 1));

        Choices hypothized = as(choices, Intention.Confirm);
        assertRecognizedAsHypothesis(hypothized, "Yes Miss", new Prompt.Result(0));
        assertRecognizedAsHypothesis(hypothized, "No Miss", new Prompt.Result(1));

        try (SpeechRecognizer recognizer = getRecognizer(TeaseLibSRGS.Strict.class);
                SpeechRecognitionInputMethod inputMethod = new SpeechRecognitionInputMethod(recognizer)) {
            assertRecognizedAsHypothesis(inputMethod, hypothized, "No Miss of course", new Prompt.Result(1));
        }

        try (SpeechRecognizer recognizer = getRecognizer(TeaseLibSRGS.Relaxed.class);
                SpeechRecognitionInputMethod inputMethod = new SpeechRecognitionInputMethod(recognizer)) {
            assertRejected(inputMethod, hypothized, "No Miss of course");
        }

        assertRejected(choices, "Of not");
    }

    @Test
    public void testSRGSBuilderAlternativePhrasesToChoiceMappingWithRepair() throws InterruptedException {
        Choices choices = multipleChoicesAlternativePhrases();

        Choices hypothized = as(choices, Intention.Confirm);
        assertRecognizedAsHypothesis(hypothized, withoutPunctation("Yes, of"), new Prompt.Result(0));
        assertRecognizedAsHypothesis(hypothized, withoutPunctation("No, of"), new Prompt.Result(1));
    }

    private static Choices optionalPhraseToDistiniguishMulitpleChoices() {
        return new Choices(Locale.ENGLISH, Intention.Decide, //
                new Choice("I have it"), new Choice("I don't have it"));
    }

    @Test
    public void testSRGSBuilderOptionalPhraseToDistiniguishMulitpleChoices() throws InterruptedException {
        Choices choices = optionalPhraseToDistiniguishMulitpleChoices();
        assertRecognized(choices, "I have it", new Prompt.Result(0));
        assertRecognized(choices, "I don't have it", new Prompt.Result(1));
    }

    private static Choices multiplePhrasesOfMultipleChoicesAreDistinct() {
        String[] yes = { "Yes Miss, of course", "Yes, of course, Miss", "Yes, of course" };
        String[] no = { "No Miss, of course not", "No, of course not, Miss", "No, of course not" };
        return new Choices(Locale.ENGLISH, Intention.Decide, //
                new Choice("Yes #title, of course", "Yes Miss, of course", yes),
                new Choice("No #title, of course not", "No Miss, of course not", no));
    }

    @Test
    public void testSRGSBuilderMultiplePhrasesOfMultipleChoicesAreDistinct() throws InterruptedException {
        Choices choices = multiplePhrasesOfMultipleChoicesAreDistinct();

        assertRecognized(choices, withoutPunctation("Yes Miss, of course"), new Prompt.Result(0, 0));
        assertRecognized(choices, withoutPunctation("Yes, of course, Miss"), new Prompt.Result(0, 0));
        assertRecognized(choices, withoutPunctation("Yes, of course"), new Prompt.Result(0, 0));

        assertRecognized(choices, withoutPunctation("No Miss, of course not"), new Prompt.Result(1, 1));
        assertRecognized(choices, withoutPunctation("No, of course not, Miss"), new Prompt.Result(1, 1));
        assertRecognized(choices, withoutPunctation("No, of course not"), new Prompt.Result(1, 1));

        Choices hypothized = as(choices, Intention.Confirm);
        assertRecognizedAsHypothesis(hypothized, "Yes Miss", new Prompt.Result(0));
        assertRecognizedAsHypothesis(hypothized, "No Miss", new Prompt.Result(1));

        try (SpeechRecognizer recognizer = getRecognizer(TeaseLibSRGS.Strict.class);
                SpeechRecognitionInputMethod inputMethod = new SpeechRecognitionInputMethod(recognizer)) {
            // Repaired and recognized
            assertRecognizedAsHypothesis(inputMethod, hypothized, "No Miss of course", new Prompt.Result(1));
        }

        try (SpeechRecognizer recognizer = getRecognizer(TeaseLibSRGS.Relaxed.class);
                SpeechRecognitionInputMethod inputMethod = new SpeechRecognitionInputMethod(recognizer)) {
            // TODO Not repaired, not recognized -> trailing NULL rule (missing "not") results in no indices
            // - phrase would probably be recognized as hypothesis, but the event is not always fired by the emulation
            assertRejected(inputMethod, hypothized, "No Miss of course");
        }

        assertRejected(choices, "Of not");
        assertRejected(choices, "Of course");
        assertRejected(choices, "Of course not");
    }

    private static Choices multipleChoicesAlternativePhrasesWithOptionalPartsAreDistinct() {
        String[] yes = { "Yes Miss, of course", "Yes, of course, Miss", "Yes, of course", "Of course", "I have it" };
        String[] no = { "No Miss, of course not", "No, of course not, Miss", "No, of course not", "Of course not",
                "I don't have it" };
        return new Choices(Locale.ENGLISH, Intention.Decide, //
                new Choice("Yes #title, of course", "Yes Miss, of course", yes),
                new Choice("No #title, of course not", "No Miss, of course not", no));
    }

    @Test
    public void testSRGSBuilderMultipleChoicesAlternativePhrasesWithOptionalPartsAreDistinct()
            throws InterruptedException {
        Choices choices = multipleChoicesAlternativePhrasesWithOptionalPartsAreDistinct();

        assertRecognized(choices, withoutPunctation("Yes Miss, of course"), new Prompt.Result(0, 0, 0));
        assertRecognized(choices, withoutPunctation("Yes, of course, Miss"), new Prompt.Result(0, 0, 0));
        assertRecognized(choices, withoutPunctation("Yes, of course"), new Prompt.Result(0, 0, 0));
        assertRecognized(choices, withoutPunctation("of course"), new Prompt.Result(0, 0, 0));
        assertRecognized(choices, withoutPunctation("I have it"), new Prompt.Result(0));

        assertRecognized(choices, withoutPunctation("No Miss, of course not"), new Prompt.Result(1, 1, 1));
        assertRecognized(choices, withoutPunctation("No, of course not, Miss"), new Prompt.Result(1, 1, 1));
        assertRecognized(choices, withoutPunctation("No, of course not"), new Prompt.Result(1, 1, 1));
        assertRecognized(choices, withoutPunctation("of course not"), new Prompt.Result(1, 1, 1));
        assertRecognized(choices, withoutPunctation("I don't have it"), new Prompt.Result(1));

        Choices hypothized = as(choices, Intention.Confirm);
        assertRecognizedAsHypothesis(hypothized, "Yes Miss", new Prompt.Result(0));
        assertRecognizedAsHypothesis(hypothized, "No Miss", new Prompt.Result(1));
        assertRecognizedAsHypothesis(hypothized, "No Miss of course", new Prompt.Result(1));

        assertRejected(choices, "Of not");
    }

    private static Choices multiplePhrasesOfMultipleChoicesAreDistinctWithoutOptionalParts() {
        String[] yes = { "Yes Miss, of course", "Yes, of course, Miss", "Yes, of course", "I have it" };
        String[] no = { "No Miss, of course not", "No, of course not, Miss", "No, of course not", "I don't have it" };
        return new Choices(Locale.ENGLISH, Intention.Decide, //
                new Choice("Yes #title, of course", "Yes Miss, of course", yes),
                new Choice("No #title, of course not", "No Miss, of course not", no));
    }

    @Test
    public void testSRGSBuilderMultiplePhrasesOfMultipleChoicesAreDistinctWithoutOptionalParts()
            throws InterruptedException {
        Choices choices = multiplePhrasesOfMultipleChoicesAreDistinctWithoutOptionalParts();

        assertRecognized(choices, withoutPunctation("Yes Miss, of course"), new Prompt.Result(0, 0));
        assertRecognized(choices, withoutPunctation("Yes, of course, Miss"), new Prompt.Result(0, 0, 0, 0));
        assertRecognized(choices, withoutPunctation("Yes, of course"), new Prompt.Result(0, 0, 0, 0));
        assertRecognized(choices, withoutPunctation("I have it"), new Prompt.Result(0));

        assertRecognized(choices, withoutPunctation("No Miss, of course not"), new Prompt.Result(1, 1, 1, 1));
        assertRecognized(choices, withoutPunctation("No, of course not, Miss"), new Prompt.Result(1, 1, 1, 1));
        assertRecognized(choices, withoutPunctation("No, of course not"), new Prompt.Result(1, 1, 1, 1));
        assertRecognized(choices, withoutPunctation("I don't have it"), new Prompt.Result(1));

        Choices hypothized = as(choices, Intention.Confirm);
        assertRecognizedAsHypothesis(hypothized, "Yes Miss", new Prompt.Result(0));
        assertRecognizedAsHypothesis(hypothized, "No Miss", new Prompt.Result(1));
        assertRecognizedAsHypothesis(hypothized, "No Miss of course", new Prompt.Result(1));

        assertRejected(choices, "Of not");
        assertRejected(choices, "Of course");
        assertRejected(choices, "Of course not");
    }

    private static Choices phrasesWithMultipleCommonStartGroups() {
        String[] cum = { "Dear mistress, may I cum", "Please mistress, may I cum" };
        String[] wank = { "Dear mistress, may I wank", "Please mistress, may I wank" };
        return new Choices(Locale.ENGLISH, Intention.Decide, //
                new Choice("Dear mistress, may I cum", "Dear mistress, may I cum", cum),
                new Choice("Dear mistress, may I wank", "Dear mistress, may I wank", wank));
    }

    @Test
    public void testSRGSBuilderPhrasesWithSeveralCommonStartGroups() throws InterruptedException {
        Choices choices = phrasesWithMultipleCommonStartGroups();

        assertRecognized(choices, withoutPunctation("Dear mistress, may I cum"), new Prompt.Result(0));
        assertRecognized(choices, withoutPunctation("Please mistress, may I cum"), new Prompt.Result(0));

        assertRecognized(choices, withoutPunctation("Dear mistress, may I wank"), new Prompt.Result(1));
        assertRecognized(choices, withoutPunctation("Please mistress, may I wank"), new Prompt.Result(1));

        assertRejected(choices, "Please Mistress");
        assertRejected(choices, "Dear Mistress");

        assertRejected(choices, "May I cum");
        assertRejected(choices, "May I wank");

        assertRejected(choices, withoutPunctation("Mistress, may I"));
    }

    private static Choices phrasesWithMultipleCommonEndGroups() {
        String[] cum = { "May I cum, please", "May I cum, dear Mistress" };
        String[] wank = { "May I wank, please", "May I wank, dear Mistress" };
        return new Choices(Locale.ENGLISH, Intention.Decide, //
                new Choice("May I cum, please", "May I cum, please", cum),
                new Choice("May I wank, please", "May I wank, please", wank));
    }

    @Test
    public void testSRGSBuilderPhrasesWithSeveralCommonEndGroups() throws InterruptedException {
        Choices choices = phrasesWithMultipleCommonEndGroups();

        assertRecognized(choices, withoutPunctation("May I cum, please"), new Prompt.Result(0));
        assertRecognized(choices, withoutPunctation("May I cum, dear mistress"), new Prompt.Result(0));

        assertRecognized(choices, withoutPunctation("May I wank, please"), new Prompt.Result(1));
        assertRecognized(choices, withoutPunctation("May I wank, dear mistress"), new Prompt.Result(1));

        assertRejected(choices, "please");
        assertRejected(choices, "Dear Mistress");
    }

    @Test
    public void testSRGSBuilderPhrasesWithSeveralCommonEndGroupsPhraseToChoiceMapping() throws InterruptedException {
        Choices choices = phrasesWithMultipleCommonEndGroups();

        Choices hypothized = as(choices, Intention.Confirm);
        assertRecognizedAsHypothesis(hypothized, "May I cum", new Prompt.Result(0));
        assertRecognizedAsHypothesis(hypothized, "May I wank", new Prompt.Result(1));
    }

    private static Choices identicalPhrasesInDifferentChoices() {
        String[] yes = { "Yes Miss, of course", "Yes, of course, Miss" };
        String[] no = { "Yes Miss, of course", "No, of course not, Miss" };
        return new Choices(Locale.ENGLISH, Intention.Decide, //
                new Choice("Yes #title, of course", "Yes Miss, of course", yes),
                new Choice("No #title, of course not", "No Miss, of course not", no));
    }

    @Test
    public void testSRGSBuilderIdenticalPhrasesInDifferentChoices() throws InterruptedException {
        Choices choices = identicalPhrasesInDifferentChoices();

        try (SpeechRecognizer recognizer = getRecognizer(TeaseLibSRGS.Strict.class);
                SpeechRecognitionInputMethod inputMethod = new SpeechRecognitionInputMethod(recognizer)) {
            // Being recognized as choice==0 isn't totally wrong -> add validation task for PCM-scripts
            assertRecognized(inputMethod, choices, withoutPunctation("Yes, of course, Miss"), new Prompt.Result(0));
        }

        try (SpeechRecognizer recognizer = getRecognizer(TeaseLibSRGS.Relaxed.class);
                SpeechRecognitionInputMethod inputMethod = new SpeechRecognitionInputMethod(recognizer)) {
            assertRejected(inputMethod, choices, withoutPunctation("Yes, of course, Miss"));
        }

        assertRecognized(choices, withoutPunctation("No, of course not, Miss"), new Prompt.Result(1));

        assertRejected(choices, withoutPunctation("Yes Miss, of course, Miss"));
        assertRejected(choices, withoutPunctation("Yes Miss, of course not, Miss"));
        // Rejected since the phrase occurs contains in both choices
        assertRejected(choices, withoutPunctation("Yes Miss, of course"));
    }

    private static Choices oneOfCommonAndChoicesMixed() {
        String[] yes = { "Yes Miss, of course", "Yes, of course, Miss" };
        String[] no = { "No Miss, of course", "No, of course not, Miss" };
        return new Choices(Locale.ENGLISH, Intention.Decide, //
                new Choice("Yes #title, of course", "Yes Miss, of course", yes),
                new Choice("No #title, of course not", "No Miss, of course not", no));
    }

    @Test
    public void testSRGSBuilderOneOfCommonAndChoicesMixed() throws InterruptedException {
        Choices choices = oneOfCommonAndChoicesMixed();

        assertRejected(choices, withoutPunctation("of course"));

        assertRecognized(choices, withoutPunctation("Yes Miss, of course"), new Prompt.Result(0));
        assertRecognized(choices, withoutPunctation("Yes, of course, Miss"), new Prompt.Result(0));
        assertRecognized(choices, withoutPunctation("No Miss, of course"), new Prompt.Result(1));
        assertRecognized(choices, withoutPunctation("No, of course not, Miss"), new Prompt.Result(1));
    }

    private static Choices distinctChociesWithPairwiseCommonParts() {
        return new Choices(Locale.ENGLISH, Intention.Decide, //
                new Choice("A at M attached"), new Choice("A at N attached"), new Choice("B at N attached"),
                new Choice("B at M attached"));
    }

    @Test
    public void testSRGSBuilderDistinctChociesWithPairwiseCommonParts() throws InterruptedException {
        Choices choices = distinctChociesWithPairwiseCommonParts();

        assertRecognized(choices, "A at M attached", new Prompt.Result(0));
        assertRecognized(choices, "A at N attached", new Prompt.Result(1));
        assertRecognized(choices, "B at N attached", new Prompt.Result(2));
        assertRecognized(choices, "B at M attached", new Prompt.Result(3));
    }

    private static Choices distinctChociesWithPairwiseCommonPartsShort() {
        return new Choices(Locale.ENGLISH, Intention.Decide, //
                new Choice("A M"), new Choice("A N"), new Choice("B N"), new Choice("B M"));
    }

    @Test
    public void testSRGSBuilderDistinctChociesWithPairwiseCommonPartsShort() throws InterruptedException {
        Choices choices = distinctChociesWithPairwiseCommonPartsShort();

        assertRecognized(choices, "A M", new Prompt.Result(0));
        assertRecognized(choices, "A N", new Prompt.Result(1));
        assertRecognized(choices, "B N", new Prompt.Result(2));
        assertRecognized(choices, "B M", new Prompt.Result(3));
    }

    @Test
    public void testSRGSBuilderMultiLevelCommon() throws InterruptedException {
        Choices choices = new Choices(Locale.ENGLISH, Intention.Decide, //
                new Choice("A B0 C0 D"), //
                new Choice("A B1 C0 D"), //
                new Choice("A B2 C2 D"));

        assertRecognized(choices, "A B0 C0 D", new Prompt.Result(0));
        assertRecognized(choices, "A B1 C0 D", new Prompt.Result(1));
        assertRecognized(choices, "A B2 C2 D", new Prompt.Result(2));
    }

    @Test
    public void testSRGSBuilderMultiLevelCommon2() throws InterruptedException {
        Choices choices = new Choices(Locale.ENGLISH, Intention.Decide, //
                new Choice("A B0 C0 D"), //
                new Choice("A B1 C0 D"), //
                new Choice("A B2 C2 D"), //
                new Choice("A B3 C2 D"));

        assertRecognized(choices, "A B0 C0 D", new Prompt.Result(0));
        assertRecognized(choices, "A B1 C0 D", new Prompt.Result(1));
        assertRecognized(choices, "A B2 C2 D", new Prompt.Result(2));
        assertRecognized(choices, "A B3 C2 D", new Prompt.Result(3));
    }

}
