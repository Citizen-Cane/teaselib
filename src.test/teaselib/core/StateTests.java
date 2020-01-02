package teaselib.core;

import static org.junit.Assert.*;

import java.util.concurrent.TimeUnit;

import org.junit.Test;

import teaselib.Body;
import teaselib.Household;
import teaselib.Material;
import teaselib.State;
import teaselib.Toys;
import teaselib.core.state.AbstractProxy;
import teaselib.core.state.StateProxy;
import teaselib.core.util.QualifiedName;
import teaselib.test.TestScript;
import teaselib.util.Item;

public class StateTests {

    enum PeerlessState {
        Test1
    }

    @Test
    public void testPeerlessApply() {
        TestScript script = TestScript.getOne();
        assertFalse(script.state(PeerlessState.Test1).applied());

        script.state(PeerlessState.Test1).apply();
        assertTrue(script.state(PeerlessState.Test1).applied());

        script.state(PeerlessState.Test1).remove();
        assertFalse(script.state(PeerlessState.Test1).applied());
    }

    @Test
    public void testLocalState() {
        TestScript script = TestScript.getOne();
        State somethingOnNipples = script.state(Body.OnNipples);
        assertFalse(somethingOnNipples.applied());

        somethingOnNipples.applyTo(Toys.Nipple_Clamps);
        assertTrue(somethingOnNipples.applied());
        assertTrue(somethingOnNipples.expired());
        assertTrue(somethingOnNipples.is(Toys.Nipple_Clamps));

        somethingOnNipples.remove();
        assertTrue(somethingOnNipples.expired());
        assertFalse(somethingOnNipples.applied());

        assertEquals(0, script.storage.size());
    }

    @Test
    public void testPersistentStateAndNamespaceAttribute() {
        TestScript script = TestScript.getOne();
        script.teaseLib.freezeTime();
        State somethingOnNipples = script.state(Body.OnNipples);
        assertTrue(somethingOnNipples.expired());
        assertFalse(somethingOnNipples.applied());
        assertEquals(0, script.storage.size());

        somethingOnNipples.applyTo(Toys.Nipple_Clamps).over(30, TimeUnit.MINUTES);
        assertTrue(somethingOnNipples.applied());
        assertFalse(somethingOnNipples.expired());
        assertEquals(30, somethingOnNipples.duration().remaining(TimeUnit.MINUTES));

        assertEquals(0, script.storage.size());
        somethingOnNipples.applyTo(Toys.Nipple_Clamps).over(30, TimeUnit.MINUTES).remember();

        // Assert that when a state is applied then
        // the namespace of the script is applied to that state
        assertTrue(somethingOnNipples.is(script.namespace));
        assertTrue(somethingOnNipples.is(Toys.Nipple_Clamps));
        assertTrue(script.state(Toys.Nipple_Clamps).is(Body.OnNipples));
        assertTrue(script.state(Toys.Nipple_Clamps).is(script.namespace));

        // There's no attribute for nipple clamps, because we didn't set any,
        // the namespace is only added automatically to Body.SomethingOnNipples
        // because thats the state our test script applied,
        // and empty attribute lists aren't persisted

        assertTrue(script.storage
                .containsKey(QualifiedName.of(TeaseLib.DefaultDomain, "Toys", "Nipple_Clamps.state.duration")));
        assertTrue(script.storage
                .containsKey(QualifiedName.of(TeaseLib.DefaultDomain, "Toys", "Nipple_Clamps.state.peers")));
        assertFalse(script.storage
                .containsKey(QualifiedName.of(TeaseLib.DefaultDomain, "Toys", "Nipple_Clamps.state.attributes")));

        assertTrue(
                script.storage.containsKey(QualifiedName.of(TeaseLib.DefaultDomain, "Body", "OnNipples.state.peers")));
        assertTrue(script.storage
                .containsKey(QualifiedName.of(TeaseLib.DefaultDomain, "Body", "OnNipples.state.duration")));
        assertTrue(script.storage
                .containsKey(QualifiedName.of(TeaseLib.DefaultDomain, "Body", "OnNipples.state.attributes")));

        assertEquals(0, ((StateProxy) script.state(script.namespace)).peers().size());

        somethingOnNipples.remove();
        assertEquals(0, script.storage.size());

        assertFalse(somethingOnNipples.is(script.namespace));
        assertFalse(somethingOnNipples.is(Toys.Nipple_Clamps));
        assertFalse(script.state(Toys.Nipple_Clamps).is(Body.OnNipples));
        assertFalse(script.state(Toys.Nipple_Clamps).is(script.namespace));

        assertFalse(somethingOnNipples.expired());
        script.debugger.advanceTime(30, TimeUnit.MINUTES);
        assertTrue(somethingOnNipples.expired());
        assertFalse(somethingOnNipples.applied());
    }

