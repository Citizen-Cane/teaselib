package teaselib.core.speechrecognition.srgs;

import static org.junit.Assert.assertEquals;

import java.util.List;

import org.junit.Test;

public class PhrasesTest {

    @Test
    public void testSliceMultiplePhrases() {
        List<Sequences<String>> slices = Phrases.of( //
                "Yes Miss, of course", //
                "Yes, of course, Miss", //
                "Yes, of course", //
                "of course");

        assertEquals(3, slices.size());

        assertEquals(new Sequences<>(new Sequence<>("Yes", "Miss"), new Sequence<>("Yes"), new Sequence<>("Yes"),
                new Sequence<>()), slices.get(0));
        assertEquals(new Sequences<>(new Sequence<>("of", "course")), slices.get(1));
        assertEquals(new Sequences<>(new Sequence<>(), new Sequence<>("Miss"), new Sequence<>(), new Sequence<>()),
                slices.get(2));

        System.out.println(slices);
    }

}
