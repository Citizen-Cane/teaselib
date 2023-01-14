package teaselib.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static teaselib.Body.OnNipples;
import static teaselib.Bondage.Ankle_Restraints;
import static teaselib.Bondage.Chains;
import static teaselib.Bondage.Wrist_Restraints;
import static teaselib.Toys.Collar;
import static teaselib.Toys.Nipple_Clamps;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
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
import teaselib.TeaseScriptPersistence.Domain;
import teaselib.Toys;
import teaselib.core.state.AbstractProxy;
import teaselib.core.state.ItemProxy;
import teaselib.core.util.QualifiedString;
import teaselib.test.TestScript;
import teaselib.util.Item;
import teaselib.util.Items;
import teaselib.util.Items.Query;
import teaselib.util.Select;
import teaselib.util.math.Varieties;

public class ItemsTest {
    @Test
    public void testSimpleApplyWithStates() throws IOException {
        try (TestScript script = new TestScript()) {
            script.state(Clothes.Stockings).apply();
            assertTrue(script.state(Clothes.Stockings).applied());
            // TODO Would be true if only a single item existed
            // -> to resolve applying state should also apply default item - review
            assertFalse(script.item(Clothes.Stockings).applied());

            script.item(Clothes.Garter_Belt).setAvailable(true);
            Items items = script.items(Clothes.Garter_Belt).getAvailable();
            items.apply();
            assertTrue(script.state(Clothes.Garter_Belt).applied());
            assertTrue(script.item(Clothes.Garter_Belt).applied());
        }
    }

    @Test
    public void testSimpleApplyWithoutDefaultPeers() throws IOException {
        try (TestScript script = new TestScript()) {
            script.item(Clothes.Stockings).apply();
            assertTrue(script.state(Clothes.Stockings).applied());
            assertTrue(script.item(Clothes.Stockings).applied());

            script.item(Clothes.Garter_Belt).setAvailable(true);
            Items items = script.items(Clothes.Garter_Belt).getAvailable();
            items.apply();
            assertTrue(script.state(Clothes.Garter_Belt).applied());
            assertTrue(script.item(Clothes.Garter_Belt).applied());
        }
    }

    @Test
    public void testSimpleApplyWithDefaultPeers() throws IOException {
        try (TestScript script = new TestScript()) {
            Item gag = script.item(Toys.Gag);
            gag.apply();
            assertTrue(script.item(Toys.Gag).applied());

            script.item(Toys.Buttplug).setAvailable(true);
            Items buttPlugs = script.items(Toys.Buttplug).getAvailable();
            buttPlugs.apply();
            assertTrue(script.item(Toys.Buttplug).applied());
        }
    }

    @Test
    public void testGetAvailableItemsFirst() throws IOException {
        try (TestScript script = new TestScript()) {
            Items.Collection gags = script.items(Toys.Gag).inventory();
            assertFalse(gags.anyAvailable());

            Item ringGag = gags.matching(Toys.Gags.Ring_Gag).get();
            assertTrue(ringGag.is(Toys.Gags.Ring_Gag));

            ringGag.setAvailable(true);
            assertTrue(gags.anyAvailable());

            Items.Collection sameGags = script.items(Toys.Gag).inventory();
            assertTrue(sameGags.anyAvailable());
            assertTrue(sameGags.get().is(Toys.Gags.Ring_Gag));

            Item sameRingGag = script.item(Toys.Gag);
            assertTrue(sameRingGag.isAvailable());
            assertTrue(sameRingGag.is(Toys.Gags.Ring_Gag));

            Item againTheSameGag = script.items(Toys.Gag).item();
            assertTrue(againTheSameGag.isAvailable());
            assertTrue(againTheSameGag.is(Toys.Gags.Ring_Gag));

            assertEquals(ringGag, sameRingGag);
            assertEquals(ringGag, againTheSameGag);
        }
    }

    @Test
    public void testAvailable() throws IOException {
        try (TestScript script = new TestScript()) {
            Items.Collection gags = script.items(Toys.Gag).inventory();
            assertFalse(gags.anyAvailable());
            assertEquals(0, gags.getAvailable().size());

            Item ringGag = gags.matching(Toys.Gags.Ring_Gag).get();
            assertTrue(ringGag.is(Toys.Gags.Ring_Gag));

            Item ballGag = script.items(Toys.Gag).matching(Toys.Gags.Ball_Gag).item();
            assertTrue(ballGag.is(Toys.Gags.Ball_Gag));

            ringGag.setAvailable(true);
            assertTrue(ringGag.isAvailable());
            assertEquals(ringGag, gags.get());
        }
    }

    @Test
    public void testMatching() throws IOException {
        try (TestScript script = new TestScript()) {
            script.addTestUserItems();
            Items.Collection gags = script.items(Toys.Gag).inventory();

            Items.Collection bitGags = gags.matching(Toys.Gags.Bit_Gag, Body.Orifice.Oral);
            assertEquals(1, bitGags.size());

            Item bitGag = bitGags.get();
            Item sameBitGag = gags.matching(Toys.Gags.Bit_Gag).get();

            assertEquals(sameBitGag, bitGag);
        }
    }

