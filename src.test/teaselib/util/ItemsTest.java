package teaselib.util;

import static org.junit.Assert.*;

import java.util.concurrent.TimeUnit;

import org.junit.Test;

import teaselib.Body;
import teaselib.Clothes;
import teaselib.Features;
import teaselib.Household;
import teaselib.Material;
import teaselib.State;
import teaselib.TeaseScript;
import teaselib.Toys;
import teaselib.core.state.ItemProxy;
import teaselib.core.util.QualifiedItem;
import teaselib.test.TestScript;
import teaselib.util.math.Varieties;

public class ItemsTest {
    @Test
    public void testSimpleApplyWithStates() {
        TeaseScript script = TestScript.getOne();

        script.state(Clothes.Stockings).apply();
        assertTrue(script.state(Clothes.Stockings).applied());
        assertTrue(script.item(Clothes.Stockings).applied());

        Items items = script.items(Clothes.Garter_Belt);
        items.apply();
        assertTrue(script.state(Clothes.Garter_Belt).applied());
        assertTrue(script.item(Clothes.Garter_Belt).applied());
    }

    @Test
    public void testSimpleApplyWithoutDefaultPeers() {
        TeaseScript script = TestScript.getOne();

        script.item(Clothes.Stockings).apply();
        assertTrue(script.state(Clothes.Stockings).applied());
        assertTrue(script.item(Clothes.Stockings).applied());

        Items items = script.items(Clothes.Garter_Belt);
        items.apply();
        assertTrue(script.item(Clothes.Garter_Belt).applied());
    }

    @Test
    public void testSimpleApplyWithDefaultPeers() {
        TeaseScript script = TestScript.getOne();

        Item gag = script.item(Toys.Gag);
        gag.apply();
        assertTrue(script.item(Toys.Gag).applied());

        Items buttPlugs = script.items(Toys.Buttplug);
        buttPlugs.apply();
        assertTrue(script.item(Toys.Buttplug).applied());
    }

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

        Item chains = chainedUp.get(Toys.Chains);
        assertEquals(QualifiedItem.of(Toys.Chains), QualifiedItem.of(chains));
        chainedUp.get(Toys.Wrist_Restraints).applyTo(Toys.Chains);
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

        assertTrue(collar.duration().remaining(TimeUnit.SECONDS) < State.TEMPORARY);
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

    @Test
    public void testVarieties() {
        TestScript script = TestScript.getOne();

        Items inventory = script.items(Toys.Collar, Toys.Ankle_Restraints, Toys.Wrist_Restraints, Toys.Chains);
        Varieties<Items> varieties = inventory.prefer(Features.Lockable, Material.Leather).varieties();
        Items restraints = varieties.reduce(Items::best);
        assertEquals(4, restraints.size());

        Item collar = restraints.get(Toys.Collar);
        Item anklecuffs = restraints.get(Toys.Ankle_Restraints);
        Item wristCuffs = restraints.get(Toys.Wrist_Restraints);
        Item chains = restraints.get(Toys.Chains);

        assertNotEquals(Item.NotFound, collar);
        assertNotEquals(Item.NotFound, anklecuffs);
        assertNotEquals(Item.NotFound, wristCuffs);
        assertNotEquals(Item.NotFound, chains);
    }

    @Test
    public void testItemsSubList() {
        TestScript script = TestScript.getOne();

        Item wristRestraints = script.item(Toys.Wrist_Restraints);
        assertTrue(wristRestraints.is(Toys.Wrist_Restraints));

        Item wristRestraints2 = script.items(Toys.Wrist_Restraints).items(Toys.Wrist_Restraints).get();
        assertNotEquals(Item.NotFound, wristRestraints2);
        assertTrue(wristRestraints2.is(Toys.Wrist_Restraints));
    }

    @Test
    public void testItemValues() {
        TestScript script = TestScript.getOne();

        Items restraints = script.items(Toys.Wrist_Restraints, Toys.Ankle_Restraints);

        Object[] values = restraints.values();

        assertEquals(Toys.Wrist_Restraints, values[0]);
        assertEquals(Toys.Ankle_Restraints, values[1]);
    }

    @Test
    public void testAnyApplied() {
        TestScript script = TestScript.getOne();

        Items restraints = script.items(Toys.Wrist_Restraints, Toys.Ankle_Restraints);

        assertFalse(restraints.anyApplied());

        script.item(Toys.Wrist_Restraints).apply();
        assertTrue(restraints.anyApplied());
        assertFalse(restraints.allApplied());

        script.item(Toys.Ankle_Restraints).apply();
        assertTrue(restraints.anyApplied());
        assertTrue(restraints.allApplied());

        restraints.remove();
        assertFalse(restraints.anyApplied());
    }

    @Test
    public void testWhatItemHasBeenApplied() {
        TestScript script = TestScript.getOne();
        script.addTestUserItems();

        assertFalse(script.state(Toys.Wrist_Restraints).applied());

        Item leatherCuffs = script.items(Toys.Wrist_Restraints).query(Material.Leather).get();
        Item handCuffs = script.items(Toys.Wrist_Restraints).query(Material.Metal).get();

        assertNotEquals(leatherCuffs, handCuffs);

        leatherCuffs.apply();

        assertTrue(script.state(Body.WristsTied).is(leatherCuffs));
        assertFalse(script.state(Body.WristsTied).is(handCuffs));

        assertTrue(leatherCuffs.applied());
        assertFalse(handCuffs.applied());
    }

