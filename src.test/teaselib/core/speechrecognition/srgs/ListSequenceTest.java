package teaselib.core.speechrecognition.srgs;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.junit.Test;

public class ListSequenceTest {

    static String[] splitWords(String string) {
        return ListSequenceUtil.splitWords(string);
    }

    @Test
    public void testSequenceStart() {
        ListSequence<String> s1 = new ListSequence<>(splitWords("The dog jumped over the fence"));
        ListSequence<String> s2 = new ListSequence<>(splitWords("The dog"));

        assertTrue(s1.startsWith(s2));
        assertFalse(s1.endsWith(s2));
        assertEquals(0, s1.indexOf(s2));
        assertEquals(0, s1.lastIndexOf(s2));
    }

    @Test
    public void testSequenceEnd() {
        ListSequence<String> s1 = new ListSequence<>(splitWords("The dog jumped over the fence"));
        ListSequence<String> s2 = new ListSequence<>(splitWords("the fence"));

        assertTrue(s1.endsWith(s2));
        assertFalse(s1.startsWith(s2));
        assertEquals(s1.size() - s2.size(), s1.indexOf(s2));
        assertEquals(s1.size() - s2.size(), s1.lastIndexOf(s2));
    }

    @Test
    public void testSequenceMid() {
        ListSequence<String> s1 = new ListSequence<>(splitWords("The dog jumped over the fence"));
        ListSequence<String> s2 = new ListSequence<>(splitWords("dog jumped"));

        assertFalse(s1.startsWith(s2));
        assertFalse(s1.endsWith(s2));
        assertEquals(1, s1.indexOf(s2));
        assertEquals(1, s1.lastIndexOf(s2));
    }

    @Test
    public void testCommonStartNoMatchSameLengths() {
        ListSequence<String> s1 = new ListSequence<>(splitWords("My dog jumped over the fence"));
        ListSequence<String> s2 = new ListSequence<>(splitWords("The dog looked over the fence"));
        ListSequence<String> s3 = new ListSequence<>(splitWords("The dog jumped over the sofa"));
        ListSequence<String> s4 = new ListSequence<>(splitWords("The dog jumped over the fence"));

        ListSequence<String> commonStart = new ListSequences<>(s1, s2, s3, s4).commonStart();
        assertEquals(new ListSequence<>(splitWords("")), commonStart);
    }

    @Test
    public void testCommonStartNoMatchDifferentLengths() {
        ListSequence<String> s1 = new ListSequence<>(splitWords("My dog jumped over the fence"));
        ListSequence<String> s2 = new ListSequence<>(splitWords("The dog looked over the fence Schmidt"));
        ListSequence<String> s3 = new ListSequence<>(splitWords("The dog jumped over the sofa"));
        ListSequence<String> s4 = new ListSequence<>(splitWords("One two three"));

        ListSequence<String> commonStart = new ListSequences<>(s1, s2, s3, s4).commonStart();
        assertEquals(new ListSequence<>(splitWords("")), commonStart);
    }

    @Test
    public void testCommonStart1() {
        ListSequence<String> s1 = new ListSequence<>(splitWords("The dog jumped over the fence"));
        ListSequence<String> s2 = new ListSequence<>(splitWords("The dog looked over the fence"));
        ListSequence<String> s3 = new ListSequence<>(splitWords("The dog jumped over the sofa"));
        ListSequence<String> s4 = new ListSequence<>(splitWords("The dog jumped over the fence"));

        ListSequence<String> commonStart = new ListSequences<>(s1, s2, s3, s4).commonStart();
        assertEquals(new ListSequence<>(splitWords("The dog")), commonStart);
    }

    @Test
    public void testCommonStart2() {
        ListSequence<String> s1 = new ListSequence<>(splitWords("The dog jumped over the fence"));
        ListSequence<String> s2 = new ListSequence<>(splitWords("The dog jumped over the fence"));
        ListSequence<String> s3 = new ListSequence<>(splitWords("The dog jumped over the sofa"));
        ListSequence<String> s4 = new ListSequence<>(splitWords("The dog jumped over my fence"));

        ListSequence<String> commonStart = new ListSequences<>(s1, s2, s3, s4).commonStart();
        assertEquals(new ListSequence<>(splitWords("The dog jumped over")), commonStart);
    }

