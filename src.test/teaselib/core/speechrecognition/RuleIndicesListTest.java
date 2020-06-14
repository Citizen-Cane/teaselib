package teaselib.core.speechrecognition;

import static org.junit.Assert.assertEquals;

import java.util.Collections;

import org.junit.Test;

public class RuleIndicesListTest {

    @Test
    public void testIntersection() {
        RuleIndicesList empty = new RuleIndicesList(Collections.emptyList());
        assertEquals(Collections.emptySet(), empty.intersection());
    }

}
