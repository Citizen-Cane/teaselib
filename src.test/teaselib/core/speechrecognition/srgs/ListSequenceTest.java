package teaselib.core.speechrecognition.srgs;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.junit.Test;

public class ListSequenceTest {

    static String[] splitWords(String string) {
        return StringSequence.splitWords(string);
    }

    @Test
    public void testSequenceStart() {
        Sequence<String> s1 = new Sequence<>(splitWords("The dog jumped over the fence"));
        Sequence<String> s2 = new Sequence<>(splitWords("The dog"));

        assertTrue(s1.startsWith(s2));
        assertFalse(s1.endsWith(s2));
        assertEquals(0, s1.indexOf(s2));
        assertEquals(0, s1.lastIndexOf(s2));
    }

    @Test
    public void testSequenceEnd() {
        Sequence<String> s1 = new Sequence<>(splitWords("The dog jumped over the fence"));
        Sequence<String> s2 = new Sequence<>(splitWords("the fence"));

        assertTrue(s1.endsWith(s2));
        assertFalse(s1.startsWith(s2));
        assertEquals(s1.size() - s2.size(), s1.indexOf(s2));
        assertEquals(s1.size() - s2.size(), s1.lastIndexOf(s2));
    }

    @Test
    public void testSequenceMid() {
        Sequence<String> s1 = new Sequence<>(splitWords("The dog jumped over the fence"));
        Sequence<String> s2 = new Sequence<>(splitWords("dog jumped"));

        assertFalse(s1.startsWith(s2));
        assertFalse(s1.endsWith(s2));
        assertEquals(1, s1.indexOf(s2));
        assertEquals(1, s1.lastIndexOf(s2));
    }

    @Test
    public void testCommonStartNoMatchSameLengths() {
        Sequence<String> commonStart = StringSequences.ignoreCase("My dog jumped over the fence",
                "The dog looked over the fence", "The dog jumped over the sofa", "The dog jumped over the fence")
                .commonStart();
        assertEquals(new Sequence<>(splitWords("")), commonStart);
    }

    @Test
    public void testCommonStartNoMatchDifferentLengths() {
        Sequence<String> commonStart = StringSequences.ignoreCase("My dog jumped over the fence",
                "The dog looked over the fence Schmidt", "The dog jumped over the sofa", "One two three").commonStart();
        assertEquals(new Sequence<>(splitWords("")), commonStart);
    }

    @Test
    public void testCommonStart1() {
        Sequence<String> commonStart = StringSequences.ignoreCase("The dog jumped over the fence",
                "The dog looked over the fence", "The dog jumped over the sofa", "The dog jumped over the fence")
                .commonStart();
        assertEquals(new Sequence<>(splitWords("The dog")), commonStart);
    }

    @Test
    public void testCommonStart2() {
        Sequence<String> commonStart = StringSequences.ignoreCase("The dog jumped over the fence",
                "The dog jumped over the fence", "The dog jumped over the sofa", "The dog jumped over my fence")
                .commonStart();
        assertEquals(new Sequence<>(splitWords("The dog jumped over")), commonStart);
    }

    @Test
    public void testCommonStart3() {
        Sequence<String> commonStart = StringSequences.ignoreCase("The dog jumped over the fence",
                "The dog jumped over the fence", "The dog jumped over the fence", "The dog jumped over the fence")
                .commonStart();
        assertEquals(new Sequence<>(splitWords("The dog jumped over the fence")), commonStart);
    }

    @Test
    public void testCommonEndSameLengths() {
        Sequence<String> commonEnd = StringSequences.ignoreCase("My dog jumped over the fence",
                "The dog looked over the fence", "The dog jumped over the sofa", "The dog jumped over the fence")
                .commonEnd();
        assertEquals(new Sequence<>(splitWords("")), commonEnd);
    }

    @Test
    public void testCommonEndDifferentLengths() {
        Sequence<String> commonEnd = StringSequences.ignoreCase("My dog jumped over the fence",
                "The dog looked over the fence", "The dog jumped over the fence fast", "My dog jumped over the fence")
                .commonEnd();
        assertEquals(new Sequence<>(splitWords("")), commonEnd);
    }

    @Test
    public void testCommonEnd1() {
        Sequence<String> commonEnd = StringSequences.ignoreCase("My dog jumped over the fence",
                "The dog looked over the fence", "The dog jumped over the fence", "The dog jumped over the fence")
                .commonEnd();
        assertEquals(new Sequence<>(splitWords("over the fence")), commonEnd);
    }

    @Test
    public void testCommonEnd2() {
        Sequence<String> commonEnd = StringSequences.ignoreCase("The dog jumped over the fence",
                "The dog jumped over the fence", "The cat jumped over the fence", "The dog jumped over the fence")
                .commonEnd();
        assertEquals(new Sequence<>(splitWords("jumped over the fence")), commonEnd);
    }

    @Test
    public void testCommonEnd3() {
        Sequence<String> commonEnd = StringSequences.ignoreCase("The dog jumped over the fence",
                "The dog jumped over the fence", "The dog jumped over the fence", "The dog jumped over the fence")
                .commonEnd();
        assertEquals(new Sequence<>(splitWords("The dog jumped over the fence")), commonEnd);
    }

    @Test
    public void testCommonMiddleNoMatchSameLengths() {
        Sequence<String> commonMiddle = StringSequences.ignoreCase("Your dog jumped over your fence",
                "The dog looked over the fence", "The cat jumped under the sofa", "The dog jumped over my fence")
                .commonMiddle();
        assertEquals(new Sequence<>(splitWords("")), commonMiddle);
    }

