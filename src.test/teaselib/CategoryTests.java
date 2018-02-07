package teaselib;

import static org.junit.Assert.*;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.junit.Test;

public class CategoryTests {

    @Test
    public void testToyCategories() {
        testCategories(Toys.Categories, Toys.values());
    }

    @Test
    public void testHouseholdCategories() {
        testCategories(Household.Categories, Household.values());
    }

    @Test
    public void testClothesCategories() {
        testCategories(Clothes.Categories, Clothes.values());
    }

    private static <T extends Enum<?>> void testCategories(T[][] categories, T[] values) {
        Set<T> all = new HashSet<>();
        for (T[] items : Arrays.asList(categories)) {
            all.addAll(asSet(items));
        }

        for (T item : values) {
            assertTrue("Item " + item + " not assigned to any catagory", all.contains(item));
        }

        assertEquals("There are items in more then one category", values.length, all.size());
    }

    @SafeVarargs
    private static <T> Set<T> asSet(T... items) {
        return new HashSet<>(Arrays.asList(items));
    }
}
