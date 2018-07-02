package teaselib.util;

import static org.junit.Assert.*;

import java.util.concurrent.TimeUnit;

import org.junit.Test;

import teaselib.Body;
import teaselib.State;
import teaselib.TeaseScript;
import teaselib.Toys;
import teaselib.core.util.QualifiedItem;
import teaselib.test.TestScript;

public class ItemsTests {

    @Test
    public void testGetAvailableItemsFirst() {
        TeaseScript script = TestScript.getOne();
        Items gags = script.items(Toys.Gag);

        assertFalse(gags.anyAvailable());
        assertFalse(gags.get(0).is(Toys.Gags.Ring_Gag));

        Item ringGag = gags.query(Toys.Gags.Ring_Gag).get();
        assertTrue(ringGag.is(Toys.Gags.Ring_Gag));

        ringGag.setAvailable(true);
        assertTrue(gags.anyAvailable());

        Items sameGags = script.items(Toys.Gag);
        assertTrue(sameGags.anyAvailable());
        assertFalse(gags.get(0).is(Toys.Gags.Ring_Gag));

        Item sameRingGag = script.item(Toys.Gag);
        assertTrue(sameRingGag.isAvailable());
        assertTrue(sameRingGag.is(Toys.Gags.Ring_Gag));

        Item againTheSameGag = script.items(Toys.Gag).getAvailable().get(0);
        assertTrue(againTheSameGag.isAvailable());
        assertTrue(againTheSameGag.is(Toys.Gags.Ring_Gag));
        assertFalse(gags.get(0).is(Toys.Gags.Ring_Gag));

        assertEquals(ringGag, sameRingGag);
        assertEquals(ringGag, againTheSameGag);
    }

    @Test
    public void testAvailable() {
        TeaseScript script = TestScript.getOne();
        Items gags = script.items(Toys.Gag);

        assertFalse(gags.anyAvailable());

        Item ringGag = gags.query(Toys.Gags.Ring_Gag).get();
        assertTrue(ringGag.is(Toys.Gags.Ring_Gag));

        assertEquals(0, gags.getAvailable().size());
        assertNotEquals(ringGag, script.item(Toys.Gag));
        assertTrue(script.item(Toys.Gag).is(Toys.Gags.Ball_Gag));

        ringGag.setAvailable(true);
        assertTrue(ringGag.isAvailable());

        assertEquals(ringGag, gags.getAvailable().get(0));

    }

    @Test
    public void testAll() {
        TestScript script = TestScript.getOne();
        script.addTestUserItems();

        Items gags = script.items(Toys.Gag);

        Items bitGags = gags.query(Toys.Gags.Bit_Gag, Body.Orifice.Oral);
        assertEquals(1, bitGags.size());

        Item bitGag = bitGags.get(0);
        Item sameRingGag = gags.query(Toys.Gags.Bit_Gag).get();

        assertEquals(sameRingGag, bitGag);
    }

    @Test
    public void testGet() {
        TeaseScript script = TestScript.getOne();
        Items gags = script.items(Toys.Gag);
        assertEquals(Toys.Gags.values().length, gags.size());

        assertNotEquals(Item.NotFound, gags.get());
        assertFalse(gags.get().isAvailable());

        Item bitGag = gags.query(Toys.Gags.Bit_Gag).get();
        assertTrue(bitGag.is(Toys.Gags.Bit_Gag));

        Item penisGag = gags.query(Toys.Gags.Penis_Gag).get();
        penisGag.setAvailable(true);

        assertEquals(penisGag, gags.get());
        assertEquals(bitGag, gags.query(Toys.Gags.Bit_Gag).get());

        assertFalse(gags.query(Toys.Gags.Bit_Gag).get().isAvailable());
        assertTrue(gags.query(Toys.Gags.Penis_Gag).get().isAvailable());

        assertEquals(penisGag, gags.get());
        assertTrue(gags.query(Toys.Gags.Penis_Gag).get().isAvailable());

        Item noRingGag = gags.prefer(Toys.Gags.Ring_Gag).get();
        assertEquals(penisGag, noRingGag);

        Item availablePenisGag = gags.prefer(Toys.Gags.Penis_Gag).get();
        assertEquals(penisGag, availablePenisGag);
    }