    @Test
    public void testCommonMiddleNoMatchDifferentLengths() {
        Sequence<String> commonMiddle = StringSequences.ignoreCase("Your dog jumped over your fence",
                "The dog looked over the fence", "The cat jumped under the sofa", "One two three").commonMiddle();
        assertEquals(new Sequence<>(splitWords("")), commonMiddle);
    }

    @Test
    public void testCommonMiddle1() {
        Sequence<String> commonMiddle = StringSequences.ignoreCase("My dog jumped over the fence",
                "The cat jumped over the fence", "The dog jumped over your sofa", "The dog jumped over the fence")
                .commonMiddle();
        assertEquals(new Sequence<>(splitWords("jumped over")), commonMiddle);
    }

    @Test
    public void testCommonMiddle2a() {
        Sequence<String> commonMiddle = StringSequences.ignoreCase("The dog jumped over the fence",
                "The dog jumped over the fence", "Your cat jumped over your sofa", "The dog jumped over the fence")
                .commonMiddle();
        assertEquals(new Sequence<>(splitWords("jumped over")), commonMiddle);
    }

    @Test
    public void testCommonMiddle2b() {
        Sequence<String> commonMiddle = StringSequences.ignoreCase("The dog jumped over the fence",
                "The dog jumped over the sofa", "The cat jumped over the fence", "The dog jumped over the fence")
                .commonMiddle();
        assertEquals(new Sequence<>(splitWords("jumped over the")), commonMiddle);
    }

    @Test
    public void testCommonMiddle3() {
        Sequence<String> commonMiddle = StringSequences.ignoreCase("The dog jumped over the fence",
                "The dog jumped over the fence", "The dog jumped over the fence", "The dog jumped over the fence")
                .commonMiddle();
        assertEquals(new Sequence<>(splitWords("The dog jumped over the fence")), commonMiddle);
    }

    @Test
    public void testCommonMiddleFindsEnd1() {
        Sequence<String> commonEnd = StringSequences.ignoreCase("My dog jumped over the fence",
                "The dog looked over the fence", "The dog jumped over the fence", "The dog jumped over the fence")
                .commonMiddle();
        assertEquals(new Sequence<>(splitWords("over the fence")), commonEnd);
    }

    @Test
    public void testCommonMiddleFindsEnd2() {
        Sequence<String> commonEnd = StringSequences.ignoreCase("The dog jumped over the fence",
                "The dog jumped over the fence", "The cat jumped over the fence", "The dog jumped over the fence")
                .commonMiddle();
        assertEquals(new Sequence<>(splitWords("jumped over the fence")), commonEnd);
    }

    @Test
    public void testCommonMiddleFindsEnd3() {
        Sequence<String> commonEnd = StringSequences.ignoreCase("The dog jumped over the fence",
                "The dog jumped over the fence", "The dog jumped over the fence", "The dog jumped over the fence")
                .commonMiddle();
        assertEquals(new Sequence<>(splitWords("The dog jumped over the fence")), commonEnd);
    }

    @Test
    public void testSliceStart() {
        List<StringSequences> slices = StringSequences.slice( //
                "The dog jumped over the fence, did he?", //
                "The dog jumped under the fence", //
                "The dog jumped over my sofa", //
                "The dog looked over the fence, did he?");
        assertFalse(slices.isEmpty());
    }

    @Test
    public void testSliceStartSameSize() {
        List<StringSequences> slices = StringSequences.slice( //
                "The dog jumped over the fence,", //
                "The dog jumped under the fence", //
                "The dog jumped over my sofa", //
                "The dog looked over the fence");
        assertFalse(slices.isEmpty());
    }

    @Test
    public void testSliceMiddle() {
        List<StringSequences> slices = StringSequences.slice( //
                "The dog jumped over the fence, did he?", //
                "The dog jumped over the fence", //
                "My cat jumped over my sofa", //
                "The dog looked over the fence, did he?");
        assertFalse(slices.isEmpty());
    }

    @Test
    public void testSliceMiddleSameSize() {
        List<StringSequences> slices = StringSequences.slice( //
                "The dog jumped over the fence", //
                "The dog jumped over the fence", //
                "My cat jumped over my sofa", //
                "The dog looked over the fence");
        assertFalse(slices.isEmpty());
    }

    @Test
    public void testSliceEnd() {
        List<StringSequences> slices = StringSequences.slice( //
                "The dog jumped over the fence, did he?", //
                "The dog jumped over the fence", //
                "Your cat crawled under my fence", //
                "The dog looked over the fence, did he?");
        assertFalse(slices.isEmpty());
    }

    @Test
    public void testSliceEndSameSize() {
        List<StringSequences> slices = StringSequences.slice( //
                "The dog jumped over the fence", //
                "The dog jumped over the fence", //
                "Your cat crawled under my fence", //
                "The dog looked over the fence");
        assertFalse(slices.isEmpty());
    }

    @Test
    public void testSliceAllDifferent() {
        List<StringSequences> slices = StringSequences.slice( //
                "No dog jumped over the fence, you know?", //
                "The dog jumped under the fence", //
                "The cat looked over my sofa", //
                "The dog jumped over the fence, sure?");
        assertFalse(slices.isEmpty());
    }

    @Test
    public void testSliceAllSame() {
        List<StringSequences> slices = StringSequences.slice( //
                "The dog jumped over the fence.", //
                "The dog jumped over the fence", //
                "The dog jumped over the fence.", //
                "The dog jumped over the fence.");
        assertFalse(slices.isEmpty());
    }

}
