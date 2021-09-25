package teaselib.util;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.junit.Test;

import teaselib.Accessoires;
import teaselib.Body;
import teaselib.Bondage;
import teaselib.Clothes;
import teaselib.Features;
import teaselib.Gadgets;
import teaselib.Household;
import teaselib.Material;
import teaselib.Shoes;
import teaselib.State;
import teaselib.TeaseScript;
import teaselib.TeaseScriptPersistence.Domain;
import teaselib.Toys;
import teaselib.core.state.AbstractProxy;
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
        // TODO Would be true if only a single item existed
        // -> to resolve applying state should also apply default item - review
        assertFalse(script.item(Clothes.Stockings).applied());

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
        assertTrue(script.state(Clothes.Garter_Belt).applied());
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
        TestScript script = TestScript.getOne();

        Items gags = script.items(Toys.Gag);

        assertFalse(gags.anyAvailable());
        assertFalse(gags.get().is(Toys.Gags.Ring_Gag));

        Item ringGag = gags.matching(Toys.Gags.Ring_Gag).get();
        assertTrue(ringGag.is(Toys.Gags.Ring_Gag));

        ringGag.setAvailable(true);
        assertTrue(gags.anyAvailable());

        Items sameGags = script.items(Toys.Gag);
        assertTrue(sameGags.anyAvailable());
        assertTrue(sameGags.get().is(Toys.Gags.Ring_Gag));

        Item sameRingGag = script.item(Toys.Gag);
        assertTrue(sameRingGag.isAvailable());
        assertTrue(sameRingGag.is(Toys.Gags.Ring_Gag));

        Item againTheSameGag = script.items(Toys.Gag).getAvailable().get();
        assertTrue(againTheSameGag.isAvailable());
        assertTrue(againTheSameGag.is(Toys.Gags.Ring_Gag));

        assertEquals(ringGag, sameRingGag);
        assertEquals(ringGag, againTheSameGag);
    }

    @Test
    public void testAvailable() {
        TestScript script = TestScript.getOne();

        Items gags = script.items(Toys.Gag);
        assertFalse(gags.anyAvailable());

        Item ringGag = gags.matching(Toys.Gags.Ring_Gag).get();
        assertTrue(ringGag.is(Toys.Gags.Ring_Gag));

        assertEquals(0, gags.getAvailable().size());
        assertNotEquals(ringGag, script.item(Toys.Gag));
        assertTrue(script.item(Toys.Gag).is(Toys.Gags.Ball_Gag));

        ringGag.setAvailable(true);
        assertTrue(ringGag.isAvailable());

        assertEquals(ringGag, gags.get());

    }

    @Test
    public void testMatching() {
        TestScript script = TestScript.getOne();
        script.addTestUserItems();

        Items gags = script.items(Toys.Gag);

        Items bitGags = gags.matching(Toys.Gags.Bit_Gag, Body.Orifice.Oral);
        assertEquals(1, bitGags.size());

        Item bitGag = bitGags.get();
        Item sameBitGag = gags.matching(Toys.Gags.Bit_Gag).get();

        assertEquals(sameBitGag, bitGag);
    }

    @Test
    public void testGet() {
        TestScript script = TestScript.getOne();

        Items gags = script.items(Toys.Gag);
        assertEquals(Toys.Gags.values().length, gags.size());

        assertNotEquals(Item.NotFound, gags.get());
        assertFalse(gags.get().isAvailable());

        Item bitGag = gags.matching(Toys.Gags.Bit_Gag).get();
        assertTrue(bitGag.is(Toys.Gags.Bit_Gag));

        Item penisGag = gags.matching(Toys.Gags.Penis_Gag).get();
        penisGag.setAvailable(true);

        assertEquals(penisGag, gags.get());
        assertEquals(bitGag, gags.matching(Toys.Gags.Bit_Gag).get());

        assertFalse(gags.matching(Toys.Gags.Bit_Gag).get().isAvailable());
        assertTrue(gags.matching(Toys.Gags.Penis_Gag).get().isAvailable());

        assertEquals(penisGag, gags.get());
        assertTrue(gags.matching(Toys.Gags.Penis_Gag).get().isAvailable());

        Item noRingGag = gags.prefer(Toys.Gags.Ring_Gag).get();
        assertEquals(penisGag, noRingGag);

        Item availablePenisGag = gags.prefer(Toys.Gags.Penis_Gag).get();
        assertEquals(penisGag, availablePenisGag);
    }

    // TODO Add more tests for prefer() in order to find out if the method makes sense
    // TODO Add tests for best() in order to find out if the method makes sense

    @Test
    public void testContains() {
        TeaseScript script = TestScript.getOne();
        Items gags = script.items(Toys.Gag);
        assertEquals(Toys.Gags.values().length, gags.size());

        assertTrue(gags.contains(Toys.Gag));
        assertFalse(gags.contains(Toys.Buttplug));

        assertTrue(gags.contains("teaselib.Toys.Gag"));
        assertFalse(gags.contains("teaselib.Toys.Buttplug"));

        assertTrue(gags.contains(script.item(Toys.Gag)));
        assertFalse(gags.contains(script.item(Toys.Buttplug)));
    }

    @Test
    public void testAny() {
        TestScript script = TestScript.getOne();

        Items gags = script.items(Toys.Gag);
        assertEquals(Toys.Gags.values().length, gags.size());

        assertNotEquals(Item.NotFound, gags.get());
        assertFalse(gags.get().isAvailable());
        for (Item item : script.items(Toys.Gag)) {
            assertFalse(item.isAvailable());
        }
        assertFalse(gags.anyAvailable());
        assertFalse(script.items(Toys.Collar).anyAvailable());

        Item penisGag = gags.matching(Toys.Gags.Penis_Gag).get();
        penisGag.setAvailable(true);

        assertTrue(gags.anyAvailable());
        assertEquals(1, script.items(Toys.Gag).getAvailable().size());
        assertEquals(penisGag, script.items(Toys.Gag).getAvailable().get());
    }

    @Test
    public void testItemsNone() {
        assertFalse(Items.None.anyAvailable());
        assertFalse(Items.None.allAvailable());
        assertFalse(Items.None.anyApplied());
        assertFalse(Items.None.allApplied());
        assertFalse(Items.None.anyApplicable());
        assertFalse(Items.None.allApplicable());
        assertFalse(Items.None.anyAre("foobar"));
        assertFalse(Items.None.allAre("foobar"));
        assertTrue(Items.None.anyExpired());
        assertTrue(Items.None.allExpired());

        TestScript script = TestScript.getOne();
        Items gags = script.items(Toys.values());

        assertFalse(gags.equals(Items.None));
        assertFalse(Items.None.equals(gags));
        assertTrue(Items.None.equals(Items.None));
        assertTrue(gags.equals(gags));

        Items none = new Items(new Item[] {});
        assertTrue(none.equals(Items.None));
        assertTrue(Items.None.equals(none));
    }

    @Test
    public void testAll() {
        TestScript script = TestScript.getOne();
        Items gags = script.items(Toys.values());
        assertFalse(gags.allAvailable());

        gags.get().setAvailable(true);
        assertTrue(gags.anyAvailable());
        assertFalse(gags.allAvailable());

        script.setAvailable(Toys.values());
        assertTrue(gags.allAvailable());
    }

    @Test
    public void testRetainIsLogicalAnd() {
        TestScript script = TestScript.getOne();

        Items buttPlugs = script.items(Toys.Buttplug);
        assertTrue(buttPlugs.size() > 1);

        Item analBeads = buttPlugs.matching(Toys.Anal.Beads).get();
        assertNotEquals(Item.NotFound, analBeads);

        Items allAnalbeads = script.items(Toys.Buttplug).matching(Toys.Anal.Beads);
        assertEquals(1, allAnalbeads.size());
        assertEquals(analBeads, allAnalbeads.get());
    }

    @Test
    public void testGetDoesntSearchForPeersOrAttributes() {
        TeaseScript script = TestScript.getOne();
        Items chainedUp = script.items(Toys.Wrist_Restraints, Toys.Ankle_Restraints, Bondage.Chains);

        Item chains = chainedUp.get(Bondage.Chains);
        assertEquals(QualifiedItem.of(Bondage.Chains), QualifiedItem.of(AbstractProxy.itemImpl(chains).value()));
        chainedUp.get(Toys.Wrist_Restraints).applyTo(Bondage.Chains);
        assertEquals(QualifiedItem.of(Bondage.Chains), QualifiedItem.of(AbstractProxy.itemImpl(chains).value()));
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
        assertEquals(0, collar.duration().remaining(TimeUnit.SECONDS));
    }

    @Test
    public void testRemainingDurationAfterRemoved() {
        TestScript script = TestScript.getOne();
        script.debugger.freezeTime();

        Item collar = script.item(Toys.Collar);
        collar.apply().over(1, TimeUnit.HOURS);
        assertEquals(1, collar.duration().remaining(TimeUnit.HOURS));

        script.debugger.advanceTime(20, TimeUnit.MINUTES);
        assertEquals(40, collar.duration().remaining(TimeUnit.MINUTES));

        collar.remove();
        assertEquals(40, collar.duration().remaining(TimeUnit.MINUTES));
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
    public void testVarietiesNone() {
        TestScript script = TestScript.getOne();
        Items none = script.items(new Enum<?>[] {});
        assertTrue(none.varieties().isEmpty());
    }

    @Test
    public void testVarietiesAll() {
        TestScript script = TestScript.getOne();
        script.addTestUserItems();
        script.addTestUserItems2();

        Items inventory = script.items(Toys.Collar, Toys.Ankle_Restraints, Toys.Wrist_Restraints, Bondage.Chains);
        Varieties<Items> all = inventory.varieties();
        assertEquals(4, all.size());
    }

    @Test
    public void testVarietiesBestAtributes() {
        TestScript script = TestScript.getOne();
        script.addTestUserItems();
        script.addTestUserItems2();

        Items inventory = script.items(Toys.Collar, Toys.Ankle_Restraints, Toys.Wrist_Restraints, Bondage.Chains);
        Items restraints = inventory.prefer(Features.Lockable, Material.Leather);
        assertEquals(4, restraints.size());

        Item collar = restraints.get(Toys.Collar);
        Item anklecuffs = restraints.get(Toys.Ankle_Restraints);
        Item wristCuffs = restraints.get(Toys.Wrist_Restraints);
        Item chains = restraints.get(Bondage.Chains);

        assertNotEquals(Item.NotFound, collar);
        assertNotEquals(Item.NotFound, anklecuffs);
        assertNotEquals(Item.NotFound, wristCuffs);
        assertNotEquals(Item.NotFound, chains);
    }

    @Test
    public void testVarietiesBestAppliedInSet() {
        TestScript script = TestScript.getOne();
        script.addTestUserItems();
        script.addTestUserItems2();

        Items inventory = script.items(Toys.Collar, Toys.Ankle_Restraints, Toys.Wrist_Restraints, Bondage.Chains);
        testAnyWithAppliedItem(inventory, Material.Leather);
        testAnyWithAppliedItem(inventory, Material.Metal);
    }

    private static void testAnyWithAppliedItem(Items inventory, Material material) {
        Varieties<Items> all = inventory.varieties();
        assertEquals(4, all.size());

        Items cuffsWithMaterial = inventory.matching(Toys.Wrist_Restraints, material);
        assertFalse(cuffsWithMaterial.equals(Items.None));
        assertEquals(1, cuffsWithMaterial.size());

        cuffsWithMaterial.apply();
        Items appliedCuffs = all.reduce(Items::best);
        assertTrue(appliedCuffs.get(Toys.Wrist_Restraints).is(material));
    }

    @Test
    public void testVarietiesBestAppliedNotInSet() {
        TestScript script = TestScript.getOne();
        script.addTestUserItems();
        script.addTestUserItems2();

        Items allMetal = script.items(Toys.Wrist_Restraints, Toys.Humbler).prefer(Material.Metal);
        assertTrue(allMetal.item(Toys.Wrist_Restraints).is(Material.Metal));
        assertTrue(allMetal.item(Toys.Humbler).is(Material.Metal));

        script.items(Toys.Wrist_Restraints).matching(Material.Leather).get().apply();
        Items alreadyApplied = script.items(Toys.Wrist_Restraints, Toys.Humbler).prefer(Material.Metal);
        assertTrue(alreadyApplied.item(Toys.Wrist_Restraints).is(Material.Leather));
        assertTrue(alreadyApplied.item(Toys.Humbler).is(Material.Wood));
    }

    @Test
    public void testItemsSubList() {
        TestScript script = TestScript.getOne();

        Item wristRestraints = script.item(Toys.Wrist_Restraints);
        assertTrue(wristRestraints.is(Toys.Wrist_Restraints));

        Item wristRestraints2 = script.items(Toys.Wrist_Restraints).items(Toys.Wrist_Restraints).get();
        assertNotEquals(Item.NotFound, wristRestraints2);
        assertTrue(wristRestraints2.is(Toys.Wrist_Restraints));

        Item notFound = script.items(Toys.Wrist_Restraints).items(Toys.Humbler).get();
        assertEquals(Item.NotFound, notFound);
    }

    @Test
    public void testItemValues() {
        TestScript script = TestScript.getOne();
        script.addTestUserItems();

        Items restraints = script.items(Toys.Wrist_Restraints, Toys.Ankle_Restraints);
        assertEquals(4, restraints.size());

        List<Object> values = new ArrayList<>(restraints.valueSet());
        assertEquals(2, values.size());
        assertEquals(QualifiedItem.of(Toys.Wrist_Restraints), values.get(0));
        assertEquals(QualifiedItem.of(Toys.Ankle_Restraints), values.get(1));
    }

    @Test
    public void testAnyApplied() {
        TestScript script = TestScript.getOne();

        Items restraints = script.items(Toys.Wrist_Restraints, Toys.Ankle_Restraints).matching(Material.Metal);
        assertEquals(2, restraints.size());
        assertFalse(restraints.anyApplied());

        restraints.item(Toys.Wrist_Restraints).apply();
        assertTrue(restraints.anyApplied());
        assertFalse(restraints.allApplied());

        restraints.item(Toys.Ankle_Restraints).apply();
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

        Item leatherCuffs = script.items(Toys.Wrist_Restraints).matching(Material.Leather).get();
        Item handCuffs = script.items(Toys.Wrist_Restraints).matching(Material.Metal).get();

        assertNotEquals(leatherCuffs, handCuffs);

        leatherCuffs.apply();

        assertTrue(script.state(Body.WristsTied).is(leatherCuffs));
        assertFalse(script.state(Body.WristsTied).is(handCuffs));

        assertTrue(leatherCuffs.applied());
        assertFalse(handCuffs.applied());
    }

    @Test
    public void testAvailableItems() {
        TestScript script = TestScript.getOne();
        script.addTestUserItems();

        Items restraints = script.items(Toys.Wrist_Restraints);
        assertEquals(2, restraints.size());

        assertEquals(0, restraints.getAvailable().size());
        restraints.get().setAvailable(true);
        assertEquals(1, restraints.getAvailable().size());
        assertFalse(restraints.allAvailable());
        assertTrue(restraints.preferred().allAvailable());
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

    // TODO test that query doesn't work with namespace and show how to do it right

    @Test
    public void testMatchingItemInstanceAttributesDontInterferWithApplied() {
        TestScript script = TestScript.getOne();
        script.debugger.freezeTime();

        Items gags1 = script.items(Toys.Gag);
        Item ringGag = gags1.matching(Toys.Gags.Ring_Gag).get();
        assertTrue(ringGag.is(Toys.Gags.Ring_Gag));
        ringGag.apply();
        assertTrue(ringGag.applied());
        assertTrue(script.state(Body.InMouth).is(ringGag));
        // assertTrue(script.state(Toys.Gag).is(ringGag)); // ???

        Items gags2 = script.items(Toys.Gag);
        Item ringGag2 = gags2.matching(Toys.Gags.Ring_Gag).get();
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
        Item muzzleGag = gags3.matching(Toys.Gags.Muzzle_Gag).get();

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
    public void testMatchingDifferentItemsOfAKIndDontInterferShort() {
        TestScript script = TestScript.getOne();
        script.debugger.freezeTime();

        Items gags = script.items(Toys.Gag);
        Item ringGag = gags.matching(Toys.Gags.Ring_Gag).get();
        ringGag.apply();
        assertTrue(ringGag.applied());
        assertTrue(ringGag.is(Body.InMouth));
        assertTrue(ringGag.is(Toys.Gags.Ring_Gag));
        assertTrue(ringGag.is(script.namespace));

        Item muzzleGag = gags.matching(Toys.Gags.Muzzle_Gag).get();
        assertFalse(muzzleGag.applied());
        assertFalse(muzzleGag.is(Body.InMouth));
        assertFalse(muzzleGag.is(Toys.Gags.Ring_Gag));
        assertFalse(muzzleGag.is(script.namespace));
    }

    @Test
    public void documentThatApplyingStateDoesntApplyItems() {
        TeaseScript script = TestScript.getOne();

        script.item(Household.Clothes_Pegs).applyTo(Body.OnNipples);

        assertTrue(script.state(Household.Clothes_Pegs).applied());
        assertTrue(script.state(Household.Clothes_Pegs).is(Body.OnNipples));
        assertTrue(script.state(Household.Clothes_Pegs).is(script.namespace));

        assertTrue(script.item(Household.Clothes_Pegs).applied());
        assertTrue(script.item(Household.Clothes_Pegs).is(Body.OnNipples));
        assertTrue(script.item(Household.Clothes_Pegs).is(script.namespace));

        assertTrue(script.state(Body.OnNipples).applied());
        assertTrue(script.state(Body.OnNipples).is(Household.Clothes_Pegs));

        assertFalse(script.item(Body.OnNipples).applied());
        assertFalse(script.item(Body.OnNipples).is(Household.Clothes_Pegs));
    }

    @Test
    public void testApplyToRemoveFrom() {
        TeaseScript script = TestScript.getOne();
        State pegs = script.state(Household.Clothes_Pegs);
        State nips = script.state(Body.OnNipples);

        script.item(Household.Clothes_Pegs).applyTo(Body.OnNipples);
        assertTrue(nips.applied());
        assertTrue(nips.is(Household.Clothes_Pegs));
        assertTrue(pegs.applied());
        assertTrue(pegs.is(Household.Clothes_Pegs));

        script.item(Household.Clothes_Pegs).removeFrom(Body.OnNipples);
        assertFalse(nips.applied());
        assertFalse(nips.is(Household.Clothes_Pegs));
        assertFalse(pegs.applied());
        assertFalse(pegs.is(Household.Clothes_Pegs));
    }

    @Test
    public void testThatApplyingItemsIsCompatibleToApplyingSingleItem() {
        TestScript script = TestScript.getOne();
        script.addTestUserItems();

        Items restraints = script.items(Toys.Wrist_Restraints);
        assertEquals(2, restraints.size());

        restraints.apply();

        Items applied = script.items(Toys.Wrist_Restraints).getApplied();
        assertEquals(1, applied.size());
    }

    @Test
    public void testRemovingMultipleItems() {
        TestScript script = TestScript.getOne();
        script.addTestUserItems();

        Items ballTorture = script.items(Toys.Ball_Stretcher, Toys.Humbler);
        assertEquals(2, ballTorture.size());

        ballTorture.apply();
        assertEquals(2, ballTorture.getApplied().size());
        ballTorture.remove();
        assertEquals(0, ballTorture.getApplied().size());

        ballTorture.apply();
        assertEquals(2, ballTorture.getApplied().size());

        script.item(Toys.Humbler).remove();
        assertEquals(1, ballTorture.getApplied().size());
        script.item(Toys.Ball_Stretcher).remove();
        assertEquals(0, ballTorture.getApplied().size());
    }

    @Test
    public void testRemoveSingleItem() {
        TestScript script = TestScript.getOne();
        script.addTestUserItems();

        Items gags = script.items(Toys.Gag);
        assertEquals(5, gags.size());

        Iterator<Item> gag = gags.iterator();
        Item gag0 = gag.next();
        Item gag1 = gag.next();
        State inMouth = script.state(Body.InMouth);

        gag0.apply();
        assertTrue(gag0.applied());
        assertFalse(gag1.applied());

        gag1.apply();
        assertTrue(gag0.applied());
        assertTrue(gag1.applied());

        gag0.remove();
        assertEquals(1, gags.getApplied().size());
        assertTrue(inMouth.applied());

        gag1.remove();
        assertEquals(0, gags.getApplied().size());
        assertFalse(inMouth.applied());
    }

    @Test
    public void testRemoveSingleItemImplicit() {
        TestScript script = TestScript.getOne();
        script.addTestUserItems();

        Items gags = script.items(Toys.Gag);
        assertEquals(5, gags.size());

        Iterator<Item> gag = gags.iterator();
        Item gag0 = gag.next();
        Item gag1 = gag.next();
        State inMouth = script.state(Body.InMouth);

        gag0.applyTo(Body.InMouth);
        assertTrue(gag0.applied());
        assertFalse(gag1.applied());

        gag1.applyTo(Body.InMouth);
        assertTrue(gag0.applied());
        assertTrue(gag1.applied());

        gag0.remove();
        assertFalse(gag0.applied());
        assertTrue(gag1.applied());
        assertEquals(1, gags.getApplied().size());
        assertTrue(inMouth.applied());

        gag1.remove();
        assertEquals(0, gags.getApplied().size());
        assertFalse(inMouth.applied());
    }

    @Test
    public void testRemoveFromSingleItemExplicit() {
        TestScript script = TestScript.getOne();
        script.addTestUserItems();

        Items gags = script.items(Toys.Gag);
        assertEquals(5, gags.size());

        Iterator<Item> gag = gags.iterator();
        Item gag0 = gag.next();
        Item gag1 = gag.next();
        State inMouth = script.state(Body.InMouth);

        gag0.applyTo(Body.InMouth);
        assertTrue(gag0.applied());
        assertFalse(gag1.applied());

        gag1.applyTo(Body.InMouth);
        assertTrue(gag0.applied());
        assertTrue(gag1.applied());

        // TODO should be gag0.defaultPeers()
        gag0.removeFrom(Body.InMouth, Body.CantLick);
        assertFalse(gag0.applied());
        // TODO State removed too early
        assertTrue(gag1.applied());
        assertEquals(1, gags.getApplied().size());
        assertTrue(inMouth.applied());

        // TODO should be gag1.defaultPeers()
        gag1.removeFrom(Body.InMouth, Body.CantLick);
        assertEquals(0, gags.getApplied().size());
        assertFalse(inMouth.applied());
    }

    @Test
    public void testThatRemovingItemsIsCompatibleToRemovingSingleItem() {
        TestScript script = TestScript.getOne();
        script.addTestUserItems();

        Items restraints = script.items(Toys.Wrist_Restraints);
        assertEquals(2, restraints.size());

        restraints.stream().forEach(Item::apply);
        assertEquals(2, restraints.getApplied().size());

        restraints.remove();
        assertEquals(1, restraints.getApplied().size());
    }

    @Test
    public void testThatRemovingPartiallyAppliedItemsRequiresGetApplied() {
        TestScript script = TestScript.getOne();
        script.addTestUserItems();

        Items restraints = script.items(Toys.Wrist_Restraints, Toys.Ankle_Restraints);
        assertEquals(4, restraints.size());

        restraints.item(Toys.Wrist_Restraints).apply();
        assertEquals(1, restraints.getApplied().size());

        try {
            restraints.remove();
            fail("Only applied items can be removed");
        } catch (IllegalStateException e) { // expected
        }

        restraints.getApplied().remove();
        assertEquals(0, restraints.getApplied().size());
    }

    @Test
    public void testOrElseItemsEnum() {
        TestScript script = TestScript.getOne();
        script.addTestUserItems();

        Items restraints1 = script.items(Toys.Wrist_Restraints);
        assertEquals(2, restraints1.size());

        Items restraints2 = script.items(Toys.Wrist_Restraints).orElseItems(Toys.Wrist_Restraints);
        assertEquals(restraints1, restraints2);
    }

    @Test
    public void testOrElseItemsString() {
        TestScript script = TestScript.getOne();
        script.addTestUserItems();

        Items restraints1 = script.items("teaselib.Toys.Wrist_Restraints");
        assertEquals(2, restraints1.size());

        Items restraints2 = script.items("teaselib.Toys.Wrist_Restraints")
                .orElseItems("teaselib.Toys.Wrist_Restraints");
        assertEquals(restraints1, restraints2);
    }

    @Test
    public void testOrElsePreferEnum() {
        TestScript script = TestScript.getOne();
        script.addTestUserItems();
        script.items(Toys.Wrist_Restraints).matching(Material.Metal).get().setAvailable(true);

        Items restraints = script.items(Toys.Wrist_Restraints).matching(Material.Wood).orElsePrefer(Material.Metal);
        assertTrue(restraints.allAre(Material.Metal));
    }

    @Test
    public void testOrElsePreferString() {
        TestScript script = TestScript.getOne();
        script.addTestUserItems();
        script.items(Toys.Wrist_Restraints).matching(Material.Metal).get().setAvailable(true);

        Items restraints = script.items("teaselib.Toys.Wrist_Restraints").matching("teaselib.Material.Wood")
                .orElsePrefer("teaselib.Material.Metal");

        assertTrue(restraints.allAre("teaselib.Material.Metal"));
    }

    @Test
    public void testOrElseMatchingEnum() {
        TestScript script = TestScript.getOne();
        script.addTestUserItems();
        script.items(Toys.Wrist_Restraints).matching(Material.Metal).get().setAvailable(true);

        Items restraints = script.items(Toys.Wrist_Restraints).matching(Material.Wood).orElseMatching(Material.Metal);
        assertTrue(restraints.allAre(Material.Metal));
    }

    @Test
    public void testOrElseMatchingString() {
        TestScript script = TestScript.getOne();
        script.addTestUserItems();
        script.items(Toys.Wrist_Restraints).matching(Material.Metal).get().setAvailable(true);

        Items restraints = script.items("teaselib.Toys.Wrist_Restraints").matching("teaselib.Material.Wood")
                .orElseMatching("teaselib.Material.Metal");
        assertTrue(restraints.allAre("teaselib.Material.Metal"));
    }

    @Test
    public void testWithoutEnum() {
        TestScript script = TestScript.getOne();

        Items all = script.items(Toys.Wrist_Restraints, Toys.Ankle_Restraints);
        assertEquals(4, all.size());
        assertEquals(script.item(Toys.Ankle_Restraints), all.item(Toys.Ankle_Restraints));
        assertEquals(script.item(Toys.Wrist_Restraints), all.item(Toys.Wrist_Restraints));

        Items without = all.without(Toys.Ankle_Restraints);
        assertEquals(2, without.size());

        assertEquals(Item.NotFound, without.item(Toys.Ankle_Restraints));
        assertEquals(script.item(Toys.Wrist_Restraints), without.item(Toys.Wrist_Restraints));

        assertEquals(script.item(Toys.Wrist_Restraints),
                without.orElseItems(Toys.Ankle_Restraints, Toys.Wrist_Restraints).item(Toys.Wrist_Restraints));
        assertEquals(script.item(Toys.Ankle_Restraints),
                without.orElseItems(Toys.Ankle_Restraints).item(Toys.Ankle_Restraints));

        assertEquals(2, all.without(Material.Metal).size());
    }

    @Test
    public void testWithoutString() {
        TestScript script = TestScript.getOne();

        Items all = script.items("teaselib.Toys.Wrist_Restraints", "teaselib.Toys.Ankle_Restraints");
        assertEquals(4, all.size());
        assertEquals(script.item("teaselib.Toys.Ankle_Restraints"), all.item("teaselib.Toys.Ankle_Restraints"));
        assertEquals(script.item("teaselib.Toys.Wrist_Restraints"), all.item("teaselib.Toys.Wrist_Restraints"));

        Items without = all.without("teaselib.Toys.Ankle_Restraints");
        assertEquals(2, without.size());

        assertEquals(Item.NotFound, without.item("teaselib.Toys.Ankle_Restraints"));
        assertEquals(script.item("teaselib.Toys.Wrist_Restraints"), without.item("teaselib.Toys.Wrist_Restraints"));

        assertEquals(script.item(Toys.Wrist_Restraints),
                without.orElseItems("teaselib.Toys.Ankle_Restraints", "teaselib.Toys.Wrist_Restraints")
                        .item("teaselib.Toys.Wrist_Restraints"));
        assertEquals(script.item(Toys.Ankle_Restraints),
                without.orElseItems("teaselib.Toys.Ankle_Restraints").item("teaselib.Toys.Ankle_Restraints"));

        assertEquals(2, all.without(Material.Metal).size());
    }

    @Test
    public void testItemAppliedToItems() {
        TestScript script = TestScript.getOne();

        Items restraints = script.items(Toys.Wrist_Restraints, Toys.Ankle_Restraints, Toys.Collar);
        restraints.apply();
        Items chains = script.items(Bondage.Chains, Accessoires.Bells);
        chains.applyTo(restraints);

        Item singleChainItem = chains.get(Bondage.Chains);
        Item bells = chains.get(Accessoires.Bells);
        State bellsState = script.state(Accessoires.Bells);
        assertTrue(bells.applied());
        assertTrue(bells.is(Toys.Wrist_Restraints));

        Item wristRestraints = restraints.get(Toys.Wrist_Restraints);
        State wristRestraintsState = script.state(Toys.Wrist_Restraints);
        assertTrue(bellsState.is(wristRestraintsState));
        assertTrue(wristRestraintsState.is(bellsState));

        assertTrue(bells.is(wristRestraints));
        assertTrue(wristRestraints.is(bells));
        assertTrue(bells.is(restraints));

        assertFalse(bells.is(singleChainItem));
        assertFalse(singleChainItem.is(bells));
        assertFalse(bells.is(chains));
        assertTrue(chains.anyAre(bells));
        assertFalse(chains.allAre(bells));

        singleChainItem.remove();
        assertTrue(bells.applied());
        assertTrue(bells.is(Toys.Wrist_Restraints));
        assertTrue(bells.is(wristRestraints));
        assertTrue(bells.is(restraints));
    }

    @Test
    public void testDomain() {
        TestScript script = TestScript.getOne();
        Domain domain = script.domain(Gadgets.Key_Release);

        Items items = script.items(Toys.Wrist_Restraints);
        Items handled = items.of(domain);

        assertEquals(items.size(), handled.size());
        assertEquals(script.defaultDomain.toString(), AbstractProxy.itemImpl(items.get()).domain);
        assertEquals(domain.toString(), AbstractProxy.itemImpl(handled.get()).domain);
    }

    @Test
    public void testItemsStatement() {
        TestScript script = TestScript.getOne();

        Items myOutfit = script.items(Toys.Collar, Shoes.High_Heels);
        assertEquals(2, myOutfit.size());

        Items allShoes = script.items(Shoes.All);
        assertEquals(5, allShoes.size());

        // TODO using Statement.values isn't a query -> change to (package-) private field
        Items myShoes = myOutfit.items(Shoes.All.values);
        assertEquals(1, myShoes.size());

        Items myShoes2 = myOutfit.items(Shoes.All);
        assertEquals(1, myShoes2.size());

    }
}