    // TODO Add more tests for prefer() in order to find out if the method makes sense
    // TODO Add tests for selectAppliableSet() in order to find out if the method makes sense

    @Test
    public void testContains() {
        TeaseScript script = TestScript.getOne();
        Items gags = script.items(Toys.Gag);
        assertEquals(Toys.Gags.values().length, gags.size());

        assertTrue(gags.contains(Toys.Gag));
        assertFalse(gags.contains(Toys.Buttplug));

        assertTrue(gags.contains("teaselib.Toys.Gag"));
        assertFalse(gags.contains("teaselib.Toys.Buttplug"));
    }

    @Test
    public void testAny() {
        TestScript script = TestScript.getOne();

        Items gags = script.items(Toys.Gag);
        assertEquals(Toys.Gags.values().length, gags.size());

        assertNotEquals(Item.NotFound, gags.get());
        assertFalse(gags.get().isAvailable());
        for (Item item : script.items(Toys.Gag)) {
            assertTrue(!item.isAvailable());
        }
        assertTrue(script.items(Toys.Collar).getAvailable().isEmpty());

        Item penisGag = gags.query(Toys.Gags.Penis_Gag).get();
        penisGag.setAvailable(true);

        assertEquals(1, script.items(Toys.Gag).getAvailable().size());
        assertEquals(penisGag, script.items(Toys.Gag).getAvailable().get(0));
    }

    @Test
    public void testRetainIsLogicalAnd() {
        TeaseScript script = TestScript.getOne();
        Items buttPlugs = script.items(Toys.Buttplug);
        assertTrue(buttPlugs.size() > 1);

        Item analBeads = buttPlugs.query(Toys.Anal.Beads).get();
        assertNotEquals(Item.NotFound, analBeads);

        Items allAnalbeads = script.items(Toys.Buttplug).query(Toys.Anal.Beads);
        assertTrue(allAnalbeads.size() == 1);
        assertEquals(analBeads, allAnalbeads.get(0));
    }

    @Test
    public void testGetDoesntSearchForPeersOrAttributes() {
        TeaseScript script = TestScript.getOne();
        Items chainedUp = script.items(Toys.Wrist_Restraints, Toys.Ankle_Restraints, Toys.Chains);

        Item chains = chainedUp.item(Toys.Chains);
        assertEquals(QualifiedItem.of(Toys.Chains), QualifiedItem.of(chains));
        chainedUp.item(Toys.Wrist_Restraints).applyTo(Toys.Chains);
        assertEquals(QualifiedItem.of(Toys.Chains), QualifiedItem.of(chains));
    }

    @Test
    public void testApplyToDefault() {
        TeaseScript script = TestScript.getOne();

        Item collar = script.item(Toys.Collar);
        assertFalse(collar.isAvailable());

        assertTrue(collar.is(Toys.Collar));
        assertFalse(collar.is(Body.AroundNeck));

        collar.apply();

        assertTrue(script.state(Body.AroundNeck).applied());
        assertTrue(collar.is(Body.AroundNeck));
    }

    @Test
    public void testRemainingDurationIsNegativeIfNotApplied() {
        TeaseScript script = TestScript.getOne();

        Item collar = script.item(Toys.Collar);
        assertFalse(collar.applied());

        assertTrue(collar.duration().remaining(TimeUnit.SECONDS) < State.REMOVED);
    }

    @Test
    public void showThatNamespaceAttributeIsSymetric() {
        TestScript script = TestScript.getOne();

        Item nippleClamps = script.item(Toys.Nipple_Clamps);
        nippleClamps.apply();
        State onNipples = script.state(Body.OnNipples);

        assertTrue(onNipples.is(script.namespace));
        assertTrue(onNipples.is(Toys.Nipple_Clamps));
        assertTrue(nippleClamps.is(Body.OnNipples));
        assertTrue(nippleClamps.is(script.namespace));

        nippleClamps.remove();

        assertFalse(onNipples.is(script.namespace));
        assertFalse(onNipples.is(Toys.Nipple_Clamps));
        assertFalse(script.state(Toys.Nipple_Clamps).is(Body.OnNipples));
        assertFalse(script.state(Toys.Nipple_Clamps).is(script.namespace));

        assertFalse(nippleClamps.applied());
        assertFalse(onNipples.applied());
    }
}
