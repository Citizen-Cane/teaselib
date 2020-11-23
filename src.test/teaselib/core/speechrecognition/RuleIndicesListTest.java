package teaselib.core.speechrecognition;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

import org.junit.Test;

public class RuleIndicesListTest {

    @Test
    public void testIntersection() {
        RuleIndicesList empty = new RuleIndicesList(Collections.emptyList());
        assertEquals(Collections.emptySet(), empty.intersection());
    }

    @Test
    public void testDistinctValue() {
        Integer[][] values = { { 0, 1, 2, 3, 4 }, { 1, 2, 4 }, { 2, 4 }, { 2 }, { 0, 2, 3, 4, 5 }, { 2, 5 },
                { 2, 5 }, };
        RuleIndicesList indices = new RuleIndicesList(
                Arrays.stream(values).map(Arrays::asList).map(HashSet::new).collect(Collectors.toList()));
        assertEquals(2, indices.singleResult().orElseThrow().intValue());
    }

    @Test(expected = NoSuchElementException.class)
    public void testSingleResultNotPresent() {
        Integer[][] values = { { 0, 1, 2, 3, 4 }, { 1, 2, 4 }, { 2, 4 }, { 2 }, { 0, 2, 3, 4, 5 }, { 2, 5 }, { 2, 5 },
                { 0, 5 } };
        RuleIndicesList indices = new RuleIndicesList(
                Arrays.stream(values).map(Arrays::asList).map(HashSet::new).collect(Collectors.toList()));
        assertEquals(2, indices.singleResult().orElseThrow().intValue());
    }

}
