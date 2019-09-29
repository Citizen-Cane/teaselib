package teaselib.core.speechrecognition.srgs;

import static org.junit.Assert.*;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.Test;

public class ListSequenceTest {

    static List<String> splitWords(String string) {
        return StringSequence.splitWords(string);
    }

    public static Sequences<String> ignoreCase(String... strings) {
        List<StringSequence> sequences = Arrays.stream(strings).map(StringSequence::ignoreCase)
                .collect(Collectors.toList());
        return new StringSequences(sequences);
    }

    @Test
    public void testSequenceStart() {
        Sequence<String> s1 = new Sequence<>(splitWords("The dog jumped over the fence"), StringSequence.Traits);
        Sequence<String> s2 = new Sequence<>(splitWords("The dog"), StringSequence.Traits);

        assertTrue(s1.startsWith(s2));
        assertFalse(s1.endsWith(s2));
        assertEquals(0, s1.indexOf(s2));
        assertEquals(0, s1.lastIndexOf(s2));
    }

    @Test
    public void testSequenceEnd() {
        Sequence<String> s1 = new Sequence<>(splitWords("The dog jumped over the fence"), StringSequence.Traits);
        Sequence<String> s2 = new Sequence<>(splitWords("the fence"), StringSequence.Traits);

        assertTrue(s1.endsWith(s2));
        assertFalse(s1.startsWith(s2));
        assertEquals(s1.size() - s2.size(), s1.indexOf(s2));
        assertEquals(s1.size() - s2.size(), s1.lastIndexOf(s2));
    }

    @Test
    public void testSequenceMid() {
        Sequence<String> s1 = new Sequence<>(splitWords("The dog jumped over the fence"), StringSequence.Traits);
        Sequence<String> s2 = new Sequence<>(splitWords("dog jumped"), StringSequence.Traits);

        assertFalse(s1.startsWith(s2));
        assertFalse(s1.endsWith(s2));
        assertEquals(1, s1.indexOf(s2));
        assertEquals(1, s1.lastIndexOf(s2));
    }

    @Test
    public void testCommonStartNoMatchSameLengths() {
        Sequence<String> commonStart = ignoreCase("My dog jumped over the fence", "The dog looked over the fence",
                "The dog jumped over the sofa", "The dog jumped over the fence").commonStart();
        assertEquals(new Sequence<>(splitWords(""), StringSequence.Traits), commonStart);
    }

    @Test
    public void testCommonStartNoMatchDifferentLengths() {
        Sequence<String> commonStart = ignoreCase("My dog jumped over the fence",
                "The dog looked over the fence Schmidt", "The dog jumped over the sofa", "One two three").commonStart();
        assertEquals(new Sequence<>(splitWords(""), StringSequence.Traits), commonStart);
    }

    @Test
    public void testCommonStart1() {
        Sequence<String> commonStart = ignoreCase("The dog jumped over the fence", "The dog looked over the fence",
                "The dog jumped over the sofa", "The dog jumped over the fence").commonStart();
        assertEquals(new Sequence<>(splitWords("The dog"), StringSequence.Traits), commonStart);
    }

    @Test
    public void testCommonStart2() {
        Sequence<String> commonStart = ignoreCase("The dog jumped over the fence", "The dog jumped over the fence",
                "The dog jumped over the sofa", "The dog jumped over my fence").commonStart();
        assertEquals(new Sequence<>(splitWords("The dog jumped over"), StringSequence.Traits), commonStart);
    }

    @Test
    public void testCommonStart3() {
        Sequence<String> commonStart = ignoreCase("The dog jumped over the fence", "The dog jumped over the fence",
                "The dog jumped over the fence", "The dog jumped over the fence").commonStart();
        assertEquals(new Sequence<>(splitWords("The dog jumped over the fence"), StringSequence.Traits),
                commonStart);
    }

