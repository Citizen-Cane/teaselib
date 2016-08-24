package teaselib;

import static org.junit.Assert.*;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.junit.Test;

public class ToyTests {

    @Test
    public void testToyCategoriesCompletness() {

        Set<Toys> all = new HashSet<Toys>();
        for (Toys[] toys : Arrays.asList(Toys.Categories)) {
            all.addAll(asSet(toys));
        }

        for (Toys toy : Toys.values()) {
            assertTrue("Toy " + toy + " not assigned to any catagory",
                    all.contains(toy));
        }

        assertEquals("There are items in more then one category",
                Toys.values().length, all.size());
    }

    private static <T> Set<T> asSet(T... items) {
        return new HashSet<T>(Arrays.asList(items));
    }
}
