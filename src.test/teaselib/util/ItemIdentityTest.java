/**
 *
 */
package teaselib.util;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import teaselib.Body;
import teaselib.Household;
import teaselib.State;
import teaselib.State.Persistence.Until;
import teaselib.Toys;
import teaselib.Toys.Gags;
import teaselib.core.ItemImpl;
import teaselib.core.TeaseLib;
import teaselib.core.state.ItemProxy;
import teaselib.core.util.*;
import teaselib.test.TestScript;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Citizen-Cane
 *
 */
public class ItemIdentityTest {
    @Test
    public void testRetrievingTheIdenticalItem() throws IOException {
        try (TestScript script = new TestScript()) {
            Items.Collection gags = script.items(Toys.Gag).inventory();

            Item ringGag = gags.matching(Toys.Gags.Ring_Gag).get();
            Assertions.assertTrue(ringGag.is(Gags.Ring_Gag));

            Item sameRingGag = script.items(Toys.Gag).matching(Toys.Gags.Ring_Gag).item();
            assertEquals(ringGag, sameRingGag);

            Assertions.assertTrue(ringGag.is(Gags.Ring_Gag));
            Assertions.assertTrue(sameRingGag.is(Gags.Ring_Gag));
        }
    }

    @Test
    public void testApplyingTheIdenticalItem() throws IOException {
        try (TestScript script = new TestScript()) {
            var gags = script.items(Toys.Gag);

            Item ringGag = gags.matching(Toys.Gags.Ring_Gag).item();
            assertFalse(ringGag.is(Body.InMouth));

            ringGag.apply();
            Assertions.assertTrue(ringGag.is(Body.InMouth));
            Assertions.assertTrue(script.state(Body.InMouth).is(Toys.Gag));
            Assertions.assertTrue(script.state(Body.InMouth).is(Gags.Ring_Gag));

            State mouth = script.state(Body.InMouth);
            Assertions.assertTrue(mouth.is(Toys.Gag));
            Assertions.assertTrue(mouth.is(Gags.Ring_Gag));
            Assertions.assertTrue(mouth.is(ringGag));
        }
    }

    @Test
    public void testComparingItemsAndStateWorks() throws IOException {
        try (TestScript script = new TestScript()) {
            var gags = script.items(Toys.Gag);

            Item ringGag = gags.matching(Toys.Gags.Ring_Gag).item();
            assertFalse(ringGag.is(Body.InMouth));
            ringGag.apply();

            Assertions.assertTrue(ringGag.is(Body.InMouth));
            Assertions.assertTrue(ringGag.is(script.state(Body.InMouth)));
        }
    }

    @Test
    public void testItemInstanceRemoveSameInstance() throws IOException {
        try (TestScript script = new TestScript()) {
            Item chastityDevice = script.item(Toys.Chastity_Device);
            State onPenis = script.state(Body.OnPenis);

            chastityDevice.apply();

            Assertions.assertTrue(chastityDevice.applied());
            Assertions.assertTrue(onPenis.applied());

            chastityDevice.remove();

            assertFalse(chastityDevice.applied());
            assertFalse(onPenis.applied());
        }
    }

    @Test
    public void testItemInstanceRemoveAnyInstance() throws IOException {
        try (TestScript script = new TestScript()) {
            Item chastityDevice = script.items(Toys.Chastity_Device).matching(Toys.Chastity_Devices.Gates_of_Hell)
                    .item();
            State onPenis = script.state(Body.OnPenis);

            chastityDevice.apply();

            Assertions.assertTrue(chastityDevice.applied());
            Assertions.assertTrue(onPenis.applied());

            Item otherChastityDevice = script.items(Toys.Chastity_Device).matching(Toys.Chastity_Devices.Belt).item();

            // Removing the wrong item doesn't work
            try {
                otherChastityDevice.remove();
                fail("Removing un-applied item must throw");
            } catch (IllegalStateException e) {
                Assertions.assertTrue(chastityDevice.applied());
                Assertions.assertTrue(onPenis.applied());
            }

            Assertions.assertTrue(chastityDevice.applied());
            Assertions.assertTrue(onPenis.applied());

            // Instead just remove the default item
            script.item(Toys.Chastity_Device).remove();
            assertFalse(chastityDevice.applied());
            assertFalse(onPenis.applied());
        }
    }

