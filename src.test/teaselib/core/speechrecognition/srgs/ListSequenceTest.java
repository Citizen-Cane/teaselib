package teaselib.core.speechrecognition.srgs;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.junit.Test;

public class ListSequenceTest {

    static String[] splitWords(String string) {
        return SequenceUtil.splitWords(string);
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
        Sequence<String> s1 = new Sequence<>(splitWords("My dog jumped over the fence"));
        Sequence<String> s2 = new Sequence<>(splitWords("The dog looked over the fence"));
        Sequence<String> s3 = new Sequence<>(splitWords("The dog jumped over the sofa"));
        Sequence<String> s4 = new Sequence<>(splitWords("The dog jumped over the fence"));

        Sequence<String> commonStart = new Sequences<>(s1, s2, s3, s4).commonStart();
        assertEquals(new Sequence<>(splitWords("")), commonStart);
    }

    @Test
    public void testCommonStartNoMatchDifferentLengths() {
        Sequence<String> s1 = new Sequence<>(splitWords("My dog jumped over the fence"));
        Sequence<String> s2 = new Sequence<>(splitWords("The dog looked over the fence Schmidt"));
        Sequence<String> s3 = new Sequence<>(splitWords("The dog jumped over the sofa"));
        Sequence<String> s4 = new Sequence<>(splitWords("One two three"));

        Sequence<String> commonStart = new Sequences<>(s1, s2, s3, s4).commonStart();
        assertEquals(new Sequence<>(splitWords("")), commonStart);
    }

    @Test
    public void testCommonStart1() {
        Sequence<String> s1 = new Sequence<>(splitWords("The dog jumped over the fence"));
        Sequence<String> s2 = new Sequence<>(splitWords("The dog looked over the fence"));
        Sequence<String> s3 = new Sequence<>(splitWords("The dog jumped over the sofa"));
        Sequence<String> s4 = new Sequence<>(splitWords("The dog jumped over the fence"));

        Sequence<String> commonStart = new Sequences<>(s1, s2, s3, s4).commonStart();
        assertEquals(new Sequence<>(splitWords("The dog")), commonStart);
    }

    @Test
    public void testCommonStart2() {
        Sequence<String> s1 = new Sequence<>(splitWords("The dog jumped over the fence"));
        Sequence<String> s2 = new Sequence<>(splitWords("The dog jumped over the fence"));
        Sequence<String> s3 = new Sequence<>(splitWords("The dog jumped over the sofa"));
        Sequence<String> s4 = new Sequence<>(splitWords("The dog jumped over my fence"));

        Sequence<String> commonStart = new Sequences<>(s1, s2, s3, s4).commonStart();
        assertEquals(new Sequence<>(splitWords("The dog jumped over")), commonStart);
    }

    @Test
    public void testCommonStart3() {
        Sequence<String> s1 = new Sequence<>(splitWords("The dog jumped over the fence"));
        Sequence<String> s2 = new Sequence<>(splitWords("The dog jumped over the fence"));
        Sequence<String> s3 = new Sequence<>(splitWords("The dog jumped over the fence"));
        Sequence<String> s4 = new Sequence<>(splitWords("The dog jumped over the fence"));

        Sequence<String> commonStart = new Sequences<>(s1, s2, s3, s4).commonStart();
        assertEquals(new Sequence<>(splitWords("The dog jumped over the fence")), commonStart);
    }

    @Test
    public void testCommonEndSameLengths() {
        Sequence<String> s1 = new Sequence<>(splitWords("My dog jumped over the fence"));
        Sequence<String> s2 = new Sequence<>(splitWords("The dog looked over the fence"));
        Sequence<String> s3 = new Sequence<>(splitWords("The dog jumped over the sofa"));
        Sequence<String> s4 = new Sequence<>(splitWords("The dog jumped over the fence"));

        Sequence<String> commonEnd = new Sequences<>(s1, s2, s3, s4).commonEnd();
        assertEquals(new Sequence<>(splitWords("")), commonEnd);
    }

    @Test
    public void testCommonEndDifferentLengths() {
        Sequence<String> s1 = new Sequence<>(splitWords("My dog jumped over the fence"));
        Sequence<String> s2 = new Sequence<>(splitWords("The dog looked over the fence"));
        Sequence<String> s3 = new Sequence<>(splitWords("The dog jumped over the fence Schmidt"));
        Sequence<String> s4 = new Sequence<>(splitWords("One two three"));

        Sequence<String> commonEnd = new Sequences<>(s1, s2, s3, s4).commonEnd();
        assertEquals(new Sequence<>(splitWords("")), commonEnd);
    }

    @Test
    public void testCommonEnd1() {
        Sequence<String> s1 = new Sequence<>(splitWords("My dog jumped over the fence"));
        Sequence<String> s2 = new Sequence<>(splitWords("The dog looked over the fence"));
        Sequence<String> s3 = new Sequence<>(splitWords("The dog jumped over the fence"));
        Sequence<String> s4 = new Sequence<>(splitWords("The dog jumped over the fence"));

        Sequence<String> commonEnd = new Sequences<>(s1, s2, s3, s4).commonEnd();
        assertEquals(new Sequence<>(splitWords("over the fence")), commonEnd);
    }