    @Test
    public void testCommonEndSameLengths() {
        Sequence<String> commonEnd = ignoreCase("My dog jumped over the fence", "The dog looked over the fence",
                "The dog jumped over the sofa", "The dog jumped over the fence").commonEnd();
        assertEquals(new Sequence<>(splitWords(""), StringSequence.Traits), commonEnd);
    }

    @Test
    public void testCommonEndDifferentLengths() {
        Sequence<String> commonEnd = ignoreCase("My dog jumped over the fence", "The dog looked over the fence",
                "The dog jumped over the fence fast", "My dog jumped over the fence").commonEnd();
        assertEquals(new Sequence<>(splitWords(""), StringSequence.Traits), commonEnd);
    }

    @Test
    public void testCommonEnd() {
        Sequence<String> commonEnd = ignoreCase("My dog jumped over the fence", "The dog looked over the fence",
                "The dog jumped over the fence", "The dog jumped over the fence").commonEnd();
        assertEquals(new Sequence<>(splitWords("over the fence"), StringSequence.Traits), commonEnd);
    }

    @Test
    public void testCommonEnd2() {
        Sequence<String> commonEnd = ignoreCase("The dog jumped over the fence", "The dog jumped over the fence",
                "The cat jumped over the fence", "The dog jumped over the fence").commonEnd();
        assertEquals(new Sequence<>(splitWords("jumped over the fence"), StringSequence.Traits), commonEnd);
    }

    @Test
    public void testCommonEnd3() {
        Sequence<String> commonEnd = ignoreCase("The dog jumped over the fence", "The dog jumped over the fence",
                "The dog jumped over the fence", "The dog jumped over the fence").commonEnd();
        assertEquals(new Sequence<>(splitWords("The dog jumped over the fence"), StringSequence.Traits),
                commonEnd);
    }

    @Test
    public void testCommonMiddleNoMatchSameLengths() {
        Sequence<String> commonMiddle = ignoreCase("Your dog jumped over your fence", "The dog looked over the fence",
                "The cat jumped under the sofa", "The dog jumped over my fence").commonMiddle();
        assertEquals(new Sequence<>(splitWords(""), StringSequence.Traits), commonMiddle);
    }

    @Test
    public void testCommonMiddleNoMatchDifferentLengths() {
        Sequence<String> commonMiddle = ignoreCase("Your dog jumped over your fence", "The dog looked over the fence",
                "The cat jumped under the sofa", "One two three").commonMiddle();
        assertEquals(new Sequence<>(splitWords(""), StringSequence.Traits), commonMiddle);
    }

    @Test
    public void testCommonMiddle1() {
        Sequence<String> commonMiddle = ignoreCase("My dog jumped over the fence", "The cat jumped over the fence",
                "The dog jumped over your sofa", "The dog jumped over the fence").commonMiddle();
        assertEquals(new Sequence<>(splitWords("jumped over"), StringSequence.Traits), commonMiddle);
    }

    @Test
    public void testCommonMiddle2a() {
        Sequence<String> commonMiddle = ignoreCase("The dog jumped over the fence", "The dog jumped over the fence",
                "Your cat jumped over your sofa", "The dog jumped over the fence").commonMiddle();
        assertEquals(new Sequence<>(splitWords("jumped over"), StringSequence.Traits), commonMiddle);
    }

    @Test
    public void testCommonMiddle2b() {
        Sequence<String> commonMiddle = ignoreCase("The dog jumped over the fence", "The dog jumped over the sofa",
                "The cat jumped over the fence", "The dog jumped over the fence").commonMiddle();
        assertEquals(new Sequence<>(splitWords("jumped over the"), StringSequence.Traits), commonMiddle);
    }

    @Test
    public void testCommonMiddle3() {
        Sequence<String> commonMiddle = ignoreCase("The dog jumped over the fence", "The dog jumped over the fence",
                "The dog jumped over the fence", "The dog jumped over the fence").commonMiddle();
        assertEquals(new Sequence<>(splitWords("The dog jumped over the fence"), StringSequence.Traits),
                commonMiddle);
    }