    @Test
    public void testCommonStart3() {
        ListSequence<String> s1 = new ListSequence<>(splitWords("The dog jumped over the fence"));
        ListSequence<String> s2 = new ListSequence<>(splitWords("The dog jumped over the fence"));
        ListSequence<String> s3 = new ListSequence<>(splitWords("The dog jumped over the fence"));
        ListSequence<String> s4 = new ListSequence<>(splitWords("The dog jumped over the fence"));

        ListSequence<String> commonStart = new ListSequences<>(s1, s2, s3, s4).commonStart();
        assertEquals(new ListSequence<>(splitWords("The dog jumped over the fence")), commonStart);
    }

    @Test
    public void testCommonEndSameLengths() {
        ListSequence<String> s1 = new ListSequence<>(splitWords("My dog jumped over the fence"));
        ListSequence<String> s2 = new ListSequence<>(splitWords("The dog looked over the fence"));
        ListSequence<String> s3 = new ListSequence<>(splitWords("The dog jumped over the sofa"));
        ListSequence<String> s4 = new ListSequence<>(splitWords("The dog jumped over the fence"));

        ListSequence<String> commonEnd = new ListSequences<>(s1, s2, s3, s4).commonEnd();
        assertEquals(new ListSequence<>(splitWords("")), commonEnd);
    }

    @Test
    public void testCommonEndDifferentLengths() {
        ListSequence<String> s1 = new ListSequence<>(splitWords("My dog jumped over the fence"));
        ListSequence<String> s2 = new ListSequence<>(splitWords("The dog looked over the fence"));
        ListSequence<String> s3 = new ListSequence<>(splitWords("The dog jumped over the fence Schmidt"));
        ListSequence<String> s4 = new ListSequence<>(splitWords("One two three"));

        ListSequence<String> commonEnd = new ListSequences<>(s1, s2, s3, s4).commonEnd();
        assertEquals(new ListSequence<>(splitWords("")), commonEnd);
    }

    @Test
    public void testCommonEnd1() {
        ListSequence<String> s1 = new ListSequence<>(splitWords("My dog jumped over the fence"));
        ListSequence<String> s2 = new ListSequence<>(splitWords("The dog looked over the fence"));
        ListSequence<String> s3 = new ListSequence<>(splitWords("The dog jumped over the fence"));
        ListSequence<String> s4 = new ListSequence<>(splitWords("The dog jumped over the fence"));

        ListSequence<String> commonEnd = new ListSequences<>(s1, s2, s3, s4).commonEnd();
        assertEquals(new ListSequence<>(splitWords("over the fence")), commonEnd);
    }

    @Test
    public void testCommonEnd2() {
        ListSequence<String> s1 = new ListSequence<>(splitWords("The dog jumped over the fence"));
        ListSequence<String> s2 = new ListSequence<>(splitWords("The dog jumped over the fence"));
        ListSequence<String> s3 = new ListSequence<>(splitWords("The cat jumped over the fence"));
        ListSequence<String> s4 = new ListSequence<>(splitWords("The dog jumped over the fence"));

        ListSequence<String> commonEnd = new ListSequences<>(s1, s2, s3, s4).commonEnd();
        assertEquals(new ListSequence<>(splitWords("jumped over the fence")), commonEnd);
    }

    @Test
    public void testCommonEnd3() {
        ListSequence<String> s1 = new ListSequence<>(splitWords("The dog jumped over the fence"));
        ListSequence<String> s2 = new ListSequence<>(splitWords("The dog jumped over the fence"));
        ListSequence<String> s3 = new ListSequence<>(splitWords("The dog jumped over the fence"));
        ListSequence<String> s4 = new ListSequence<>(splitWords("The dog jumped over the fence"));

        ListSequence<String> commonEnd = new ListSequences<>(s1, s2, s3, s4).commonEnd();
        assertEquals(new ListSequence<>(splitWords("The dog jumped over the fence")), commonEnd);
    }

