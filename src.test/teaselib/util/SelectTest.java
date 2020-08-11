package teaselib.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import org.junit.Test;

import teaselib.Toys;
import teaselib.test.TestScript;

public class SelectTest {
    TestScript test = TestScript.getOne();

    @Test
    public void testItemQuery() {
        Items.Query items = test.query(Toys.Humbler);

        assertEquals(1, items.get().size());
        assertEquals(0, items.get().getAvailable().size());
        assertFalse(items.get().anyAvailable());

        Items.Query itemsQuery = Select.select(items, Items::getAvailable);
        assertEquals(0, itemsQuery.get().size());
        assertFalse(itemsQuery.get().anyAvailable());
    }

    @Test
    public void testStateQuery() {
        States.Query states = test.states(Toys.Humbler);
        assertEquals(1, states.get().size());

        States.Query applied = Select.select(states, States::applied);
        assertEquals(0, applied.get().size());

        test.state(Toys.Humbler).apply();
        assertEquals(1, applied.get().size());
    }

}