    @Test
    public void testCommonEnd2() {
        Sequence<String> s1 = new Sequence<>(splitWords("The dog jumped over the fence"));
        Sequence<String> s2 = new Sequence<>(splitWords("The dog jumped over the fence"));
        Sequence<String> s3 = new Sequence<>(splitWords("The cat jumped over the fence"));
        Sequence<String> s4 = new Sequence<>(splitWords("The dog jumped over the fence"));

        Sequence<String> commonEnd = new Sequences<>(s1, s2, s3, s4).commonEnd();
        assertEquals(new Sequence<>(splitWords("jumped over the fence")), commonEnd);
    }

    @Test
    public void testCommonEnd3() {
        Sequence<String> s1 = new Sequence<>(splitWords("The dog jumped over the fence"));
        Sequence<String> s2 = new Sequence<>(splitWords("The dog jumped over the fence"));
        Sequence<String> s3 = new Sequence<>(splitWords("The dog jumped over the fence"));
        Sequence<String> s4 = new Sequence<>(splitWords("The dog jumped over the fence"));

        Sequence<String> commonEnd = new Sequences<>(s1, s2, s3, s4).commonEnd();
        assertEquals(new Sequence<>(splitWords("The dog jumped over the fence")), commonEnd);
    }

    @Test
    public void testCommonMiddleNoMatchSameLengths() {
        Sequence<String> s1 = new Sequence<>(splitWords("Your dog jumped over your fence"));
        Sequence<String> s2 = new Sequence<>(splitWords("The dog looked over the fence"));
        Sequence<String> s3 = new Sequence<>(splitWords("The cat jumped under the sofa"));
        Sequence<String> s4 = new Sequence<>(splitWords("The dog jumped over my fence"));

        Sequence<String> commonMiddle = new Sequences<>(s1, s2, s3, s4).commonMiddle();
        assertEquals(new Sequence<>(splitWords("")), commonMiddle);
    }

    @Test
    public void testCommonMiddleNoMatchDifferentLengths() {
        Sequence<String> s1 = new Sequence<>(splitWords("Your dog jumped over your fence"));
        Sequence<String> s2 = new Sequence<>(splitWords("The dog looked over the fence"));
        Sequence<String> s3 = new Sequence<>(splitWords("The cat jumped under the sofa"));
        Sequence<String> s4 = new Sequence<>(splitWords("One two three"));

        Sequence<String> commonMiddle = new Sequences<>(s1, s2, s3, s4).commonMiddle();
        assertEquals(new Sequence<>(splitWords("")), commonMiddle);
    }

    @Test
    public void testCommonMiddle1() {
        Sequence<String> s1 = new Sequence<>(splitWords("My dog jumped over the fence"));
        Sequence<String> s2 = new Sequence<>(splitWords("The cat jumped over the fence"));
        Sequence<String> s3 = new Sequence<>(splitWords("The dog jumped over your sofa"));
        Sequence<String> s4 = new Sequence<>(splitWords("The dog jumped over the fence"));

        Sequence<String> commonMiddle = new Sequences<>(s1, s2, s3, s4).commonMiddle();
        assertEquals(new Sequence<>(splitWords("jumped over")), commonMiddle);
    }

    @Test
    public void testCommonMiddle2a() {
        Sequence<String> s1 = new Sequence<>(splitWords("The dog jumped over the fence"));
        Sequence<String> s2 = new Sequence<>(splitWords("The dog jumped over the fence"));
        Sequence<String> s3 = new Sequence<>(splitWords("Your cat jumped over your sofa"));
        Sequence<String> s4 = new Sequence<>(splitWords("The dog jumped over the fence"));

        Sequence<String> commonMiddle = new Sequences<>(s1, s2, s3, s4).commonMiddle();
        assertEquals(new Sequence<>(splitWords("jumped over")), commonMiddle);
    }

    @Test
    public void testCommonMiddle2b() {
        Sequence<String> s1 = new Sequence<>(splitWords("The dog jumped over the fence"));
        Sequence<String> s2 = new Sequence<>(splitWords("The dog jumped over the sofa"));
        Sequence<String> s3 = new Sequence<>(splitWords("The cat jumped over the fence"));
        Sequence<String> s4 = new Sequence<>(splitWords("The dog jumped over the fence"));

        Sequence<String> commonMiddle = new Sequences<>(s1, s2, s3, s4).commonMiddle();
        assertEquals(new Sequence<>(splitWords("jumped over the")), commonMiddle);
    }

