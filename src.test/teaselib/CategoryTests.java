package teaselib;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import teaselib.util.Select;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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

    @Test
    public void testShoesCategories() {
        testCategories(Shoes.Categories, Shoes.values());
    }

    @Test
    public void testBondageCategories() {
        testCategories(Bondage.Categories, Bondage.values());
    }

    @Test
    public void testAccessoiresCategories() {
        testCategories(Accessoires.Categories, Accessoires.values());
    }

    @SuppressWarnings("unchecked")
    private static <T extends Enum<?>> void testCategories(List<Select.Statement> categories, T[] values) {
        Set<T> all = new HashSet<>();
        for (Select.Statement items : categories) {
            all.addAll(asSet((T[]) items.values));
        }

        for (T item : values) {
            Assertions.assertTrue(all.contains(item), "Item " + item + " not assigned to any catagory");
        }

        Assertions.assertEquals(values.length, all.size(), "There are items in more then one category");
    }

    @SafeVarargs
    private static <T> Set<T> asSet(T... items) {
        return new HashSet<>(Arrays.asList(items));
    }
}
