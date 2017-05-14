package teaselib.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.concurrent.TimeUnit;

import org.junit.Test;

import teaselib.Body;
import teaselib.HouseHold;
import teaselib.Material;
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
        assertTrue(somethingOnNipples.is(Toys.Nipple_clamps));

        somethingOnNipples.remove();
        assertTrue(somethingOnNipples.expired());
        assertFalse(somethingOnNipples.applied());

        assertEquals(0, script.persistence.storage.size());
    }

    @Test
    public void testPersistentStateAndNamespace() {
        TestScript script = TestScript.getOne();
        script.teaseLib.freezeTime();
        State somethingOnNipples = script.state(Body.SomethingOnNipples);
        assertTrue(somethingOnNipples.expired());
        assertFalse(somethingOnNipples.applied());
        assertEquals(0, script.persistence.storage.size());

        somethingOnNipples.apply(Toys.Nipple_clamps).over(30, TimeUnit.MINUTES);
        assertTrue(somethingOnNipples.applied());
        assertFalse(somethingOnNipples.expired());
        assertEquals(30, somethingOnNipples.duration().remaining(TimeUnit.MINUTES));

        assertEquals(0, script.persistence.storage.size());
        somethingOnNipples.apply(Toys.Nipple_clamps).over(30, TimeUnit.MINUTES).remember();

        // Assert that when a state is applied then
        // the namespace of the script is applied to that state
        assertTrue(somethingOnNipples.is(script.namespace));
        assertTrue(somethingOnNipples.is(Toys.Nipple_clamps));
        assertTrue(script.state(Toys.Nipple_clamps).is(Body.SomethingOnNipples));
        assertFalse(script.state(Toys.Nipple_clamps).is(script.namespace));

        assertEquals(1, script.state(script.namespace).peers().size());
        assertEquals(Body.SomethingOnNipples, script.state(script.namespace).peers().iterator().next());
        assertTrue(script.state(script.namespace).is(Body.SomethingOnNipples));

        assertEquals(6, script.persistence.storage.size());
        somethingOnNipples.remove();
        assertEquals(0, script.persistence.storage.size());
        assertEquals(0, script.state(script.namespace).peers().size());

        assertTrue(somethingOnNipples.expired());
        assertFalse(somethingOnNipples.applied());
    }

    @Test
    public void testPersistentStateAndValueCompatibility() {
        TestScript script = TestScript.getOne();
        script.teaseLib.freezeTime();

        Item nippleClampsItem = script.item(Toys.Nipple_clamps);
        assertFalse(nippleClampsItem.isAvailable());
        nippleClampsItem.setAvailable(true);
        assertTrue(nippleClampsItem.isAvailable());

        State nippleClampsState = script.state(Toys.Nipple_clamps);
        assertFalse(nippleClampsState.applied());
        assertTrue(nippleClampsState.expired());

        nippleClampsState.apply(Body.SomethingOnNipples).over(1, TimeUnit.HOURS);
        assertTrue(nippleClampsState.applied());
        assertFalse(nippleClampsState.expired());

        assertTrue(nippleClampsItem.isAvailable());
        nippleClampsItem.setAvailable(false);
        assertFalse(nippleClampsItem.isAvailable());

        assertTrue(nippleClampsState.applied());
        assertFalse(nippleClampsState.expired());
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

    @Test
    public void testIsImplementsLogicalAnd() {
        TestScript script = TestScript.getOne();
        script.teaseLib.freezeTime();

        assertFalse(script.state(Body.SomethingOnNipples).applied());
        assertFalse(script.state(Toys.Nipple_clamps).applied());

        script.state(HouseHold.Weights).apply(Toys.Nipple_clamps);
        script.state(Toys.Nipple_clamps).apply(Body.SomethingOnNipples, Material.Metal).over(30, TimeUnit.MINUTES);

        assertTrue(script.state(Body.SomethingOnNipples).applied());
        assertTrue(script.state(Toys.Nipple_clamps).applied());

        assertTrue(script.state(Toys.Nipple_clamps).is(Material.Metal));
        assertTrue(script.state(Toys.Nipple_clamps).is(HouseHold.Weights));
        assertFalse(script.state(Toys.Nipple_clamps).is(Material.Rubber));

        assertTrue(script.state(Toys.Nipple_clamps).is(Material.Metal, HouseHold.Weights));
        assertFalse(script.state(Toys.Nipple_clamps).is(Material.Rubber, HouseHold.Weights));
    }

    @Test
    public void testIsImplementsLogicalAndPlusMixesWithStrings() {
        TestScript script = TestScript.getOne();
        script.teaseLib.freezeTime();

        assertFalse(script.state(Body.SomethingOnNipples).applied());
        assertFalse(script.state(Toys.Nipple_clamps).applied());
        assertFalse(script.state("teaselib.Body.SomethingOnNipples").applied());
        assertFalse(script.state("teaselib.Toys.Nipple_clamps").applied());

        script.state(HouseHold.Weights).apply(Toys.Nipple_clamps);
        script.state(Toys.Nipple_clamps).apply(Body.SomethingOnNipples, Material.Metal).over(30, TimeUnit.MINUTES);

        assertTrue(script.state(Body.SomethingOnNipples).applied());
        assertTrue(script.state(Toys.Nipple_clamps).applied());
        assertTrue(script.state("teaselib.Body.SomethingOnNipples").applied());
        assertTrue(script.state("teaselib.Toys.Nipple_clamps").applied());

        assertTrue(script.state(Toys.Nipple_clamps).is(Material.Metal));
        assertTrue(script.state(Toys.Nipple_clamps).is(HouseHold.Weights));
        assertFalse(script.state(Toys.Nipple_clamps).is(Material.Rubber));

        assertTrue(script.state(Toys.Nipple_clamps).is("teaselib.Material.Metal"));
        assertTrue(script.state(Toys.Nipple_clamps).is("teaselib.HouseHold.Weights"));
        assertFalse(script.state(Toys.Nipple_clamps).is("teaselib.Material.Rubber"));

        assertTrue(script.state("teaselib.Toys.Nipple_clamps").is(Material.Metal));
        assertTrue(script.state("teaselib.Toys.Nipple_clamps").is(HouseHold.Weights));
        assertFalse(script.state("teaselib.Toys.Nipple_clamps").is(Material.Rubber));

        assertTrue(script.state("teaselib.Toys.Nipple_clamps").is(Material.Metal));
        assertTrue(script.state("teaselib.Toys.Nipple_clamps").is(HouseHold.Weights));
        assertFalse(script.state("teaselib.Toys.Nipple_clamps").is(Material.Rubber));

        assertTrue(script.state("teaselib.Toys.Nipple_clamps").is("teaselib.Material.Metal"));
        assertTrue(script.state("teaselib.Toys.Nipple_clamps").is("teaselib.HouseHold.Weights"));
        assertFalse(script.state("teaselib.Toys.Nipple_clamps").is("teaselib.Material.Rubber"));

        assertTrue(script.state(Toys.Nipple_clamps).is(Material.Metal, HouseHold.Weights));
        assertFalse(script.state(Toys.Nipple_clamps).is(Material.Rubber, HouseHold.Weights));
        assertTrue(script.state(Toys.Nipple_clamps).is("teaselib.Material.Metal", HouseHold.Weights));
        assertFalse(script.state(Toys.Nipple_clamps).is("teaselib.Material.Rubber", HouseHold.Weights));
        assertTrue(script.state(Toys.Nipple_clamps).is("teaselib.Material.Metal", "teaselib.HouseHold.Weights"));
        assertFalse(script.state(Toys.Nipple_clamps).is("teaselib.Material.Rubber", "teaselib.HouseHold.Weights"));

        assertTrue(script.state("teaselib.Toys.Nipple_clamps").is(Material.Metal, HouseHold.Weights));
        assertFalse(script.state("teaselib.Toys.Nipple_clamps").is(Material.Rubber, HouseHold.Weights));
        assertTrue(script.state("teaselib.Toys.Nipple_clamps").is("teaselib.Material.Metal", HouseHold.Weights));
        assertFalse(script.state("teaselib.Toys.Nipple_clamps").is("teaselib.Material.Rubber", HouseHold.Weights));
        assertTrue(script.state("teaselib.Toys.Nipple_clamps").is("teaselib.Material.Metal",
                "teaselib.HouseHold.Weights"));
        assertFalse(script.state("teaselib.Toys.Nipple_clamps").is("teaselib.Material.Rubber",
                "teaselib.HouseHold.Weights"));
    }

}