    @Test
    public void testItemInstanceRemoveAnyInstanceStringBased() throws IOException {
        try (TestScript script = new TestScript()) {
            Item chastityDevice = script.items("teaselib.Toys.Chastity_Device")
                    .matching("teaselib.Toys.Chastity_Devices.Gates_of_Hell").item();
            State onPenis = script.state("teaselib.Body.OnPenis");

            chastityDevice.apply();

            Assertions.assertTrue(chastityDevice.applied());
            Assertions.assertTrue(onPenis.applied());

            Item otherChastityDevice = script.items("teaselib.Toys.Chastity_Device")
                    .matching("teaselib.Toys.Chastity_Devices.Belt").item();

            // Removing the wrong item doesn't work
            try {
                otherChastityDevice.remove();
                fail("Removing un-applied item must throw");
            } catch (IllegalStateException e) {
                Assertions.assertTrue(chastityDevice.applied());
                Assertions.assertTrue(onPenis.applied());
            }

            // Instead just remove the default item
            script.item("teaselib.Toys.Chastity_Device").remove();
            assertFalse(chastityDevice.applied());
            assertFalse(onPenis.applied());
        }
    }

    @Test
    public void testApplyLotsOfItemInstancesAndRemoveOneAfterAnother() throws IOException {
        try (TestScript script = new TestScript()) {
            State nipples = script.state(Body.OnNipples);
            ArrayList<Item> clothesPegsOnNipples = placeClothesPegs(script, nipples);

            for (Item peg : clothesPegsOnNipples) {
                peg.remove();
            }

            verifyAllPegsRemoved(script, nipples, clothesPegsOnNipples);
        }
    }

    @Test
    public void testApplyLotsOfItemInstancesAndRemoveAtOnce() throws IOException {
        try (TestScript script = new TestScript()) {
            State nipples = script.state(Body.OnNipples);
            ArrayList<Item> clothesPegsOnNipples = placeClothesPegs(script, nipples);

            // remove just on item instance
            clothesPegsOnNipples.remove(0).remove();
            Assertions.assertTrue(script.state(Household.Clothes_Pegs).applied());
            // Testing script default item doesn'twork here,
            // since we've created a lot of temporary items ourselves
            script.state(Household.Clothes_Pegs).remove();

            verifyAllPegsRemoved(script, nipples, clothesPegsOnNipples);
        }
    }

    @Test
    public void testApplyLotsOfItemInstancesAndRemovingAlreadyRemovedDoesntRemoveAll() throws IOException {
        try (TestScript script = new TestScript()) {
            State nipples = script.state(Body.OnNipples);
            State clothesPinsState = script.state(Household.Clothes_Pegs);
            Item notApplied = script.item(Household.Clothes_Pegs);

            assertFalse(clothesPinsState.applied());
            placeClothesPegs(script, nipples);
            assertFalse(notApplied.applied());

            Assertions.assertTrue(clothesPinsState.applied());
            notApplied.apply();
            Assertions.assertTrue(notApplied.applied());
            notApplied.remove();
            assertFalse(notApplied.applied());
            Assertions.assertTrue(clothesPinsState.applied());
        }
    }

    @Test
    public void testApplyLotsOfItemInstancesAndRemovePegsAtOnce() throws IOException {
        try (TestScript script = new TestScript()) {
            State clothesPegs = script.state(Household.Clothes_Pegs);
            State nipples = script.state(Body.OnNipples);

            ArrayList<Item> clothesPegsOnNipples = placeClothesPegs(script, nipples);
            clothesPegs.removeFrom(Body.OnNipples);

            verifyAllPegsRemoved(script, nipples, clothesPegsOnNipples);
        }
    }

    @Test
    public void testApplyLotsOfItemInstancesAndRemoveFromNipplesAtOnce() throws IOException {
        try (TestScript script = new TestScript()) {
            State nipples = script.state(Body.OnNipples);
            List<Item> clothesPegsOnNipples = placeClothesPegs(script, nipples);

            Assertions.assertTrue(script.state(Body.OnNipples).applied());
            nipples.removeFrom(Household.Clothes_Pegs);

            verifyAllPegsRemoved(script, nipples, clothesPegsOnNipples);
        }
    }