    @Test
    public void testAppliedItems() {
        TestScript script = TestScript.getOne();
        script.addTestUserItems();

        Items restraints = script.items(Toys.Wrist_Restraints);
        assertEquals(2, restraints.size());

        script.item(Toys.Wrist_Restraints).apply();

        Items applied = script.items(Toys.Wrist_Restraints).getApplied();
        assertEquals(1, applied.size());
        assertTrue(applied.get().is(restraints.get()));
        assertEquals(restraints.get(), applied.get());
    }

    @Test
    public void testExplicitelyAppliedItems() {
        TestScript script = TestScript.getOne();
        script.addTestUserItems();

        Items restraints = script.items(Toys.Wrist_Restraints);
        assertEquals(2, restraints.size());

        restraints.get().apply();

        Items applied = script.items(Toys.Wrist_Restraints).getApplied();
        assertEquals(1, applied.size());
    }

    // TODO test that query doesn't work with namespace and show how to do it right

    @Test
    public void testQueryItemInstanceAttributesDontInterferWithApplied() {
        TestScript script = TestScript.getOne();
        script.debugger.freezeTime();

        Items gags1 = script.items(Toys.Gag);
        Item ringGag = gags1.query(Toys.Gags.Ring_Gag).get();
        assertTrue(ringGag.is(Toys.Gags.Ring_Gag));
        ringGag.apply();
        assertTrue(ringGag.applied());
        assertTrue(script.state(Body.InMouth).is(ringGag));
        // assertTrue(script.state(Toys.Gag).is(ringGag)); // ???

        Items gags2 = script.items(Toys.Gag);
        Item ringGag2 = gags2.query(Toys.Gags.Ring_Gag).get();
        assertEquals(ringGag, ringGag2);
        assertNotSame(ringGag, ringGag2);
        assertEquals(((ItemProxy) ringGag).item, ((ItemProxy) ringGag2).item);

        assertTrue(ringGag.is(Toys.Gags.Ring_Gag));

        // "Is this gag applied"
        assertTrue(ringGag2.applied());
        // "Is the object in your mouth this ring gag?"
        assertTrue(script.state(Body.InMouth).is(ringGag2));
        // TODO Answer the question "Is the applied gag this ring gag?"
        // assertTrue(script.state(Toys.Gag).is(ringGag2)); // ???

        Items gags3 = script.items(Toys.Gag);
        Item muzzleGag = gags3.query(Toys.Gags.Muzzle_Gag).get();

        assertNotSame(ringGag2, muzzleGag);
        assertTrue(muzzleGag.is(Toys.Gag));
        assertTrue(muzzleGag.is(Toys.Gags.Muzzle_Gag));
        assertFalse(script.state(Toys.Gag).is(muzzleGag));

        assertFalse(muzzleGag.applied());
        assertFalse(script.state(Body.InMouth).is(muzzleGag));

        assertTrue(script.state(Toys.Gag).is(Body.InMouth));
        assertTrue(script.state(Body.InMouth).is(Toys.Gag));

        assertFalse(muzzleGag.applied());
        assertFalse(muzzleGag.is(Body.InMouth));
        assertFalse(muzzleGag.is(Toys.Gags.Ring_Gag));
        assertFalse(muzzleGag.is(script.namespace));
    }

    @Test
    public void testQueryDifferentItemsOfAKIndDontInterferShort() {
        TestScript script = TestScript.getOne();
        script.debugger.freezeTime();

        Items gags = script.items(Toys.Gag);
        Item ringGag = gags.query(Toys.Gags.Ring_Gag).get();
        ringGag.apply();
        assertTrue(ringGag.applied());
        assertTrue(ringGag.is(Body.InMouth));
        assertTrue(ringGag.is(Toys.Gags.Ring_Gag));
        assertTrue(ringGag.is(script.namespace));

        Item muzzleGag = gags.query(Toys.Gags.Muzzle_Gag).get();
        assertFalse(muzzleGag.applied());
        assertFalse(muzzleGag.is(Body.InMouth));
        assertFalse(muzzleGag.is(Toys.Gags.Ring_Gag));
        assertFalse(muzzleGag.is(script.namespace));
    }

    @Test
    public void documentDifferenceBetweenItemAndState() {
        TeaseScript script = TestScript.getOne();

        script.item(Household.Clothes_Pegs).applyTo(Body.OnNipples);
        assertTrue(script.item(Body.OnNipples).applied());

        assertTrue(script.item(Household.Clothes_Pegs).applied());
        assertTrue(script.item(Household.Clothes_Pegs).is(Body.OnNipples));
        assertTrue(script.item(Household.Clothes_Pegs).is(script.namespace));

        assertTrue(script.state(Body.OnNipples).is(Household.Clothes_Pegs));
        assertFalse(script.item(Body.OnNipples).is(Household.Clothes_Pegs));
    }
}