    @Test
    public void testCommonMiddle3() {
        Sequence<String> s1 = new Sequence<>(splitWords("The dog jumped over the fence"));
        Sequence<String> s2 = new Sequence<>(splitWords("The dog jumped over the fence"));
        Sequence<String> s3 = new Sequence<>(splitWords("The dog jumped over the fence"));
        Sequence<String> s4 = new Sequence<>(splitWords("The dog jumped over the fence"));

        Sequence<String> commonMiddle = new Sequences<>(s1, s2, s3, s4).commonMiddle();
        assertEquals(new Sequence<>(splitWords("The dog jumped over the fence")), commonMiddle);
    }

    @Test
    public void testCommonMiddleFindsEnd1() {
        Sequence<String> s1 = new Sequence<>(splitWords("My dog jumped over the fence"));
        Sequence<String> s2 = new Sequence<>(splitWords("The dog looked over the fence"));
        Sequence<String> s3 = new Sequence<>(splitWords("The dog jumped over the fence"));
        Sequence<String> s4 = new Sequence<>(splitWords("The dog jumped over the fence"));

        Sequence<String> commonEnd = new Sequences<>(s1, s2, s3, s4).commonMiddle();
        assertEquals(new Sequence<>(splitWords("over the fence")), commonEnd);
    }

    @Test
    public void testCommonMiddleFindsEnd2() {
        Sequence<String> s1 = new Sequence<>(splitWords("The dog jumped over the fence"));
        Sequence<String> s2 = new Sequence<>(splitWords("The dog jumped over the fence"));
        Sequence<String> s3 = new Sequence<>(splitWords("The cat jumped over the fence"));
        Sequence<String> s4 = new Sequence<>(splitWords("The dog jumped over the fence"));

        Sequence<String> commonEnd = new Sequences<>(s1, s2, s3, s4).commonMiddle();
        assertEquals(new Sequence<>(splitWords("jumped over the fence")), commonEnd);
    }

    @Test
    public void testCommonMiddleFindsEnd3() {
        Sequence<String> s1 = new Sequence<>(splitWords("The dog jumped over the fence"));
        Sequence<String> s2 = new Sequence<>(splitWords("The dog jumped over the fence"));
        Sequence<String> s3 = new Sequence<>(splitWords("The dog jumped over the fence"));
        Sequence<String> s4 = new Sequence<>(splitWords("The dog jumped over the fence"));

        Sequence<String> commonEnd = new Sequences<>(s1, s2, s3, s4).commonMiddle();
        assertEquals(new Sequence<>(splitWords("The dog jumped over the fence")), commonEnd);
    }

    @Test
    public void testSliceStart() {
        List<Sequences<String>> slices = SequenceUtil.slice( //
                "The dog jumped over the fence, did he?", //
                "The dog jumped under the fence", //
                "The dog jumped over my sofa", //
                "The dog looked over the fence, did he?");
        assertFalse(slices.isEmpty());
    }

    @Test
    public void testSliceStartSameSize() {
        List<Sequences<String>> slices = SequenceUtil.slice( //
                "The dog jumped over the fence,", //
                "The dog jumped under the fence", //
                "The dog jumped over my sofa", //
                "The dog looked over the fence");
        assertFalse(slices.isEmpty());
    }

    @Test
    public void testSliceMiddle() {
        List<Sequences<String>> slices = SequenceUtil.slice( //
                "The dog jumped over the fence, did he?", //
                "The dog jumped over the fence", //
                "My cat jumped over my sofa", //
                "The dog looked over the fence, did he?");
        assertFalse(slices.isEmpty());
    }

    @Test
    public void testSliceMiddleSameSize() {
        List<Sequences<String>> slices = SequenceUtil.slice( //
                "The dog jumped over the fence", //
                "The dog jumped over the fence", //
                "My cat jumped over my sofa", //
                "The dog looked over the fence");
        assertFalse(slices.isEmpty());
    }

    @Test
    public void testSliceEnd() {
        List<Sequences<String>> slices = SequenceUtil.slice( //
                "The dog jumped over the fence, did he?", //
                "The dog jumped over the fence", //
                "Your cat crawled under my fence", //
                "The dog looked over the fence, did he?");
        assertFalse(slices.isEmpty());
    }

    @Test
    public void testSliceEndSameSize() {
        List<Sequences<String>> slices = SequenceUtil.slice( //
                "The dog jumped over the fence", //
                "The dog jumped over the fence", //
                "Your cat crawled under my fence", //
                "The dog looked over the fence");
        assertFalse(slices.isEmpty());
    }

    @Test
    public void testSliceAllDifferent() {
        List<Sequences<String>> slices = SequenceUtil.slice( //
                "No dog jumped over the fence, you know?", //
                "The dog jumped under the fence", //
                "The cat looked over my sofa", //
                "The dog jumped over the fence, sure?");
        assertFalse(slices.isEmpty());
    }

    @Test
    public void testSliceAllSame() {
        List<Sequences<String>> slices = SequenceUtil.slice( //
                "The dog jumped over the fence.", //
                "The dog jumped over the fence", //
                "The dog jumped over the fence.", //
                "The dog jumped over the fence.");
        assertFalse(slices.isEmpty());
    }

}