    @Test
    public void testCommonMiddleNoMatchSameLengths() {
        ListSequence<String> s1 = new ListSequence<>(splitWords("Your dog jumped over your fence"));
        ListSequence<String> s2 = new ListSequence<>(splitWords("The dog looked over the fence"));
        ListSequence<String> s3 = new ListSequence<>(splitWords("The cat jumped under the sofa"));
        ListSequence<String> s4 = new ListSequence<>(splitWords("The dog jumped over my fence"));

        ListSequence<String> commonMiddle = new ListSequences<>(s1, s2, s3, s4).commonMiddle();
        assertEquals(new ListSequence<>(splitWords("")), commonMiddle);
    }

    @Test
    public void testCommonMiddleNoMatchDifferentLengths() {
        ListSequence<String> s1 = new ListSequence<>(splitWords("Your dog jumped over your fence"));
        ListSequence<String> s2 = new ListSequence<>(splitWords("The dog looked over the fence"));
        ListSequence<String> s3 = new ListSequence<>(splitWords("The cat jumped under the sofa"));
        ListSequence<String> s4 = new ListSequence<>(splitWords("One two three"));

        ListSequence<String> commonMiddle = new ListSequences<>(s1, s2, s3, s4).commonMiddle();
        assertEquals(new ListSequence<>(splitWords("")), commonMiddle);
    }

    @Test
    public void testCommonMiddle1() {
        ListSequence<String> s1 = new ListSequence<>(splitWords("My dog jumped over the fence"));
        ListSequence<String> s2 = new ListSequence<>(splitWords("The cat jumped over the fence"));
        ListSequence<String> s3 = new ListSequence<>(splitWords("The dog jumped over your sofa"));
        ListSequence<String> s4 = new ListSequence<>(splitWords("The dog jumped over the fence"));

        ListSequence<String> commonMiddle = new ListSequences<>(s1, s2, s3, s4).commonMiddle();
        assertEquals(new ListSequence<>(splitWords("jumped over")), commonMiddle);
    }

    @Test
    public void testCommonMiddle2a() {
        ListSequence<String> s1 = new ListSequence<>(splitWords("The dog jumped over the fence"));
        ListSequence<String> s2 = new ListSequence<>(splitWords("The dog jumped over the fence"));
        ListSequence<String> s3 = new ListSequence<>(splitWords("Your cat jumped over your sofa"));
        ListSequence<String> s4 = new ListSequence<>(splitWords("The dog jumped over the fence"));

        ListSequence<String> commonMiddle = new ListSequences<>(s1, s2, s3, s4).commonMiddle();
        assertEquals(new ListSequence<>(splitWords("jumped over")), commonMiddle);
    }

    @Test
    public void testCommonMiddle2b() {
        ListSequence<String> s1 = new ListSequence<>(splitWords("The dog jumped over the fence"));
        ListSequence<String> s2 = new ListSequence<>(splitWords("The dog jumped over the sofa"));
        ListSequence<String> s3 = new ListSequence<>(splitWords("The cat jumped over the fence"));
        ListSequence<String> s4 = new ListSequence<>(splitWords("The dog jumped over the fence"));

        ListSequence<String> commonMiddle = new ListSequences<>(s1, s2, s3, s4).commonMiddle();
        assertEquals(new ListSequence<>(splitWords("jumped over the")), commonMiddle);
    }

    @Test
    public void testCommonMiddle3() {
        ListSequence<String> s1 = new ListSequence<>(splitWords("The dog jumped over the fence"));
        ListSequence<String> s2 = new ListSequence<>(splitWords("The dog jumped over the fence"));
        ListSequence<String> s3 = new ListSequence<>(splitWords("The dog jumped over the fence"));
        ListSequence<String> s4 = new ListSequence<>(splitWords("The dog jumped over the fence"));

        ListSequence<String> commonMiddle = new ListSequences<>(s1, s2, s3, s4).commonMiddle();
        assertEquals(new ListSequence<>(splitWords("The dog jumped over the fence")), commonMiddle);
    }

    @Test
    public void testCommonMiddleFindsEnd1() {
        ListSequence<String> s1 = new ListSequence<>(splitWords("My dog jumped over the fence"));
        ListSequence<String> s2 = new ListSequence<>(splitWords("The dog looked over the fence"));
        ListSequence<String> s3 = new ListSequence<>(splitWords("The dog jumped over the fence"));
        ListSequence<String> s4 = new ListSequence<>(splitWords("The dog jumped over the fence"));

        ListSequence<String> commonEnd = new ListSequences<>(s1, s2, s3, s4).commonMiddle();
        assertEquals(new ListSequence<>(splitWords("over the fence")), commonEnd);
    }

