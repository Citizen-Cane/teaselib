package teaselib.util.math;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;

public class CombinationsTest {

    @Test
    public void testCombinations1toN_1() throws Exception {
        List<Integer[]> combinations1toN = Combinations.combinations1toN(Integer.valueOf(1));
        Assertions.assertEquals(1, combinations1toN.size());

        Integer[] result = new Integer[]{Integer.valueOf(1)};

        Assertions.assertArrayEquals(result, combinations1toN.get(0));
    }

    @Test
    public void testCombinations1toN_2() throws Exception {
        List<Integer[]> combinations1toN = Combinations.combinations1toN(Integer.valueOf(1), Integer.valueOf(2));
        Assertions.assertEquals(3, combinations1toN.size());

        Integer[] result0 = new Integer[]{Integer.valueOf(1)};
        Integer[] result1 = new Integer[]{Integer.valueOf(2)};
        Integer[] result2 = new Integer[]{Integer.valueOf(1), Integer.valueOf(2)};

        Assertions.assertArrayEquals(result0, combinations1toN.get(0));
        Assertions.assertArrayEquals(result1, combinations1toN.get(1));
        Assertions.assertArrayEquals(result2, combinations1toN.get(2));
    }

    @Test
    public void testCombinations1toN_3() throws Exception {
        List<Integer[]> combinations1toN = Combinations.combinations1toN(Integer.valueOf(1), Integer.valueOf(2),
                Integer.valueOf(3));
        Assertions.assertEquals(7, combinations1toN.size());

        Integer[] result0 = new Integer[]{Integer.valueOf(1)};
        Integer[] result1 = new Integer[]{Integer.valueOf(2)};
        Integer[] result2 = new Integer[]{Integer.valueOf(3)};
        Integer[] result3 = new Integer[]{Integer.valueOf(1), Integer.valueOf(2)};
        Integer[] result4 = new Integer[]{Integer.valueOf(1), Integer.valueOf(3)};
        Integer[] result5 = new Integer[]{Integer.valueOf(2), Integer.valueOf(3)};
        Integer[] result6 = new Integer[]{Integer.valueOf(1), Integer.valueOf(2), Integer.valueOf(3)};

        Assertions.assertArrayEquals(result0, combinations1toN.get(0));
        Assertions.assertArrayEquals(result1, combinations1toN.get(1));
        Assertions.assertArrayEquals(result2, combinations1toN.get(2));
        Assertions.assertArrayEquals(result3, combinations1toN.get(3));
        Assertions.assertArrayEquals(result4, combinations1toN.get(4));
        Assertions.assertArrayEquals(result5, combinations1toN.get(5));
        Assertions.assertArrayEquals(result6, combinations1toN.get(6));
    }

}