    @Test
    public void testThatNamespaceAttributeIsApppliedToPeers() {
        TestScript script = TestScript.getOne();
        script.teaseLib.freezeTime();

        script.state(Household.Condoms).applyTo(Body.OnPenis);
        assertTrue(script.state(Household.Condoms).is(script.namespace));
        assertTrue(script.state(Body.OnPenis).applied());

        assertTrue(script.state(Body.OnPenis).is(script.namespace));

        script.state(Toys.Chastity_Device).applyTo(Body.OnPenis);
        assertTrue(script.state(Body.OnPenis).applied());
        assertTrue(script.state(Toys.Chastity_Device).is(script.namespace));

        assertTrue(script.state(Body.OnPenis).is(script.namespace));

        script.state(Toys.Chastity_Device).remove();
        assertTrue(script.state(Body.OnPenis).applied());
        assertFalse(script.state(Toys.Chastity_Device).is(script.namespace));

        assertTrue(script.state(Body.OnPenis).is(script.namespace));

        script.state(Household.Condoms).remove();
        assertFalse(script.state(Body.OnPenis).applied());
        assertFalse(script.state(Household.Condoms).is(script.namespace));

        assertFalse(script.state(Body.OnPenis).is(script.namespace));
    }

    @Test
    public void testThatNamespaceAttributeIsRemovedWhenRemovingTheLastItem() {
        TestScript script = TestScript.getOne();
        script.teaseLib.freezeTime();

        script.state(Household.Clothes_Pegs).applyTo(Body.OnBalls);
        assertTrue(script.state(Household.Clothes_Pegs).is(script.namespace));

        script.state(Household.Clothes_Pegs).applyTo(Body.OnNipples);
        assertTrue(script.state(Household.Clothes_Pegs).is(script.namespace));

        script.state(Body.OnBalls).remove();
        assertTrue(script.state(Household.Clothes_Pegs).is(script.namespace));

        script.state(Body.OnNipples).remove();
        assertFalse(script.state(Household.Clothes_Pegs).is(script.namespace));
    }

    @Test
    public void testPersistentStateAndValueCompatibility() {
        TestScript script = TestScript.getOne();
        script.teaseLib.freezeTime();

        Item nippleClampsItem = script.item(Toys.Nipple_Clamps);
        assertFalse(nippleClampsItem.isAvailable());
        nippleClampsItem.setAvailable(true);
        assertTrue(nippleClampsItem.isAvailable());

        State nippleClampsState = script.state(Toys.Nipple_Clamps);
        assertFalse(nippleClampsState.applied());
        assertTrue(nippleClampsState.expired());

        nippleClampsState.applyTo(Body.OnNipples).over(1, TimeUnit.HOURS);
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

        State somethingOnNipples = script.state(Body.OnNipples);
        State nippleClampsState = script.state(Toys.Nipple_Clamps);

        assertFalse(somethingOnNipples.applied());
        assertFalse(nippleClampsState.applied());

        somethingOnNipples.applyTo(Toys.Nipple_Clamps).over(30, TimeUnit.MINUTES);

        assertTrue(somethingOnNipples.applied());
        assertTrue(nippleClampsState.applied());
    }

