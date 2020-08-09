package teaselib.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import org.junit.Test;

import teaselib.Toys;
import teaselib.test.TestScript;

public class ItemsQueryTest {
    TestScript test = TestScript.getOne();

    @Test
    public void testQuery() {
        ItemsQuery.Result items = test.queryItems(Toys.Humbler);

        assertEquals(1, items.get().size());
        assertEquals(0, items.get().getAvailable().size());
        assertFalse(items.get().anyAvailable());

        ItemsQuery.Result itemsQuery = ItemsQuery.select(items, Items::getAvailable);
        assertEquals(0, itemsQuery.get().size());
        assertFalse(itemsQuery.get().anyAvailable());
    }

}
