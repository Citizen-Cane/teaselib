package teaselib;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.concurrent.TimeUnit;

import org.junit.Test;

import teaselib.test.TestScript;
import teaselib.util.Item;

public class StateTests {

    @Test
    public void testLocalState() {
        TestScript script = TestScript.getOne();
        State somethingOnNipples = script.state(Body.SomethingOnNipples);
        assertFalse(somethingOnNipples.applied());
        somethingOnNipples.apply();
        assertTrue(somethingOnNipples.applied());
        assertFalse(somethingOnNipples.expired());
        somethingOnNipples.remove();
        assertTrue(somethingOnNipples.expired());
        assertFalse(somethingOnNipples.applied());
        assertEquals(0, script.persistence.storage.size());
    }

    @Test
    public void testPersistentState() {
        TestScript script = TestScript.getOne();
        State somethingOnNipples = script.state(Body.SomethingOnNipples);
        assertFalse(somethingOnNipples.applied());
        somethingOnNipples.apply(30, TimeUnit.MINUTES);
        assertTrue(somethingOnNipples.applied());
        assertFalse(somethingOnNipples.expired());
        assertEquals(30, somethingOnNipples.remaining(TimeUnit.MINUTES));
        assertEquals(1, script.persistence.storage.size());

        somethingOnNipples.remove();
        assertTrue(somethingOnNipples.expired());
        assertFalse(somethingOnNipples.applied());
        assertEquals(0, script.persistence.storage.size());
    }

    @Test
    public void testPersistentStateAndValueCompatibility() {
        TestScript script = TestScript.getOne();

        Item<Toys> nippleClampsToy = script.toy(Toys.Nipple_clamps);
        nippleClampsToy.setAvailable(true);
        assertTrue(nippleClampsToy.isAvailable());

        Item<Toys> nippleClampsItem = script.item(Toys.Nipple_clamps);
        assertTrue(nippleClampsItem.isAvailable());
        nippleClampsItem.setAvailable(false);
        assertFalse(nippleClampsItem.isAvailable());

        State nippleClampsState = script.state(Toys.Nipple_clamps);
        assertFalse(nippleClampsState.applied());
        nippleClampsState.apply(30, TimeUnit.MINUTES);

        assertEquals(2, script.persistence.storage.size());
    }

}