    private static ArrayList<Item> placeClothesPegs(TestScript script, State nipples) {
        int numberOfPegs = 10;
        ArrayList<Item> clothesPegsOnNipples = getClothesPegs(script, numberOfPegs);
        script.teaseLib.addUserItems(clothesPegsOnNipples);

        for (Item peg : clothesPegsOnNipples) {
            peg.setAvailable(true);
            Assertions.assertTrue(peg.canApply());
            assertFalse(peg.applied());
            peg.to(Body.OnNipples).apply();
            assertFalse(peg.canApply());
        }
        Assertions.assertTrue(nipples.applied());

        for (Item peg : clothesPegsOnNipples) {
            Assertions.assertTrue(peg.applied());
            Assertions.assertTrue(nipples.is(peg));
            Assertions.assertTrue(peg.is(nipples));
        }
        return clothesPegsOnNipples;
    }

    private static ArrayList<Item> getClothesPegs(TestScript script, int numberOfPegs) {
        ArrayList<Item> clothesPegs = new ArrayList<>(numberOfPegs);
        for (int i = 0; i < numberOfPegs; i++) {
            String name = "Clothes_Peg_" + i;
            Item peg = createPeg(script, name);
            clothesPegs.add(peg);
        }
        return clothesPegs;
    }

    private static Item createPeg(TestScript script, String name) {
        // TODO Improve serialization to allow for white space
        QualifiedString kind = QualifiedString.of(Household.Clothes_Pegs);
        ItemImpl item = new ItemImpl(script.teaseLib, TeaseLib.DefaultDomain, QualifiedString.from(kind, name),
                "A_Clothes_Peg");
        return new ItemProxy(script.namespace, item, script.scriptRenderer.events);
    }

    private static void verifyAllPegsRemoved(TestScript script, State nipples, List<Item> clothesPegsOnNipples) {
        State pegs = script.state(Household.Clothes_Pegs);
        assertFalse(pegs.applied());

        Item clothesPegs = script.item(Household.Clothes_Pegs);
        assertFalse(clothesPegs.applied());
        assertFalse(nipples.applied());

        for (Item peg : clothesPegsOnNipples) {
            assertFalse(peg.applied());
        }
    }

    @Test
    public void testUserItemsInstanciatingAndCaching() throws IOException {
        try (TestScript script = new TestScript()) {
            var gags = script.items(Toys.Gag);

            Item ringGag = gags.matching(Toys.Gags.Ring_Gag).item();
            Item sameRingGag = script.teaseLib.getItem(TeaseLib.DefaultDomain, QualifiedString.of(Toys.Gag),
                    "ring_gag");
            assertEquals(ringGag, sameRingGag);
            assertEquals(sameRingGag, ringGag);
            Assertions.assertNotSame(ringGag, sameRingGag);
        }
    }

    @Test
    public void testItemImplPersistance() throws ReflectiveOperationException, IOException {
        try (TestScript script = new TestScript()) {
            script.debugger.freezeTime();
            ItemImpl gag = (ItemImpl) ((ItemProxy) script.item(Toys.Gag)).item;

            String persisted = Persist.persist(gag);
            Storage storage = Storage.from(persisted);
            Item restored = restoreFromUserItems(script.teaseLib, TeaseLib.DefaultDomain, storage);

            Assertions.assertSame(gag, restored);

            script.debugger.clearStateMaps();
            Storage storage2 = Storage.from(persisted);
            Item restored2 = restoreFromUserItems(script.teaseLib, TeaseLib.DefaultDomain, storage2);

            Assertions.assertNotSame(gag, restored2);
            assertEquals(gag, restored2);
        }
    }

    public static ItemImpl restoreFromUserItems(TeaseLib teaseLib, String domain, Storage storage)
            throws ReflectiveOperationException {
        var name = new QualifiedString(storage.next());
        return (ItemImpl) teaseLib.getItem(domain, name);
    }

    @Test
    public void testItemPersistanceAndRestoreSimple() throws IOException {
        try (TestScript script = new TestScript()) {
            Item gag = script.item(Toys.Gag);
            gag.apply().remember(Until.Removed);
            Assertions.assertTrue(gag.applied());

            script.debugger.clearStateMaps();

            Item restored = script.item(Toys.Gag);
            Assertions.assertTrue(restored.applied());
            assertEquals(gag, restored);
        }
    }