    @Test
    public void testIsImplementsLogicalAnd() {
        TestScript script = TestScript.getOne();
        script.teaseLib.freezeTime();

        assertFalse(script.state(Body.OnNipples).applied());
        assertFalse(script.state(Toys.Nipple_Clamps).applied());

        script.state(Household.Weight).applyTo(Toys.Nipple_Clamps);
        script.state(Toys.Nipple_Clamps).applyTo(Body.OnNipples, Material.Metal).over(30, TimeUnit.MINUTES);

        assertTrue(script.state(Body.OnNipples).applied());
        assertTrue(script.state(Toys.Nipple_Clamps).applied());

        assertTrue(script.state(Toys.Nipple_Clamps).is(Material.Metal));
        assertTrue(script.state(Toys.Nipple_Clamps).is(Household.Weight));
        assertFalse(script.state(Toys.Nipple_Clamps).is(Material.Rubber));

        assertTrue(script.state(Toys.Nipple_Clamps).is(Material.Metal, Household.Weight));
        assertFalse(script.state(Toys.Nipple_Clamps).is(Material.Rubber, Household.Weight));
    }

    @Test
    public void testIsImplementsLogicalAndPlusMixesWithStrings() {
        TestScript script = TestScript.getOne();
        script.teaseLib.freezeTime();

        assertFalse(script.state(Body.OnNipples).applied());
        assertFalse(script.state(Toys.Nipple_Clamps).applied());
        assertFalse(script.state("teaselib.Body.OnNipples").applied());
        assertFalse(script.state("teaselib.Toys.Nipple_Clamps").applied());

        script.state(Household.Weight).applyTo(Toys.Nipple_Clamps);
        script.state(Toys.Nipple_Clamps).applyTo(Body.OnNipples, Material.Metal).over(30, TimeUnit.MINUTES);

        assertTrue(script.state(Body.OnNipples).applied());
        assertTrue(script.state(Toys.Nipple_Clamps).applied());
        assertTrue(script.state("teaselib.Body.OnNipples").applied());
        assertTrue(script.state("teaselib.Toys.Nipple_Clamps").applied());

        assertTrue(script.state(Toys.Nipple_Clamps).is(Material.Metal));
        assertTrue(script.state(Toys.Nipple_Clamps).is(Household.Weight));
        assertFalse(script.state(Toys.Nipple_Clamps).is(Material.Rubber));

        assertTrue(script.state(Toys.Nipple_Clamps).is("teaselib.Material.Metal"));
        assertTrue(script.state(Toys.Nipple_Clamps).is("teaselib.HouseHold.Weight"));
        assertFalse(script.state(Toys.Nipple_Clamps).is("teaselib.Material.Rubber"));

        assertTrue(script.state("teaselib.Toys.Nipple_Clamps").is(Material.Metal));
        assertTrue(script.state("teaselib.Toys.Nipple_Clamps").is(Household.Weight));
        assertFalse(script.state("teaselib.Toys.Nipple_Clamps").is(Material.Rubber));

        assertTrue(script.state("teaselib.Toys.Nipple_Clamps").is(Material.Metal));
        assertTrue(script.state("teaselib.Toys.Nipple_Clamps").is(Household.Weight));
        assertFalse(script.state("teaselib.Toys.Nipple_Clamps").is(Material.Rubber));

        assertTrue(script.state("teaselib.Toys.Nipple_Clamps").is("teaselib.Material.Metal"));
        assertTrue(script.state("teaselib.Toys.Nipple_Clamps").is("teaselib.HouseHold.Weight"));
        assertFalse(script.state("teaselib.Toys.Nipple_Clamps").is("teaselib.Material.Rubber"));

        assertTrue(script.state(Toys.Nipple_Clamps).is(Material.Metal, Household.Weight));
        assertFalse(script.state(Toys.Nipple_Clamps).is(Material.Rubber, Household.Weight));
        assertTrue(script.state(Toys.Nipple_Clamps).is("teaselib.Material.Metal", Household.Weight));
        assertFalse(script.state(Toys.Nipple_Clamps).is("teaselib.Material.Rubber", Household.Weight));
        assertTrue(script.state(Toys.Nipple_Clamps).is("teaselib.Material.Metal", "teaselib.HouseHold.Weight"));
        assertFalse(script.state(Toys.Nipple_Clamps).is("teaselib.Material.Rubber", "teaselib.HouseHold.Weight"));

        assertTrue(script.state("teaselib.Toys.Nipple_Clamps").is(Material.Metal, Household.Weight));
        assertFalse(script.state("teaselib.Toys.Nipple_Clamps").is(Material.Rubber, Household.Weight));
        assertTrue(script.state("teaselib.Toys.Nipple_Clamps").is("teaselib.Material.Metal", Household.Weight));
        assertFalse(script.state("teaselib.Toys.Nipple_Clamps").is("teaselib.Material.Rubber", Household.Weight));
        assertTrue(
                script.state("teaselib.Toys.Nipple_Clamps").is("teaselib.Material.Metal", "teaselib.HouseHold.Weight"));
        assertFalse(script.state("teaselib.Toys.Nipple_Clamps").is("teaselib.Material.Rubber",
                "teaselib.HouseHold.Weight"));
    }

