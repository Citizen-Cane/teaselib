package teaselib.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.concurrent.TimeUnit;

import org.junit.Test;

import teaselib.Body;
import teaselib.State;
import teaselib.Toys;
import teaselib.test.TestScript;
import teaselib.util.Item;

public class StateTests {

    @Test
    public void testLocalState() {
        TestScript script = TestScript.getOne();
        State somethingOnNipples = script.state(Body.SomethingOnNipples);
        assertFalse(somethingOnNipples.applied());

        somethingOnNipples.apply(Toys.Nipple_clamps);
        assertTrue(somethingOnNipples.applied());
        assertTrue(somethingOnNipples.expired());
        assertTrue(somethingOnNipples.peers().contains(Toys.Nipple_clamps));

        somethingOnNipples.remove();
        assertTrue(somethingOnNipples.expired());
        assertFalse(somethingOnNipples.applied());

        assertEquals(0, script.persistence.storage.size());
    }

    @Test
    public void testPersistentState() {
        TestScript script = TestScript.getOne();
        script.teaseLib.freezeTime();
        State somethingOnNipples = script.state(Body.SomethingOnNipples);
        assertTrue(somethingOnNipples.expired());
        assertFalse(somethingOnNipples.applied());
        assertEquals(0, script.persistence.storage.size());

        somethingOnNipples.apply(Toys.Nipple_clamps).over(30, TimeUnit.MINUTES);
        assertTrue(somethingOnNipples.applied());
        assertFalse(somethingOnNipples.expired());
        assertEquals(30,
                somethingOnNipples.duration().remaining(TimeUnit.MINUTES));

        assertEquals(0, script.persistence.storage.size());
        somethingOnNipples.apply(Toys.Nipple_clamps).over(30, TimeUnit.MINUTES)
                .remember();

        assertEquals(4, script.persistence.storage.size());
        somethingOnNipples.remove();
        assertEquals(0, script.persistence.storage.size());

        assertTrue(somethingOnNipples.expired());
        assertFalse(somethingOnNipples.applied());
    }

    @Test
    public void testPersistentStateAndValueCompatibility() {
        TestScript script = TestScript.getOne();
        script.teaseLib.freezeTime();

        Item<Toys> nippleClampsToy = script.toy(Toys.Nipple_clamps);
        nippleClampsToy.setAvailable(true);
        assertTrue(nippleClampsToy.isAvailable());

        Item<Toys> nippleClampsItem = script.item(Toys.Nipple_clamps);
        assertTrue(nippleClampsItem.isAvailable());
        nippleClampsItem.setAvailable(false);
        assertFalse(nippleClampsItem.isAvailable());

        State nippleClampsState = script.state(Toys.Nipple_clamps);
        assertFalse(nippleClampsState.applied());
        assertTrue(nippleClampsState.expired());
    }

    @Test
    public void testStatePeersAreImplemeted() {
        TestScript script = TestScript.getOne();
        script.teaseLib.freezeTime();

        State somethingOnNipples = script.state(Body.SomethingOnNipples);
        State nippleClampsState = script.state(Toys.Nipple_clamps);

        assertFalse(somethingOnNipples.applied());
        assertFalse(nippleClampsState.applied());

        somethingOnNipples.apply(Toys.Nipple_clamps).over(30, TimeUnit.MINUTES);

        assertTrue(somethingOnNipples.applied());
        assertTrue(nippleClampsState.applied());
    }
}