    @Test
    public void testItemInstancePersistAndRestoreWithAttributesSmallTest() throws IOException {
        try (TestScript script = new TestScript()) {
            script.debugger.freezeTime();

            var gags1 = script.items(Toys.Gag);
            Item persisted = gags1.matching(Toys.Gags.Ring_Gag).item();
            persisted.apply().over(1, TimeUnit.HOURS).remember(Until.Removed);

            script.debugger.clearStateMaps();

            var gags = script.items(Toys.Gag);
            Item restored = gags.matching(Toys.Gags.Ring_Gag).item();

            State inMouth = script.state(Body.InMouth);
            Assertions.assertTrue(inMouth.is(restored));
            Assertions.assertTrue(restored.applied());
        }
    }

    @Test
    public void testItemNamespace() throws IOException {
        try (TestScript script = new TestScript()) {
            script.debugger.freezeTime();

            var gags = script.items(Toys.Gag);
            Item ringGag = gags.matching(Toys.Gags.Ring_Gag).item();
            Item muzzleGag = gags.matching(Toys.Gags.Muzzle_Gag).item();
            ringGag.apply();

            Assertions.assertTrue(ringGag.applied());
            assertFalse(muzzleGag.applied());

            State inMouth = script.state(Body.InMouth);

            Assertions.assertTrue(inMouth.is(ringGag));
            assertFalse(inMouth.is(muzzleGag));

            Assertions.assertTrue(inMouth.is(script.namespace));
            // inMouth has not been applied as an item, and is not an item
            // -> TODO allow only pre-defined items to be applied as such
            assertFalse(script.item(Body.InMouth).is(script.namespace));

            Assertions.assertTrue(ringGag.is(script.namespace));
            assertFalse(muzzleGag.is(script.namespace));
        }
    }

    @Test
    public void testItemInstancePersistAndRestoreWithAttributes() throws IOException {
        try (TestScript script = new TestScript()) {
            Item ringGag = applyRingGagAndRemember(script);
            verifyInMouth(script);
            verifyGagApplied(script, ringGag);

            script.debugger.clearStateMaps();

            var gags = script.items(Toys.Gag);
            Item ringGag2 = gags.matching(Toys.Gags.Ring_Gag).item();
            Assertions.assertNotSame(ringGag, ringGag2);

            verifyInMouth(script);
            verifyGagApplied(script, ringGag2);
        }
    }

    @Test
    public void testItemRestoreSequenceWhenReferencedToysHaveBeenRealizedFirst() throws IOException {
        try (TestScript script = new TestScript()) {
            Item ringGag = applyRingGagAndRemember(script);
            verifyInMouth(script);
            verifyGagApplied(script, ringGag);

            script.debugger.clearStateMaps();

            verifyInMouth(script);

            var gags = script.items(Toys.Gag);
            Item ringGag2 = gags.matching(Toys.Gags.Ring_Gag).item();
            verifyGagApplied(script, ringGag2);
        }
    }

    private static Item applyRingGagAndRemember(TestScript script) {
        var gags = script.items(Toys.Gag);
        Item ringGag = gags.matching(Toys.Gags.Ring_Gag).item();

        assertFalse(ringGag.is(Body.InMouth));
        ringGag.apply().remember(Until.Removed);
        return ringGag;
    }

    private static void verifyInMouth(TestScript script) {
        State inMouth = script.state(Body.InMouth);
        Assertions.assertTrue(inMouth.applied());
        Assertions.assertTrue(inMouth.is(Toys.Gag));
        Assertions.assertTrue(inMouth.is(Gags.Ring_Gag));
    }

    private static void verifyGagApplied(TestScript script, Item gag) {
        Assertions.assertTrue(gag.applied());
        Assertions.assertTrue(gag.is(Body.InMouth));
        Assertions.assertTrue(gag.is(script.state(Body.InMouth)));
        State inMouth = script.state(Body.InMouth);
        Assertions.assertTrue(inMouth.is(gag));
    }

    @Test
    public void testCanApplyMultipleInstancesWithoutDefaultPeers() throws IOException {
        try (TestScript script = new TestScript()) {
            ArrayList<Item> clothesPegsOnNipples = getClothesPegs(script, 10);
            for (Item peg : clothesPegsOnNipples) {
                peg.setAvailable(true);
                Assertions.assertTrue(peg.canApply());
                assertFalse(peg.applied());
                peg.apply();

                State pegs = script.state(Household.Clothes_Pegs);
                Assertions.assertTrue(pegs.applied());
                Assertions.assertTrue(peg.applied());
                Assertions.assertTrue(peg.is(peg));
                assertFalse(peg.canApply());
            }
        }
    }