    @Test
    public void testStateIdentity() {
        TestScript script = TestScript.getOne();
        script.teaseLib.freezeTime();

        State clothesPegsByEnum = script.state(Household.Clothes_Pegs);
        State clothesPegsByString = script.state("teaselib.household.clothes_pegs");

        assertEquals(clothesPegsByEnum, clothesPegsByString);
        assertTrue(((StateProxy) clothesPegsByEnum).state == ((StateProxy) clothesPegsByString).state);
    }

    @Test
    public void testThatInjectedStateInstancesArentOverwrittenByEquals() {
        TestScript script = TestScript.getOne();
        State test1 = script.teaseLib.state(TeaseLib.DefaultDomain,
                new StateImpl(script.teaseLib, TeaseLib.DefaultDomain, "test"));
        State test2 = script.teaseLib.state(TeaseLib.DefaultDomain,
                new StateImpl(script.teaseLib, TeaseLib.DefaultDomain, "test"));
        assertSame(test1, test2);
    }

    @Test
    public void testApplyResetsDuration() {
        TestScript script = TestScript.getOne();
        script.teaseLib.freezeTime();

        script.state(Toys.Nipple_Clamps).applyTo(Body.OnNipples).over(30, TimeUnit.MINUTES);
        assertEquals(30, script.state(Toys.Nipple_Clamps).duration().remaining(TimeUnit.MINUTES));

        script.state(Toys.Nipple_Clamps).apply();
        // allows setting a duration for items that may be attached to multiple peers
        // - otherwise the duration would be forgotten
        assertEquals("Apply without setting a duration must retain the previous duration", 30,
                script.state(Toys.Nipple_Clamps).duration().remaining(TimeUnit.MINUTES));
        assertFalse(script.state(Toys.Nipple_Clamps).expired());
    }

    @Test
    public void testPersistentStateRememberWithoutDuration() {
        TestScript script = TestScript.getOne();
        script.teaseLib.freezeTime();

        script.state(Body.OnNipples).applyTo(Toys.Nipple_Clamps).over(30, TimeUnit.MINUTES);
        assertEquals(30, script.state(Body.OnNipples).duration().remaining(TimeUnit.MINUTES));

        script.debugger.advanceTime(10, TimeUnit.MINUTES);
        assertEquals(20, script.state(Body.OnNipples).duration().remaining(TimeUnit.MINUTES));

        // TODO Public interface or utility method
        AbstractProxy.stateImpl(script.state(Body.OnNipples)).remember();
        assertEquals(20, script.state(Body.OnNipples).duration().remaining(TimeUnit.MINUTES));

        script.debugger.clearStateMaps();
        assertEquals(20, script.state(Body.OnNipples).duration().remaining(TimeUnit.MINUTES));
    }

}