    @Test
    public void testCommonMiddleFindsEnd2() {
        ListSequence<String> s1 = new ListSequence<>(splitWords("The dog jumped over the fence"));
        ListSequence<String> s2 = new ListSequence<>(splitWords("The dog jumped over the fence"));
        ListSequence<String> s3 = new ListSequence<>(splitWords("The cat jumped over the fence"));
        ListSequence<String> s4 = new ListSequence<>(splitWords("The dog jumped over the fence"));

        ListSequence<String> commonEnd = new ListSequences<>(s1, s2, s3, s4).commonMiddle();
        assertEquals(new ListSequence<>(splitWords("jumped over the fence")), commonEnd);
    }

    @Test
    public void testCommonMiddleFindsEnd3() {
        ListSequence<String> s1 = new ListSequence<>(splitWords("The dog jumped over the fence"));
        ListSequence<String> s2 = new ListSequence<>(splitWords("The dog jumped over the fence"));
        ListSequence<String> s3 = new ListSequence<>(splitWords("The dog jumped over the fence"));
        ListSequence<String> s4 = new ListSequence<>(splitWords("The dog jumped over the fence"));

        ListSequence<String> commonEnd = new ListSequences<>(s1, s2, s3, s4).commonMiddle();
        assertEquals(new ListSequence<>(splitWords("The dog jumped over the fence")), commonEnd);
    }

    @Test
    public void testSliceStart() {
        List<ListSequences<String>> slices = ListSequenceUtil.slice( //
                "The dog jumped over the fence, did he?", //
                "The dog jumped under the fence", //
                "The dog jumped over my sofa", //
                "The dog looked over the fence, did he?");
        assertFalse(slices.isEmpty());
    }

    @Test
    public void testSliceStartSameSize() {
        List<ListSequences<String>> slices = ListSequenceUtil.slice( //
                "The dog jumped over the fence,", //
                "The dog jumped under the fence", //
                "The dog jumped over my sofa", //
                "The dog looked over the fence");
        assertFalse(slices.isEmpty());
    }

    @Test
    public void testSliceMiddle() {
        List<ListSequences<String>> slices = ListSequenceUtil.slice( //
                "The dog jumped over the fence, did he?", //
                "The dog jumped over the fence", //
                "My cat jumped over my sofa", //
                "The dog looked over the fence, did he?");
        assertFalse(slices.isEmpty());
    }

    @Test
    public void testSliceMiddleSameSize() {
        List<ListSequences<String>> slices = ListSequenceUtil.slice( //
                "The dog jumped over the fence", //
                "The dog jumped over the fence", //
                "My cat jumped over my sofa", //
                "The dog looked over the fence");
        assertFalse(slices.isEmpty());
    }

    @Test
    public void testSliceEnd() {
        List<ListSequences<String>> slices = ListSequenceUtil.slice( //
                "The dog jumped over the fence, did he?", //
                "The dog jumped over the fence", //
                "Your cat crawled under my fence", //
                "The dog looked over the fence, did he?");
        assertFalse(slices.isEmpty());
    }

    @Test
    public void testSliceEndSameSize() {
        List<ListSequences<String>> slices = ListSequenceUtil.slice( //
                "The dog jumped over the fence", //
                "The dog jumped over the fence", //
                "Your cat crawled under my fence", //
                "The dog looked over the fence");
        assertFalse(slices.isEmpty());
    }

    @Test
    public void testSliceAllDifferent() {
        List<ListSequences<String>> slices = ListSequenceUtil.slice( //
                "No dog jumped over the fence, you know?", //
                "The dog jumped under the fence", //
                "The cat looked over my sofa", //
                "The dog jumped over the fence, sure?");
        assertFalse(slices.isEmpty());
    }

    @Test
    public void testSliceAllSame() {
        List<ListSequences<String>> slices = ListSequenceUtil.slice( //
                "The dog jumped over the fence.", //
                "The dog jumped over the fence", //
                "The dog jumped over the fence.", //
                "The dog jumped over the fence.");
        assertFalse(slices.isEmpty());
    }

}