    @Test
    public void testCanApplySingleInstancesWithDefaultPeers() throws IOException {
        try (TestScript script = new TestScript()) {
            Item nippleClamps = script.item(Toys.Nipple_Clamps);
            nippleClamps.setAvailable(true);
            Assertions.assertTrue(nippleClamps.canApply());
            nippleClamps.apply();
            Assertions.assertTrue(nippleClamps.applied());
            Assertions.assertTrue(nippleClamps.is(nippleClamps));

            State onNipples = script.state(Body.OnNipples);
            Assertions.assertTrue(nippleClamps.is(nippleClamps));
            Assertions.assertTrue(nippleClamps.is(onNipples));
            Assertions.assertTrue(onNipples.is(nippleClamps));

            assertFalse(nippleClamps.canApply());
        }
    }

    @Test
    public void testItemIdentityEnumVsString() throws IOException {
        try (TestScript script = new TestScript()) {
            Item clothesPegsByEnum = script.item(Household.Clothes_Pegs);
            Item clothesPegsByString = script.item("teaselib.household.clothes_pegs");

            assertEquals(clothesPegsByEnum, clothesPegsByString);
            assertSame(((ItemProxy) clothesPegsByEnum).state, ((ItemProxy) clothesPegsByString).state);
        }
    }

    @Test
    public void testThatItemAppliesStateAsWell() throws IOException {
        try (TestScript script = new TestScript()) {
            script.debugger.freezeTime();

            script.item(Toys.Gag).apply().over(1, TimeUnit.HOURS).remember(Until.Removed);
            Assertions.assertTrue(script.item(Toys.Gag).applied());
            Assertions.assertTrue(script.state(Toys.Gag).applied());

            script.debugger.clearStateMaps();

            Assertions.assertTrue(script.item(Toys.Gag).applied());
            Assertions.assertTrue(script.state(Toys.Gag).applied());
        }
    }

    @Test
    public void testThatQueriedItemAppliesStateAsWell2() throws IOException {
        try (TestScript script = new TestScript()) {
            script.debugger.freezeTime();

            var gag = script.items(Toys.Gag).matching(Toys.Gags.Ring_Gag).item();
            gag.apply().over(1, TimeUnit.HOURS).remember(Until.Removed);
            Assertions.assertTrue(gag.applied());
            Assertions.assertTrue(script.state(Toys.Gag).applied());

            script.debugger.clearStateMaps();

            var restored = script.items(Toys.Gag).matching(Toys.Gags.Ring_Gag).item();
            Assertions.assertTrue(restored.applied());
            Assertions.assertTrue(script.state(Toys.Gag).applied());
        }
    }

    @Test
    public void testThatRestoredItemCanBeRemoved() throws IOException {
        try (TestScript script = new TestScript()) {
            script.debugger.freezeTime();

            Item gag = script.items(Toys.Gag).matching(Toys.Gags.Ring_Gag).item();
            gag.apply().over(1, TimeUnit.HOURS).remember(Until.Removed);
            Assertions.assertTrue(gag.applied());
            Assertions.assertTrue(script.state(Toys.Gag).applied());

            gag.remove();
            assertFalse(gag.applied());
            gag.apply().over(1, TimeUnit.HOURS).remember(Until.Removed);

            script.debugger.clearStateMaps();

            Item restored = script.item(Toys.Gag);
            Assertions.assertTrue(restored.applied());
            Assertions.assertTrue(script.state(Toys.Gag).applied());

            restored.remove();
            assertFalse(restored.applied());
        }
    }

