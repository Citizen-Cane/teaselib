package teaselib.core.speechrecognition.srgs;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Collections;

import org.junit.Test;

public class PhrasesSliceTest {

    @Test
    public void testCommonEnd() {
        ChoiceStringSequences choices = new ChoiceStringSequences(
                new ChoiceString("Yes, Miss", Collections.singleton(0)),
                new ChoiceString("No, Miss", Collections.singleton(1)));

        Sequences<ChoiceString> disjunct = choices.slice();
        Sequences<ChoiceString> common = choices.slice();

        assertFalse(disjunct.isEmpty());
        assertFalse(common.isEmpty());

        assertTrue(true);
    }

}