    @Test
    public void testCommonMiddleFindsEnd1() {
        Sequence<String> commonEnd = ignoreCase("My dog jumped over the fence", "The dog looked over the fence",
                "The dog jumped over the fence", "The dog jumped over the fence").commonMiddle();
        assertEquals(new Sequence<>(splitWords("over the fence"), StringSequence.Traits), commonEnd);
    }

    @Test
    public void testCommonMiddleFindsEnd2() {
        Sequence<String> commonEnd = ignoreCase("The dog jumped over the fence", "The dog jumped over the fence",
                "The cat jumped over the fence", "The dog jumped over the fence").commonMiddle();
        assertEquals(new Sequence<>(splitWords("jumped over the fence"), StringSequence.Traits), commonEnd);
    }

    @Test
    public void testCommonMiddleFindsEnd3() {
        Sequence<String> commonEnd = ignoreCase("The dog jumped over the fence", "The dog jumped over the fence",
                "The dog jumped over the fence", "The dog jumped over the fence").commonMiddle();
        assertEquals(new Sequence<>(splitWords("The dog jumped over the fence"), StringSequence.Traits),
                commonEnd);
    }

    @Test
    public void testSliceStart() {
        List<Sequences<String>> slices = StringSequences.of( //
                "The dog jumped over the fence, did he?", //
                "The dog jumped under the fence", //
                "The dog jumped over my sofa", //
                "The dog looked over the fence, did he?");
        assertFalse(slices.isEmpty());
    }

    @Test
    public void testSliceStartSameSize() {
        List<Sequences<String>> slices = StringSequences.of( //
                "The dog jumped over the fence,", //
                "The dog jumped under the fence", //
                "The dog jumped over my sofa", //
                "The dog looked over the fence");
        assertFalse(slices.isEmpty());
    }

    @Test
    public void testSliceMiddle() {
        List<Sequences<String>> slices = StringSequences.of( //
                "The dog jumped over the fence, did he?", //
                "The dog jumped over the fence", //
                "My cat jumped over my sofa", //
                "The dog looked over the fence, did he?");
        assertFalse(slices.isEmpty());
    }

    @Test
    public void testSliceMiddleSameSize() {
        List<Sequences<String>> slices = StringSequences.of( //
                "The dog jumped over the fence", //
                "The dog jumped over the fence", //
                "My cat jumped over my sofa", //
                "The dog looked over the fence");
        assertFalse(slices.isEmpty());
    }

    @Test
    public void testSliceEnd() {
        List<Sequences<String>> slices = StringSequences.of( //
                "The dog jumped over the fence, did he?", //
                "The dog jumped over the fence", //
                "Your cat crawled under my fence", //
                "The dog looked over the fence, did he?");
        assertFalse(slices.isEmpty());
    }

    @Test
    public void testSliceEndSameSize() {
        List<Sequences<String>> slices = StringSequences.of( //
                "The dog jumped over the fence", //
                "The dog jumped over the fence", //
                "Your cat crawled under my fence", //
                "The dog looked over the fence");
        assertFalse(slices.isEmpty());
    }

    @Test
    public void testSliceAllDifferent() {
        List<Sequences<String>> slices = StringSequences.of( //
                "No dog jumped over the fence, you know?", //
                "The dog jumped under the fence", //
                "The cat looked over my sofa", //
                "The dog jumped over the fence, sure?");
        assertFalse(slices.isEmpty());
    }

    @Test
    public void testSliceAllSame() {
        List<Sequences<String>> slices = StringSequences.of( //
                "The dog jumped over the fence.", //
                "The dog jumped over the fence", //
                "The dog jumped over the fence.", //
                "The dog jumped over the fence.");
        assertFalse(slices.isEmpty());
    }

}