    @Test
    public void testThatItemGuidIsRestoredProperly() throws ReflectiveOperationException, IOException {
        try (TestScript script = new TestScript()) {
            script.debugger.freezeTime();

            Item gag = script.items(Toys.Gag).matching(Toys.Gags.Ring_Gag).item();
            gag.apply().over(1, TimeUnit.HOURS).remember(Until.Removed);

            script.debugger.clearStateMaps();

            State gagState = script.state(Toys.Gag);

            TeaseLib.PersistentString persistentString = script.teaseLib.getString(
                    QualifiedName.of(TeaseLib.DefaultDomain,
                            "Toys.Gag", "state.peers"));
            String persisted = persistentString.value();
            assertNotEquals("", persisted);
            List<String> peers = new PersistedObject(ArrayList.class, persisted).toValues();
            assertEquals(3, peers.size());

            for (String persistedPeer : peers) {
                Object peer = Persist.from(persistedPeer);
                Assertions.assertTrue(gagState.is(peer), "State is missing persisted " + peer);
            }
        }
    }

    @Test
    public void testThatAppliedItemCanBeTestedWithGenericQuery() throws IOException {
        try (TestScript script = new TestScript()) {
            script.debugger.freezeTime();

            Item gag = script.items(Toys.Gag).matching(Toys.Gags.Ring_Gag).item();
            gag.apply().over(1, TimeUnit.HOURS).remember(Until.Removed);
            Assertions.assertTrue(gag.applied());
            Assertions.assertTrue(script.item(Toys.Gag).applied());
            Assertions.assertTrue(script.item(Toys.Gag).is(Gags.Ring_Gag));

            script.debugger.clearStateMaps();

            Item restored = script.items(Toys.Gag).matching(Toys.Gags.Ring_Gag).item();
            Assertions.assertTrue(restored.applied());
            Assertions.assertTrue(script.item(Toys.Gag).applied());
            Assertions.assertTrue(script.item(Toys.Gag).is(Gags.Ring_Gag));
        }
    }

    @Test
    public void testThatItemIsNotOtherItemOfSameKind() throws IOException {
        try (TestScript script = new TestScript()) {
            script.debugger.freezeTime();

            Item ringGag = script.items(Toys.Gag).matching(Toys.Gags.Ring_Gag).item();
            ringGag.apply();
            Assertions.assertTrue(ringGag.is(ringGag));

            Item bitGag = script.items(Toys.Gag).matching(Toys.Gags.Bit_Gag).item();
            assertNotEquals(ringGag, bitGag);
            assertFalse(ringGag.is(bitGag));

            Assertions.assertTrue(ringGag.applied());
            assertFalse(bitGag.applied());
        }
    }

    @Test
    public void testThatItemIsNotOtherItem() throws IOException {
        try (TestScript script = new TestScript()) {
            script.debugger.freezeTime();

            Item ringGag = script.items(Toys.Gag).matching(Toys.Gags.Ring_Gag).item();
            ringGag.apply();
            Assertions.assertTrue(ringGag.is(ringGag));

            Item analBeads = script.items(Toys.Buttplug).matching(Toys.Anal.Beads).item();
            assertNotEquals(ringGag, analBeads);
            assertFalse(ringGag.is(analBeads));
        }
    }

    @Test
    public void testThatStateIsItem() throws IOException {
        try (TestScript script = new TestScript()) {
            script.debugger.freezeTime();

            Item gag = script.item(Toys.Gag);
            gag.apply();
            Assertions.assertTrue(gag.applied());
            Assertions.assertTrue(script.state(Toys.Gag).applied());
            Assertions.assertTrue(script.state(Body.InMouth).is(gag));
            Assertions.assertTrue(script.state(Toys.Gag).is(gag));
        }
    }

    @Test
    public void testRetrievingItemViaString() throws IOException {
        try (TestScript script = new TestScript()) {
            script.debugger.freezeTime();

            Item ringGag = script.items(Toys.Gag).matching(Toys.Gags.Ring_Gag).item();
            Item bitGag = script.items(Toys.Gag).matching(Toys.Gags.Bit_Gag).item();

            QualifiedString ringGagRef = QualifiedString.of(ringGag);
            QualifiedString bitGagRef = QualifiedString.of(bitGag);
            assertNotEquals(ringGagRef, bitGagRef);
            assertNotEquals(ringGagRef.toString(), bitGagRef.toString());

            Item ballGag = script.defaultDomain.items(Toys.Gag).matching(Toys.Gags.Ball_Gag).item();
            Assertions.assertTrue(ballGag.is(Gags.Ball_Gag));
            assertEquals(ringGag, script.item(ringGagRef.toString()));
            assertEquals(bitGag, script.item(bitGagRef.toString()));
        }
    }

}