    @Test
    public void testGet() throws IOException {
        try (TestScript script = new TestScript()) {
            Items.Collection gags = script.items(Toys.Gag).inventory();
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
    }

    // TODO Add more tests for prefer() in order to find out if the method makes sense
    // TODO Add tests for best() in order to find out if the method makes sense

    @Test
    public void testContains() throws IOException {
        try (TestScript script = new TestScript()) {
            Items gags = script.items(Toys.Gag).inventory();
            assertEquals(Toys.Gags.values().length, gags.size());

            assertTrue(gags.contains(Toys.Gag));
            assertFalse(gags.contains(Toys.Buttplug));

            assertTrue(gags.contains("teaselib.Toys.Gag"));
            assertFalse(gags.contains("teaselib.Toys.Buttplug"));

            assertTrue(gags.contains(script.item(Toys.Gag)));
            assertFalse(gags.contains(script.item(Toys.Buttplug)));
        }
    }

    @Test
    public void testAnyAvailable() throws IOException {
        try (TestScript script = new TestScript()) {
            Items.Collection gags = script.items(Toys.Gag).inventory();
            assertEquals(Toys.Gags.values().length, gags.size());

            assertNotEquals(Item.NotFound, gags.get());
            assertFalse(gags.get().isAvailable());
            for (Item item : script.items(Toys.Gag).inventory()) {
                assertFalse(item.isAvailable());
            }
            assertFalse(gags.anyAvailable());
            assertFalse(script.items(Toys.Collar).anyAvailable());

            Item penisGag = gags.matching(Toys.Gags.Penis_Gag).get();
            penisGag.setAvailable(true);

            assertTrue(gags.anyAvailable());
            assertEquals(1, script.items(Toys.Gag).getApplicable().size());
            assertEquals(penisGag, script.items(Toys.Gag).getApplicable().get());
        }
    }

    @Test
    public void testItemsNone() throws IOException {
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

        try (TestScript script = new TestScript()) {
            var gags = script.items(Toys.values()).inventory();

            assertFalse(gags.equals(Items.None));
            assertFalse(Items.None.equals(gags));
            assertTrue(Items.None.equals(Items.None));
            assertTrue(gags.equals(gags));

            Items none = script.items(new Item[] {});
            assertTrue(none.equals(Items.None));
            assertTrue(Items.None.equals(none));
        }
    }

    @Test
    public void testAllAvailable() throws IOException {
        try (TestScript script = new TestScript()) {
            Items.Collection toys = script.items(Toys.All).inventory();
            assertFalse(toys.anyAvailable());

            toys.get().setAvailable(true);
            assertTrue(toys.anyAvailable());
            assertFalse(toys.allAvailable());

            script.setAvailable(Toys.values());
            assertTrue(toys.allAvailable());
        }
    }

    @Test
    public void testApplicableSet() throws IOException {
        try (TestScript script = new TestScript()) {
            var collar = script.item(Toys.Collar);
            var handcuffs = script.item(Bondage.Wrist_Restraints);
            var anklecuffs = script.item(Bondage.Ankle_Restraints);

            handcuffs.setAvailable(true);
            collar.apply();

            var cuffs = script.items(Toys.Collar, Bondage.Wrist_Restraints, Bondage.Ankle_Restraints);
            assertTrue(cuffs.anyApplicable());
            assertFalse(cuffs.allApplicable());

            var items = cuffs.getApplicableSet();
            assertTrue(items.anyApplicable());
            assertTrue(items.allApplicable());

            items.apply();
            assertFalse(cuffs.anyApplicable());
            assertFalse(cuffs.allApplied());

            anklecuffs.apply();
            assertTrue(cuffs.noneApplicable());
            assertTrue(cuffs.allApplied());
        }
    }

    @Test
    public void testRetainIsLogicalAnd() throws IOException {
        try (TestScript script = new TestScript()) {
            Items.Collection buttPlugs = script.items(Toys.Buttplug).inventory();
            assertTrue(buttPlugs.size() > 1);

            Item analBeads = buttPlugs.matching(Toys.Anal.Beads).get();
            assertNotEquals(Item.NotFound, analBeads);

            Items.Collection allAnalbeads = script.items(Toys.Buttplug).matching(Toys.Anal.Beads).inventory();
            assertEquals(1, allAnalbeads.size());
            assertEquals(analBeads, allAnalbeads.get());
        }
    }

    @Test
    public void testGetDoesntSearchForPeersOrAttributes() throws IOException {
        try (TestScript script = new TestScript()) {
            script.setAvailable(Toys.All, Household.All, Bondage.All);
            var chainedUp = script.items(Bondage.Wrist_Restraints, Bondage.Ankle_Restraints, Bondage.Chains)
                    .getApplicableSet();

            Item chains = chainedUp.get(Bondage.Chains);
            assertEquals(QualifiedString.of(Bondage.Chains), QualifiedString.of(AbstractProxy.itemImpl(chains).kind()));
            chainedUp.get(Bondage.Wrist_Restraints).to(Bondage.Chains).apply();
            assertEquals(QualifiedString.of(Bondage.Chains), QualifiedString.of(AbstractProxy.itemImpl(chains).kind()));
        }
    }

    @Test
    public void testApplyToDefault() throws IOException {
        try (TestScript script = new TestScript()) {
            Item collar = script.item(Toys.Collar);
            assertFalse(collar.isAvailable());

            assertTrue(collar.is(Toys.Collar));
            assertFalse(collar.is(Body.AroundNeck));

            collar.apply();

            assertTrue(script.state(Body.AroundNeck).applied());
            assertTrue(collar.is(Body.AroundNeck));
        }
    }

    @Test
    public void testRemainingDurationIsNegativeIfNotApplied() throws IOException {
        try (TestScript script = new TestScript()) {
            Item collar = script.item(Toys.Collar);
            assertFalse(collar.applied());
            assertEquals(0, collar.duration().remaining(TimeUnit.SECONDS));
        }
    }

    @Test
    public void testRemainingDurationAfterRemoved() throws IOException {
        try (TestScript script = new TestScript()) {
            script.debugger.freezeTime();

            Item collar = script.item(Toys.Collar);
            collar.apply().over(1, TimeUnit.HOURS);
            assertEquals(1, collar.duration().remaining(TimeUnit.HOURS));

            script.debugger.advanceTime(20, TimeUnit.MINUTES);
            assertEquals(40, collar.duration().remaining(TimeUnit.MINUTES));

            collar.remove();
            assertEquals(40, collar.duration().remaining(TimeUnit.MINUTES));
        }
    }

    @Test
    public void showThatNamespaceAttributeIsSymetric() throws IOException {
        try (TestScript script = new TestScript()) {
            Item nippleClamps = script.item(Nipple_Clamps);
            nippleClamps.apply();
            State onNipples = script.state(OnNipples);

            assertTrue(onNipples.is(script.namespace));
            assertTrue(onNipples.is(Nipple_Clamps));
            assertTrue(nippleClamps.is(OnNipples));
            assertTrue(nippleClamps.is(script.namespace));

            nippleClamps.remove();

            assertFalse(onNipples.is(script.namespace));
            assertFalse(onNipples.is(Nipple_Clamps));
            assertFalse(script.state(Nipple_Clamps).is(OnNipples));
            assertFalse(script.state(Nipple_Clamps).is(script.namespace));

            assertFalse(nippleClamps.applied());
            assertFalse(onNipples.applied());
        }
    }

    @Test
    public void testVarietiesNone() throws IOException {
        try (TestScript script = new TestScript()) {
            var none = (ItemsImpl) script.items(new Enum<?>[] {}).inventory();
            assertTrue(none.varieties().isEmpty());
        }
    }

    @Test
    public void testVarietiesAll() throws IOException {
        try (TestScript script = new TestScript()) {
            script.addTestUserItems();
            script.addTestUserItems2();

            var inventory = (ItemsImpl) script.items(Collar, Bondage.Ankle_Restraints, Bondage.Wrist_Restraints, Chains)
                    .inventory();
            Varieties<Items.Set> all = inventory.varieties();
            assertEquals(4, all.size());
        }
    }

    @Test
    public void testVarietiesBestAtributes() throws IOException {
        try (TestScript script = new TestScript()) {
            script.addTestUserItems();
            script.addTestUserItems2();
            script.setAvailable(Toys.All, Household.All, Bondage.All);

            var inventory = script.items(Collar, Ankle_Restraints, Wrist_Restraints, Chains);
            var restraints = inventory.prefer(Features.Lockable, Material.Leather).getApplicableSet();
            assertEquals(4, restraints.size());

            Item collar = restraints.get(Collar);
            Item anklecuffs = restraints.get(Ankle_Restraints);
            Item wristCuffs = restraints.get(Wrist_Restraints);
            Item chains = restraints.get(Chains);

            assertNotEquals(Item.NotFound, collar);
            assertNotEquals(Item.NotFound, anklecuffs);
            assertNotEquals(Item.NotFound, wristCuffs);
            assertNotEquals(Item.NotFound, chains);
        }
    }

    @Test
    public void testVarietiesBestAppliedInSet() throws IOException {
        try (TestScript script = new TestScript()) {
            script.addTestUserItems();
            script.addTestUserItems2();
            script.setAvailable(Bondage.Wrist_Restraints, Bondage.Ankle_Restraints, Toys.Collar, Chains);
            var available = (ItemsImpl) script.items(Collar, Bondage.Ankle_Restraints, Bondage.Wrist_Restraints, Chains)
                    .getAvailable();
            testAnyWithAppliedItem(available, Material.Leather);
            testAnyWithAppliedItem(available, Material.Metal);
        }
    }

    private static void testAnyWithAppliedItem(ItemsImpl inventory, Material material) {
        Varieties<Items.Set> all = inventory.varieties();
        assertEquals(4, all.size());

        Items.Collection cuffsWithMaterial = inventory.matching(Bondage.Wrist_Restraints, material);
        assertFalse(cuffsWithMaterial.equals(Items.None));
        assertEquals(1, cuffsWithMaterial.size());

        cuffsWithMaterial.apply();
        Items.Set appliedCuffs = all.reduce(ItemsImpl::best);
        assertTrue(appliedCuffs.get(Bondage.Wrist_Restraints).is(material));
    }

    @Test
    public void testThatPreferIgnoresApplied() throws IOException {
        try (TestScript script = new TestScript()) {
            script.addTestUserItems();
            script.addTestUserItems2();
            script.setAvailable(Bondage.All, Toys.All);

            Items allMetal = script.items(Bondage.Wrist_Restraints, Toys.Humbler).prefer(Material.Metal).inventory();
            assertTrue(allMetal.items(Bondage.Wrist_Restraints).allAre(Material.Metal));
            assertTrue(allMetal.items(Toys.Humbler).allAre(Material.Metal));

            script.items(Bondage.Wrist_Restraints).matching(Material.Leather).getApplicableSet().apply();
            Items.Collection alreadyApplied = script.items(Bondage.Wrist_Restraints, Toys.Humbler)
                    .prefer(Material.Metal).inventory();

            assertFalse(alreadyApplied.items(Bondage.Wrist_Restraints).get().is(Material.Leather));
            assertFalse(alreadyApplied.items(Toys.Humbler).get().is(Material.Wood));
            assertTrue(alreadyApplied.items(Bondage.Wrist_Restraints).get().is(Material.Metal));
            assertTrue(alreadyApplied.items(Toys.Humbler).get().is(Material.Metal));
        }
    }

    @Test
    public void testItemsSubList() throws IOException {
        try (TestScript script = new TestScript()) {
            Item wristRestraints = script.item(Bondage.Wrist_Restraints);
            assertTrue(wristRestraints.is(Bondage.Wrist_Restraints));

            Item wristRestraints2 = script.items(Bondage.Wrist_Restraints).inventory().items(Bondage.Wrist_Restraints)
                    .get();
            assertNotEquals(Item.NotFound, wristRestraints2);
            assertTrue(wristRestraints2.is(Bondage.Wrist_Restraints));

            Item notFound = script.items(Bondage.Wrist_Restraints).inventory().items(Toys.Humbler).get();
            assertEquals(Item.NotFound, notFound);
        }
    }

    @Test
    public void testItemValues() throws IOException {
        try (TestScript script = new TestScript()) {
            script.addTestUserItems();

            Items restraints = script.items(Bondage.Wrist_Restraints, Bondage.Ankle_Restraints).inventory();
            assertEquals(4, restraints.size());

            var values = new ArrayList<>(((ItemsImpl) restraints).elementSet());
            assertEquals(2, values.size());
            assertEquals(QualifiedString.of(Bondage.Wrist_Restraints), values.get(0));
            assertEquals(QualifiedString.of(Bondage.Ankle_Restraints), values.get(1));
        }
    }

    @Test
    public void testAnyApplied() throws IOException {
        try (TestScript script = new TestScript()) {
            script.setAvailable(Toys.All);
            Items.Collection restraints = script.items(Bondage.Wrist_Restraints, Bondage.Ankle_Restraints)
                    .matching(Material.Metal).inventory();
            assertEquals(2, restraints.size());
            assertFalse(restraints.anyApplied());

            restraints.items(Bondage.Wrist_Restraints).get().apply();
            assertTrue(restraints.anyApplied());
            assertFalse(restraints.allApplied());

            restraints.items(Bondage.Ankle_Restraints).get().apply();
            assertTrue(restraints.anyApplied());
            assertTrue(restraints.allApplied());

            restraints.remove();
            assertFalse(restraints.anyApplied());
        }
    }

    @Test
    public void testAllApplied() throws IOException {
        try (TestScript script = new TestScript()) {
            script.setAvailable(Bondage.Wrist_Restraints, Bondage.Ankle_Restraints);
            Items.Set restraints = script.items(Bondage.Wrist_Restraints, Bondage.Ankle_Restraints).getApplicableSet();
            assertEquals(2, restraints.size());
            assertFalse(restraints.anyApplied());

            restraints.get(Bondage.Wrist_Restraints).apply();
            assertTrue(restraints.anyApplied());
            // assertFalse(restraints.allApplied());
            assertEquals(1, restraints.getApplied().size());

            assertTrue(script.items(Bondage.Wrist_Restraints, Bondage.Ankle_Restraints).anyApplied());
            // assertFalse(script.items(Bondage.Wrist_Restraints, Bondage.Ankle_Restraints).allApplied());

            restraints.get(Bondage.Ankle_Restraints).apply();
            assertTrue(restraints.anyApplied());
            // assertTrue(restraints.allApplied());
            assertEquals(2, restraints.getApplied().size());

            assertTrue(script.items(Bondage.Wrist_Restraints, Bondage.Ankle_Restraints).anyApplied());
            assertTrue(script.items(Bondage.Wrist_Restraints, Bondage.Ankle_Restraints).allApplied());

            restraints.remove();
            assertFalse(restraints.anyApplied());
        }
    }

    @Test
    public void testWhatItemHasBeenApplied() throws IOException {
        try (TestScript script = new TestScript()) {
            script.addTestUserItems();

            assertFalse(script.state(Bondage.Wrist_Restraints).applied());

            Item leatherCuffs = script.items(Bondage.Wrist_Restraints).matching(Material.Leather).inventory().get();
            Item handCuffs = script.items(Bondage.Wrist_Restraints).matching(Material.Metal).inventory().get();

            assertNotEquals(leatherCuffs, handCuffs);

            leatherCuffs.apply();

            assertTrue(script.state(Body.WristsTied).is(leatherCuffs));
            assertFalse(script.state(Body.WristsTied).is(handCuffs));

            assertTrue(leatherCuffs.applied());
            assertFalse(handCuffs.applied());
        }
    }

    @Test
    public void testAvailableItems() throws IOException {
        try (TestScript script = new TestScript()) {
            script.addTestUserItems();

            var restraints = script.items(Bondage.Wrist_Restraints).inventory();
            assertEquals(2, restraints.size());

            assertEquals(0, restraints.getAvailable().size());
            restraints.get().setAvailable(true);
            assertEquals("Only one available item expected", 1, restraints.getAvailable().size());
            assertTrue(restraints.anyAvailable());
            assertFalse("Only one available item expected", restraints.allAvailable());

            assertTrue(script.items(Bondage.Wrist_Restraints).anyAvailable());
            assertTrue("For queries one or more available item per kind is expected",
                    script.items(Bondage.Wrist_Restraints).allAvailable());
        }
    }

    @Test
    public void testAppliedItems() throws IOException {
        try (TestScript script = new TestScript()) {
            script.addTestUserItems();

            Items.Collection restraints = script.items(Bondage.Wrist_Restraints).inventory();
            assertEquals(2, restraints.size());

            script.item(Bondage.Wrist_Restraints).apply();

            Items.Collection applied = script.items(Bondage.Wrist_Restraints).getApplied();
            assertEquals(1, applied.size());
            assertTrue(applied.get().is(restraints.get()));
            assertEquals(restraints.get(), applied.get());
        }
    }

    @Test
    public void testExplicitelyAppliedItems() throws IOException {
        try (TestScript script = new TestScript()) {
            script.addTestUserItems();

            Items.Collection restraints = script.items(Bondage.Wrist_Restraints).inventory();
            assertEquals(2, restraints.size());

            restraints.get().apply();

            Items applied = script.items(Bondage.Wrist_Restraints).getApplied();
            assertEquals(1, applied.size());
        }
    }

    // TODO test that query doesn't work with namespace and show how to do it right

    @Test
    public void testMatchingItemInstanceAttributesDontInterferWithApplied() throws IOException {
        try (TestScript script = new TestScript()) {
            script.debugger.freezeTime();

            Items.Collection gags1 = script.items(Toys.Gag).inventory();
            Item ringGag = gags1.matching(Toys.Gags.Ring_Gag).get();
            assertTrue(ringGag.is(Toys.Gags.Ring_Gag));
            ringGag.apply();
            assertTrue(ringGag.applied());
            assertTrue(script.state(Body.InMouth).is(ringGag));
            // assertTrue(script.state(Toys.Gag).is(ringGag)); // ???

            Items.Collection gags2 = script.items(Toys.Gag).inventory();
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

            Items.Collection gags3 = script.items(Toys.Gag).inventory();
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
    }

    @Test
    public void testMatchingDifferentItemsOfAKindDontInterferShort() throws IOException {
        try (TestScript script = new TestScript()) {
            script.debugger.freezeTime();

            Items.Collection gags = script.items(Toys.Gag).inventory();
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
    }

    @Test
    public void documentThatApplyingStateDoesntApplyItems() throws IOException {
        try (TestScript script = new TestScript()) {
            script.item(Household.Clothes_Pegs).to(Body.OnNipples).apply();

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
    }

    @Test
    public void testApplyToRemoveFrom() throws IOException {
        try (TestScript script = new TestScript()) {
            State pegs = script.state(Household.Clothes_Pegs);
            State nips = script.state(Body.OnNipples);

            script.item(Household.Clothes_Pegs).to(Body.OnNipples).apply();
            assertTrue(nips.applied());
            assertTrue(nips.is(Household.Clothes_Pegs));
            assertTrue(pegs.applied());
            assertTrue(pegs.is(Body.OnNipples));

            script.item(Household.Clothes_Pegs).removeFrom(Body.OnNipples);
            assertFalse(nips.applied());
            assertFalse(nips.is(Household.Clothes_Pegs));
            assertFalse(pegs.applied());
            assertFalse(pegs.is(Body.OnNipples));
        }
    }

    @Test
    public void testThatApplyingItemsIsCompatibleToApplyingSingleItem() throws IOException {
        try (TestScript script = new TestScript()) {
            script.addTestUserItems();

            script.setAvailable(Bondage.Wrist_Restraints);
            Items restraints = script.items(Bondage.Wrist_Restraints).getAvailable();
            assertEquals(2, restraints.size());

            restraints.apply();
            Items applied = script.items(Bondage.Wrist_Restraints).getApplied();
            assertEquals(1, applied.size());
        }
    }

    @Test
    public void testRemovingMultipleItems() throws IOException {
        try (TestScript script = new TestScript()) {
            script.addTestUserItems();

            script.setAvailable(Toys.All);
            var ballTorture = script.items(Toys.Ball_Stretcher, Toys.Humbler).getApplicableSet();
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
    }

    @Test
    public void testRemoveSingleItem() throws IOException {
        try (TestScript script = new TestScript()) {
            script.addTestUserItems();

            Items.Collection gags = script.items(Toys.Gag).inventory();
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
    }

    @Test
    public void testRemoveSingleItemImplicit() throws IOException {
        try (TestScript script = new TestScript()) {
            script.addTestUserItems();

            Items.Collection gags = script.items(Toys.Gag).inventory();
            assertEquals(5, gags.size());

            Iterator<Item> gag = gags.iterator();
            Item gag0 = gag.next();
            Item gag1 = gag.next();
            State inMouth = script.state(Body.InMouth);

            gag0.to(Body.InMouth).apply();
            assertTrue(gag0.applied());
            assertFalse(gag1.applied());

            gag1.to(Body.InMouth).apply();
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
    }

    @Test
    public void testQueryItemDefaultPeers() throws IOException {
        try (TestScript script = new TestScript()) {
            script.addTestUserItems();

            Item gag = script.items(Toys.Gag).matching(Toys.Gags.Ball_Gag).inventory().get();
            assertFalse(gag.applied());

            // Items are different from states in that they have attributes and default peers:
            // Attributes always apply, but default peers only if applied
            // The reason for handling attributes and default peers different is:
            // you can say "if toy is rubber then ...", it's always true, applied or not
            // but when you say "if toy is Body.InMouth then ..." the statement is only true if applied
            // if it's necessary to query default peers make them public via the Item interface
            assertFalse(gag.is(Body.CantLick));
            assertFalse(script.state(Body.InMouth).is(gag));

            gag.apply();
            assertTrue(gag.is(Body.CantLick));
            assertTrue(script.state(Body.InMouth).is(gag));
        }
    }

    @Test
    public void testRemoveFromSingleItemExplicit() throws IOException {
        try (TestScript script = new TestScript()) {
            script.addTestUserItems();

            Items.Collection gags = script.items(Toys.Gag).inventory();
            assertEquals(5, gags.size());

            Iterator<Item> gag = gags.iterator();
            Item gag0 = gag.next();
            assertTrue(gag0.is(Toys.Gags.Ball_Gag));
            Item gag1 = gag.next();
            assertTrue(gag1.is(Toys.Gags.Bit_Gag));

            State inMouth = script.state(Body.InMouth);
            State cantLick = script.state(Body.CantLick);

            gag0.to(Body.InMouth).apply();
            assertTrue(gag0.applied());
            assertFalse(gag1.applied());
            assertTrue(gag0.is(Body.InMouth));
            assertTrue("Default items must be applied", gag0.is(Body.CantLick));

            gag1.to(Body.InMouth).apply();
            assertTrue(gag0.applied());
            assertTrue(gag1.applied());

            // TODO decide whether to apply default peers on applyTo()
            // pro apply defaults: same behavior
            // con apply defaults: are there any multi-use toys that actually have default peers
            // con apply defaults: when using applyTo() management should be completely manual

            // TODO instead of explicit list remove gag0.defaultPeers()
            gag0.removeFrom(Body.InMouth, Body.CantLick);
            assertFalse(gag0.applied());
            assertFalse(gag0.is(Body.InMouth));
            assertFalse(gag0.is(Body.CantLick));
            assertFalse(inMouth.is(gag0));
            assertFalse(cantLick.is(gag0));

            assertTrue(gag1.applied());
            assertTrue(gag1.is(Body.InMouth));
            assertTrue(gag1.is(Body.CantLick));
            assertTrue(inMouth.is(gag1));
            assertTrue(cantLick.is(gag1));

            assertEquals(1, gags.getApplied().size());
            assertTrue(inMouth.applied());
            assertTrue(cantLick.applied());

            // TODO instead of explicit list remove gag1.defaultPeers()
            gag1.removeFrom(Body.InMouth, Body.CantLick);

            assertFalse(gag1.applied());
            assertFalse(gag0.is(Body.InMouth));
            assertFalse(gag0.is(Body.CantLick));

            assertFalse(gag1.applied());
            assertFalse(gag1.is(Body.InMouth));
            assertFalse(gag1.is(Body.CantLick));
            assertFalse(inMouth.is(gag1));
            assertFalse(cantLick.is(gag1));

            assertEquals(0, gags.getApplied().size());
            assertFalse(inMouth.applied());
            assertFalse(cantLick.applied());
        }
    }

    @Test
    public void testThatRemovingItemsIsCompatibleToRemovingSingleItem() throws IOException {
        try (TestScript script = new TestScript()) {
            script.addTestUserItems();

            Items.Collection restraints = script.items(Bondage.Wrist_Restraints).inventory();
            assertEquals(2, restraints.size());

            restraints.stream().forEach(Item::apply);
            assertEquals(2, restraints.getApplied().size());

            restraints.remove();
            assertEquals(1, restraints.getApplied().size());
        }
    }

    @Test
    public void testThatRemovingPartiallyAppliedItemsRequiresGetApplied() throws IOException {
        try (TestScript script = new TestScript()) {
            script.addTestUserItems();

            Items.Collection restraints = script.items(Bondage.Wrist_Restraints, Bondage.Ankle_Restraints).inventory();
            assertEquals(4, restraints.size());

            script.item(Bondage.Wrist_Restraints).setAvailable(true);
            restraints.items(Bondage.Wrist_Restraints).get().apply();
            assertEquals(1, restraints.getApplied().size());
            assertThrows("Only applied items can be removed", IllegalStateException.class, () -> restraints.remove());
            restraints.getApplied().remove();
            assertEquals(0, restraints.getApplied().size());
        }
    }

    @Test
    public void testItemNotFound() throws IOException {
        try (TestScript script = new TestScript()) {
            assertFalse(Item.NotFound.canApply());
            assertFalse(Item.NotFound.applied());
            assertTrue(Item.NotFound.removed());
            assertThrows(UnsupportedOperationException.class, () -> Item.NotFound.apply());
            assertThrows(IllegalStateException.class, () -> Item.NotFound.remove());

            var itemNotFound = script.items(Item.NotFound);
            assertFalse(itemNotFound.allApplicable());
            assertFalse(itemNotFound.anyApplied());
            assertTrue(itemNotFound.noneApplied());
            assertThrows(UnsupportedOperationException.class, () -> itemNotFound.apply());
            assertThrows(IllegalStateException.class, () -> itemNotFound.remove());
        }
    }

    @Test
    public void testOrElseSingleKind() throws IOException {
        try (TestScript script = new TestScript()) {
            script.addTestUserItems();

            script.setAvailable(Clothes.Dress);
            var dresses = script.items(Clothes.Skirt, Clothes.Blouse).orElse(script.items(Clothes.Dress));
            assertTrue(dresses.anyApplicable());
            assertEquals(Item.NotFound, dresses.getApplicableSet().get(Clothes.Skirt));
            assertTrue(dresses.getApplicableSet().get(Clothes.Dress).canApply());
        }
    }

    @Test
    public void testOrElseMultipleKinds() throws IOException {
        try (TestScript script = new TestScript()) {
            script.addTestUserItems();

            script.setAvailable(Clothes.Blouse, Clothes.Dress);
            var dresses = script.items(Clothes.Skirt, Clothes.Blouse).orElse(script.items(Clothes.Dress));
            assertTrue(dresses.anyApplicable());
            assertEquals(Item.NotFound, dresses.getApplicableSet().get(Clothes.Skirt));
            assertEquals(Item.NotFound, dresses.getApplicableSet().get(Clothes.Blouse));
            assertTrue(dresses.getApplicableSet().get(Clothes.Dress).canApply());
        }
    }

    @Test
    public void testOrElseQuery() throws IOException {
        try (TestScript script = new TestScript()) {
            script.addTestUserItems();
            script.setAvailable(Clothes.Garter_Belt, Clothes.Stockings);
            script.setAvailable(Clothes.Blouse, Clothes.Catsuit, Clothes.Dress);

            Items.Query lingerie = script.items(Clothes.Lingerie).matching(Material.Rubber)
                    .orElse(script.items(Clothes.Lingerie));
            assertTrue(lingerie.anyAvailable());
            assertEquals(2, lingerie.getAvailable().size());
            assertFalse(lingerie.getAvailable().anyAre(Material.Rubber));

            Items.Query costume = script.items(Clothes.Female).matching(Material.Rubber)
                    .orElse(script.items(Clothes.Female));
            assertTrue(costume.anyAvailable());
            assertEquals(3, costume.getAvailable().size());
            assertFalse(costume.getAvailable().allAre(Material.Rubber));

            Items.Query rubber = script.items(Clothes.Catsuit).matching(Material.Rubber)
                    .orElse(script.items(Clothes.Blouse, Clothes.Dress));
            assertTrue(rubber.allAvailable());
            assertEquals(1, rubber.getAvailable().size());
            assertTrue(rubber.getAvailable().allAre(Material.Rubber));

            Items.Query attire = script.items(Clothes.Catsuit, Clothes.Dress).prefer(Material.Rubber)
                    .orElse(script.items(Clothes.Garter_Belt, Clothes.Stockings));
            assertTrue(attire.allAvailable());
            assertEquals(2, attire.getAvailable().size());
            assertTrue(attire.getAvailable().anyAre(Material.Rubber));
            assertFalse(attire.getAvailable().allAre(Material.Rubber));
        }
    }

    @Test
    public void testItemsOrElse() throws IOException {
        try (TestScript script = new TestScript()) {
            script.addTestUserItems();
            script.setAvailable(Clothes.Garter_Belt, Clothes.Stockings);
            script.setAvailable(Clothes.Blouse, Clothes.Catsuit, Clothes.Dress);

            Items lingerie = script.items(Clothes.Lingerie).inventory().matching(Material.Rubber)
                    .orElse(() -> script.items(Clothes.Lingerie).getAvailable());
            assertTrue(lingerie.anyAvailable());
            assertEquals(2, lingerie.getAvailable().size());
            assertFalse(lingerie.getAvailable().anyAre(Material.Rubber));

            Items costume = script.items(Clothes.Female).inventory().matching(Material.Rubber)
                    .orElse(() -> script.items(Clothes.Female).getAvailable());
            assertTrue(costume.anyAvailable());
            assertEquals(3, costume.getAvailable().size());
            assertFalse(costume.getAvailable().allAre(Material.Rubber));

            Items rubber = script.items(Clothes.Catsuit).inventory().matching(Material.Rubber)
                    .orElse(() -> script.items(Clothes.Blouse, Clothes.Dress).getAvailable());
            assertTrue(rubber.allAvailable());
            assertEquals(1, rubber.getAvailable().size());
            assertTrue(rubber.getAvailable().allAre(Material.Rubber));

            Items attire = script.items(Clothes.Catsuit, Clothes.Dress).inventory().prefer(Material.Rubber)
                    .orElse(() -> script.items(Clothes.Garter_Belt, Clothes.Stockings).getAvailable());
            assertTrue(attire.allAvailable());
            assertEquals(2, attire.getAvailable().size());
            assertTrue(attire.getAvailable().anyAre(Material.Rubber));
            assertFalse(attire.getAvailable().allAre(Material.Rubber));
        }
    }

    @Test
    public void testItemsEmptyOrElseItems() throws IOException {
        try (TestScript script = new TestScript()) {
            script.addTestUserItems();
            script.setAvailable(Clothes.Catsuit, Clothes.Stockings);

            var empty = script.items(Clothes.Catsuit /* , Clothes.Stockings */).matching(Material.Wood);
            var none = empty.inventory();
            assertEquals(0, none.size());

            var catsuits = empty.orElse(script.items(Clothes.Catsuit).matching(Material.Rubber)).inventory();
            assertEquals(1, catsuits.size());

            var stockings = empty.orElse(script.items(Clothes.Stockings)).inventory();
            assertEquals(1, stockings.size());
        }
    }

    @Test
    public void testItemsOrElseItemsEnum() throws IOException {
        try (TestScript script = new TestScript()) {
            script.addTestUserItems();

            Items restraints1 = script.items(Bondage.Wrist_Restraints).inventory();
            assertEquals(2, restraints1.size());

            Items restraints2 = script.items(Bondage.Wrist_Restraints).inventory()
                    .orElse(() -> script.items(Bondage.Wrist_Restraints).inventory());
            assertEquals(restraints1, restraints2);
        }
    }

    @Test
    public void testItemsOrElseItemsString() throws IOException {
        try (TestScript script = new TestScript()) {
            script.addTestUserItems();

            Items restraints1 = script.items("teaselib.Bondage.Wrist_Restraints").inventory();
            assertEquals(2, restraints1.size());

            Items restraints2 = script.items("teaselib.Bondage.Wrist_Restraints").inventory()
                    .orElse(() -> script.items("teaselib.Bondage.Wrist_Restraints").inventory());
            assertEquals(restraints1, restraints2);
        }
    }

    @Test
    public void testOrElsePreferEnum() throws IOException {
        try (TestScript script = new TestScript()) {
            script.addTestUserItems();
            script.addTestUserItems2();
            script.items(Toys.Humbler).matching(Material.Metal).inventory().get().setAvailable(true);
            Items humbler = script.items(Toys.Humbler).matching(Material.Wood).orElsePrefer(Material.Metal).inventory();
            assertTrue(humbler.allAre(Material.Metal));
        }
    }

    @Test
    public void testOrElsePreferString() throws IOException {
        try (TestScript script = new TestScript()) {
            script.addTestUserItems();
            script.addTestUserItems2();
            script.items(Toys.Humbler).matching(Material.Metal).inventory().get().setAvailable(true);
            Items restraints = script.items("teaselib.Toys.Humbler").matching("teaselib.Material.Wood").orElsePrefer("teaselib.Material.Metal").inventory();
            assertTrue(restraints.allAre("teaselib.Material.Metal"));
        }
    }

    @Test
    public void testOrElseMatchingEnum() throws IOException {
        try (TestScript script = new TestScript()) {
            script.addTestUserItems();
            script.items(Bondage.Wrist_Restraints).matching(Material.Metal).inventory().get().setAvailable(true);

            Items restraints = script.items(Bondage.Wrist_Restraints).matching(Material.Wood)
                    .orElseMatching(Material.Metal).inventory();
            assertTrue(restraints.allAre(Material.Metal));
        }
    }

    @Test
    public void testOrElseMatchingString() throws IOException {
        try (TestScript script = new TestScript()) {
            script.addTestUserItems();
            script.items(Bondage.Wrist_Restraints).matching(Material.Metal).inventory().get().setAvailable(true);

            Items restraints = script.items("teaselib.Bondage.Wrist_Restraints").matching("teaselib.Material.Wood")
                    .orElseMatching("teaselib.Material.Metal").inventory();
            assertTrue(restraints.allAre("teaselib.Material.Metal"));
        }
    }

    @Test
    public void testQueryWithoutEnum() throws IOException {
        try (TestScript script = new TestScript()) {
            Items.Collection all = script.items(Bondage.Wrist_Restraints, Bondage.Ankle_Restraints).inventory();
            assertEquals(4, all.size());
            assertEquals(2, all.without(Material.Metal).size());

            Items.Collection without = all.without(Bondage.Ankle_Restraints);
            assertEquals(2, without.size());
            assertEquals(Item.NotFound, without.items(Bondage.Ankle_Restraints).get());

            assertTrue(without.orElse(script.items(Bondage.Ankle_Restraints, Bondage.Wrist_Restraints))
                    .contains(Bondage.Wrist_Restraints));
            assertTrue(without.orElse(script.items(Bondage.Ankle_Restraints)).contains(Bondage.Ankle_Restraints));

            script.setAvailable(Bondage.Wrist_Restraints, Bondage.Ankle_Restraints);
            assertTrue(without.orElse(script.items(Bondage.Ankle_Restraints, Bondage.Wrist_Restraints))
                    .contains(Bondage.Wrist_Restraints));
            assertFalse(without.orElse(script.items(Bondage.Ankle_Restraints)).contains(Bondage.Ankle_Restraints));
        }
    }

    @Test
    public void testQueryWithoutString() throws IOException {
        try (TestScript script = new TestScript()) {
            Items.Collection all = script
                    .items("teaselib.Bondage.Wrist_Restraints", "teaselib.Bondage.Ankle_Restraints").inventory();
            assertEquals(4, all.size());
            assertEquals(2, all.without(Material.Metal).size());

            Items.Collection without = all.without("teaselib.Bondage.Ankle_Restraints");
            assertEquals(2, without.size());
            assertEquals(Item.NotFound, without.items("teaselib.Bondage.Ankle_Restraints").get());

            assertTrue(without.orElse(script.items("teaselib.Bondage.Ankle_Restraints", "teaselib.Bondage.Wrist_Restraints"))
                    .contains("teaselib.Bondage.Wrist_Restraints"));

            assertTrue(without.orElse(script.items("teaselib.Bondage.Ankle_Restraints"))
                    .contains("teaselib.Bondage.Ankle_Restraints"));

            script.setAvailable(Bondage.Wrist_Restraints, Bondage.Ankle_Restraints);
            assertTrue(without.orElse(script.items("teaselib.Bondage.Ankle_Restraints", "teaselib.Bondage.Wrist_Restraints"))
                    .contains("teaselib.Bondage.Wrist_Restraints"));
            assertFalse(without.orElse(script.items("teaselib.Bondage.Ankle_Restraints"))
                    .contains("teaselib.Bondage.Ankle_Restraints"));
        }
    }

    @Test
    public void testItemsWithoutEnum() throws IOException {
        try (TestScript script = new TestScript()) {
            Items.Collection all = script.items(Bondage.Wrist_Restraints, Bondage.Ankle_Restraints).inventory();
            assertEquals(4, all.size());
            assertEquals(2, all.without(Material.Metal).size());

            Items without = all.without(Bondage.Ankle_Restraints);
            assertEquals(2, without.size());
            assertTrue(without.items(Bondage.Ankle_Restraints).isEmpty());

            assertTrue(without.orElse(script.items(Bondage.Ankle_Restraints, Bondage.Wrist_Restraints))
                    .contains(Bondage.Wrist_Restraints));
            assertTrue(without.orElse(script.items(Bondage.Ankle_Restraints)).contains(Bondage.Ankle_Restraints));

            script.setAvailable(Bondage.Wrist_Restraints, Bondage.Ankle_Restraints);
            assertTrue(without.orElse(script.items(Bondage.Ankle_Restraints, Bondage.Wrist_Restraints))
                    .contains(Bondage.Wrist_Restraints));
            assertFalse(without.orElse(script.items(Bondage.Ankle_Restraints)).contains(Bondage.Ankle_Restraints));
        }
    }

    @Test
    public void testItemsWithoutString() throws IOException {
        try (TestScript script = new TestScript()) {
            Items all = script
                    .items("teaselib.Bondage.Wrist_Restraints", "teaselib.Bondage.Ankle_Restraints").inventory();
            assertEquals(4, all.size());
            assertEquals(2, all.without(Material.Metal).size());

            Items without = all.without("teaselib.Bondage.Ankle_Restraints");
            assertEquals(2, without.size());
            assertTrue(without.items("teaselib.Bondage.Ankle_Restraints").isEmpty());

            assertTrue(without.orElse(script.items("teaselib.Bondage.Ankle_Restraints", "teaselib.Bondage.Wrist_Restraints"))
                    .contains("teaselib.Bondage.Wrist_Restraints"));
            assertTrue(without.orElse(script.items("teaselib.Bondage.Ankle_Restraints"))
                    .contains("teaselib.Bondage.Ankle_Restraints"));

            script.setAvailable(Bondage.Wrist_Restraints, Bondage.Ankle_Restraints);
            assertTrue(without.orElse(script.items("teaselib.Bondage.Ankle_Restraints", "teaselib.Bondage.Wrist_Restraints"))
                    .contains("teaselib.Bondage.Wrist_Restraints"));
            assertFalse(without.orElse(script.items("teaselib.Bondage.Ankle_Restraints"))
                    .contains("teaselib.Bondage.Ankle_Restraints"));
        }
    }

    @Test
    public void testItemAppliedToItems() throws IOException {
        assertTrue(Collections.emptySet().stream().allMatch(x -> true));

        try (TestScript script = new TestScript()) {
            script.setAvailable(Toys.All, Bondage.All, Accessoires.All);
            Items.Set restraints = script.items(Bondage.Wrist_Restraints, Bondage.Ankle_Restraints, Toys.Collar)
                    .getApplicableSet();
            restraints.apply();
            Items.Set chains = script.items(Bondage.Chains, Accessoires.Bells).getApplicableSet();
            chains.forEach(item -> item.applyTo(restraints));

            Item bells = chains.get(Accessoires.Bells);
            assertTrue(bells.applied());

            Item wristRestraints = restraints.get(Bondage.Wrist_Restraints);
            State wristRestraintsState = script.state(Bondage.Wrist_Restraints);
            assertTrue(bells.is(wristRestraints));
            assertTrue(bells.is(Bondage.Wrist_Restraints));

            State bellsState = script.state(Accessoires.Bells);
            assertTrue(bellsState.is(wristRestraintsState));
            assertTrue(wristRestraintsState.is(bellsState));

            assertTrue(bells.is(wristRestraints));
            assertTrue(wristRestraints.is(bells));
            assertTrue(bells.is(restraints));

            Item singleChainItem = chains.get(Bondage.Chains);
            assertFalse(bells.is(singleChainItem));
            assertFalse(singleChainItem.is(bells));

            assertFalse(bells.is(chains.get(0)));
            assertTrue(bells.is(chains.get(1)));

            // chains contains bells as an item
            // false since the bells cannot be (Accessories.Bells AND Bondage.Chains)
            assertFalse(bells.is(chains));
            assertTrue(chains.anyAre(bells));
            assertFalse(chains.allAre(bells));

            singleChainItem.remove();
            assertTrue(bells.applied());
            assertTrue(bells.is(Bondage.Wrist_Restraints));
            assertTrue(bells.is(wristRestraints));
            assertTrue(bells.is(restraints));
        }
    }

    @Test
    public void testItemNotAppliedToFreeItems() throws IOException {
        try (TestScript script = new TestScript()) {
            script.setAvailable(Bondage.Wrist_Restraints, Bondage.Ankle_Restraints, Toys.Collar);
            Items.Collection restraints = script.items(Bondage.Wrist_Restraints, Bondage.Ankle_Restraints, Toys.Collar)
                    .getAvailable();
            restraints.apply();
            script.setAvailable(Bondage.All, Accessoires.All);
            Items.Set chains = script.items(Bondage.Chains, Accessoires.Bells).getApplicableSet();
            chains.forEach(item -> item.applyTo(restraints));

            Item bells = chains.get(Accessoires.Bells);
            Item handcuffs = restraints.getFree().items(Bondage.Wrist_Restraints).get();
            assertNotEquals(Item.NotFound, handcuffs);

            assertFalse(handcuffs.is(bells));
            assertFalse(handcuffs.is(chains));

            assertFalse(bells.is(handcuffs));
            assertFalse(chains.anyAre(handcuffs));
        }
    }

    @Test
    public void testDomain() throws IOException {
        try (TestScript script = new TestScript()) {
            Domain domain = script.domain(Gadgets.Key_Release);

            Items.Collection items = script.items(Bondage.Wrist_Restraints).inventory();
            Items handled = domain.related(items);

            assertEquals(items.size(), handled.size());
            assertEquals(script.defaultDomain.toString(), AbstractProxy.itemImpl(items.get()).domain);
            handled.stream().allMatch(x -> domain.toString().equals(AbstractProxy.itemImpl(x).domain));
        }
    }

    @Test
    public void testItemsStatement() throws IOException {
        try (TestScript script = new TestScript()) {
            var myOutfit = script.items(Toys.Collar, Shoes.High_Heels).inventory();
            assertEquals(2, myOutfit.size());

            Items allShoes = script.items(Shoes.All).inventory();
            assertEquals(5, allShoes.size());

            // TODO using Statement.values isn't a query -> change to (package-) private field
            Items myShoes = myOutfit.items(Shoes.All.values);
            assertEquals(1, myShoes.size());

            Items myShoes2 = myOutfit.items(Shoes.All);
            assertEquals(1, myShoes2.size());
        }
    }

    @Test
    public void testGetItemsApplied() throws IOException {
        try (TestScript script = new TestScript()) {
            script.setAvailable(Toys.All, Shoes.All);
            Items.Set myOutfit = script.items(Toys.Collar, Shoes.High_Heels).getApplicableSet();
            assertEquals(2, myOutfit.size());

            myOutfit.apply();
            assertEquals(2, myOutfit.getApplied().size());
            assertEquals(2, script.items(Toys.All, Shoes.All).getApplied().size());
        }
    }

    @Test
    public void testGetItemsAppliedToPeers() throws IOException {
        try (TestScript script = new TestScript()) {
            script.setAvailable(Toys.All);
            Items anal = script.items(Toys.Enema_Kit, Toys.Buttplug).getApplicableSet();
            anal.forEach(Item::apply);

            Items inButt = script.items(Toys.All).matching(Body.InButt).inventory();
            assertEquals(2, inButt.size());
            assertTrue(inButt.anyAre(Toys.Buttplug));
            assertTrue(inButt.anyAre(Toys.Enema_Kit));
        }
    }

    @Test
    public void testRemovedDuration() throws IOException {
        try (TestScript script = new TestScript()) {
            var statement = Select.items(Toys.Dildo, Toys.Buttplug);
            Items.Collection plugs = script.items(statement).inventory();
            Item buttplug = plugs.items(Toys.Buttplug).get();
            buttplug.apply();
            script.debugger.advanceTime(30, TimeUnit.MINUTES);
            buttplug.remove();
            assertEquals(0, script.items(statement).inventory().removed(TimeUnit.HOURS));

            script.debugger.advanceTime(30, TimeUnit.MINUTES);
            assertEquals(30, script.items(statement).inventory().removed(TimeUnit.MINUTES));
            assertEquals(0, script.items(statement).inventory().removed(TimeUnit.HOURS));

            script.debugger.advanceTime(1, TimeUnit.HOURS);
            assertEquals(1, script.items(statement).inventory().removed(TimeUnit.HOURS));

            script.debugger.advanceTime(1, TimeUnit.HOURS);
            assertEquals(2, script.items(statement).inventory().removed(TimeUnit.HOURS));

            Item dildo = plugs.items(Toys.Dildo).get();
            dildo.apply();
            script.debugger.advanceTime(30, TimeUnit.MINUTES);
            dildo.remove();

            script.debugger.advanceTime(3, TimeUnit.HOURS);
            assertEquals(3, script.items(statement).inventory().removed(TimeUnit.HOURS));
        }
    }

    @Test
    public void testItemProxyDecoration() throws IOException {
        try (TestScript script = new TestScript()) {
            script.setAvailable(Bondage.All);
            var items = script.items(Bondage.Ankle_Restraints, Bondage.Wrist_Restraints);
            testItemProxies(script, items);
        }
    }

    @Test
    public void testItemProxyDecorationQuery() throws IOException {
        try (TestScript script = new TestScript()) {
            script.setAvailable(Bondage.All);
            var items = script.items(Bondage.Ankle_Restraints, Bondage.Wrist_Restraints).without(Features.Detachable);
            testItemProxies(script, items);
        }
    }

    private static void testItemProxies(TestScript script, Query items) {
        assertDecorated(items.getApplicableSet());
        assertDecorated(items.getAvailable());
        assertDecorated(items.inventory());
        items.inventory().apply();
        assertDecorated(items.getApplied());
        assertDecorated(script.items(items.inventory().get()));
        items.getApplied().remove();
    }

    private static void assertDecorated(Items items) {
        assertFalse("Items expected but was empty", items.isEmpty());
        String expected = "ItemProxy instances expected";
        assertTrue(expected, items.stream().allMatch(ItemProxy.class::isInstance));
    }

    @Test
    public void testItemAsOtherItems() throws Exception {
        try (TestScript script = new TestScript()) {
            script.setAvailable(Toys.Dildo, Toys.Buttplug);

            Item buttPlug = script.items(Toys.Buttplug).getApplicable().get();
            Item dildo = script.items(Toys.Dildo).getApplicable().get();
            Item analDildo = script.items(Toys.Dildo).getApplicable().to(Body.InButt).get();

            assertTrue(buttPlug.canApply());
            assertTrue(dildo.canApply());
            assertTrue(analDildo.canApply());

            buttPlug.apply();
            assertFalse(buttPlug.canApply());
            assertTrue(dildo.canApply());
            assertFalse(analDildo.canApply());

            buttPlug.remove();
            assertTrue(buttPlug.canApply());
            assertTrue(dildo.canApply());
            assertTrue(analDildo.canApply());

            analDildo.apply();
            assertFalse(buttPlug.canApply());
            assertFalse(dildo.canApply()); // already applied as similar item
            assertFalse(analDildo.canApply());

            analDildo.remove();
            assertTrue(buttPlug.canApply());
            assertTrue(dildo.canApply());
            assertTrue(analDildo.canApply());
        }
    }

}
