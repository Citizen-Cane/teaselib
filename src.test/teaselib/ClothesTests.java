package teaselib;

import static org.junit.Assert.*;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.junit.Test;

public class ClothesTests {

    @Test
    public void testClothesCategoriesCompletness() {

        Clothes[][] categories = { Clothes.MaleClothes, Clothes.MaleAccesoires, Clothes.FemaleClothes,
                Clothes.FemaleAccesoires, Clothes.MaidAccesoires, Clothes.TrannyAccesoires, Clothes.FetishAccesoires };
        Set<Clothes> all = new HashSet<>();
        for (Clothes[] clothes : categories) {
            all.addAll(asSet(clothes));
        }

        for (Clothes clotthingItem : Clothes.values()) {
            assertTrue("Toy " + clotthingItem + " not assigned to any catagory", all.contains(clotthingItem));
        }

        // CLothing items can
    }

    @SafeVarargs
    private static <T> Set<T> asSet(T... items) {
        return new HashSet<>(Arrays.asList(items));
    }
}
