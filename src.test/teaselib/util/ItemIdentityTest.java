/**
 * 
 */
package teaselib.util;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.junit.Test;

import teaselib.Body;
import teaselib.Household;
import teaselib.State;
import teaselib.State.Persistence.Until;
import teaselib.Toys;
import teaselib.Toys.Gags;
import teaselib.core.TeaseLib;
import teaselib.core.state.ItemProxy;
import teaselib.core.util.Persist;
import teaselib.core.util.PersistedObject;
import teaselib.core.util.QualifiedEnum;
import teaselib.core.util.QualifiedItem;
import teaselib.core.util.Storage;
import teaselib.test.TestScript;

/**
 * @author Citizen-Cane
 *
 */
public class ItemIdentityTest {
    @Test
    public void testRetrievingTheIdenticalItem() {
        TestScript script = TestScript.getOne();
        Items gags = script.items(Toys.Gag);

        Item ringGag = gags.matching(Toys.Gags.Ring_Gag).get();
        assertTrue(ringGag.is(Toys.Gags.Ring_Gag));

        Item sameRingGag = script.items(Toys.Gag).matching(Toys.Gags.Ring_Gag).get();
        assertEquals(ringGag, sameRingGag);

        assertTrue(ringGag.is(Toys.Gags.Ring_Gag));
        assertTrue(sameRingGag.is(Toys.Gags.Ring_Gag));
    }

    @Test
    public void testApplyingTheIdenticalItem() {
        TestScript script = TestScript.getOne();
        Items gags = script.items(Toys.Gag);

        Item ringGag = gags.matching(Toys.Gags.Ring_Gag).get();
        assertFalse(ringGag.is(Body.InMouth));

        ringGag.apply();
        assertTrue(ringGag.is(Body.InMouth));
        assertTrue(script.state(Body.InMouth).is(Toys.Gag));
        assertTrue(script.state(Body.InMouth).is(Toys.Gags.Ring_Gag));

        State mouth = script.state(Body.InMouth);
        assertTrue(mouth.is(Toys.Gag));
        assertTrue(mouth.is(Toys.Gags.Ring_Gag));
        assertTrue(mouth.is(ringGag));
    }

    @Test
    public void testComparingItemsAndStateWorks() {
        TestScript script = TestScript.getOne();
        Items gags = script.items(Toys.Gag);

        Item ringGag = gags.matching(Toys.Gags.Ring_Gag).get();
        assertFalse(ringGag.is(Body.InMouth));
        ringGag.apply();

        assertTrue(ringGag.is(Body.InMouth));
        assertTrue(ringGag.is(script.state(Body.InMouth)));
    }

    @Test
    public void testItemInstanceRemoveSameInstance() {
        TestScript script = TestScript.getOne();

        Item chastityDevice = script.item(Toys.Chastity_Device);
        State onPenis = script.state(Body.OnPenis);

        chastityDevice.apply();

        assertTrue(chastityDevice.applied());
        assertTrue(onPenis.applied());

        chastityDevice.remove();

        assertFalse(chastityDevice.applied());
        assertFalse(onPenis.applied());
    }

    @Test
    public void testItemInstanceRemoveAnyInstance() {
        TestScript script = TestScript.getOne();

        Item chastityDevice = script.items(Toys.Chastity_Device).matching(Toys.Chastity_Devices.Gates_of_Hell).get();
        State onPenis = script.state(Body.OnPenis);

        chastityDevice.apply();

        assertTrue(chastityDevice.applied());
        assertTrue(onPenis.applied());

        Item otherChastityDevice = script.items(Toys.Chastity_Device).matching(Toys.Chastity_Devices.Belt).get();

        // Removing the wrong item doesn't work
        try {
            otherChastityDevice.remove();
            fail("Removing un-applied item must throw");
        } catch (IllegalStateException e) {
            assertTrue(chastityDevice.applied());
            assertTrue(onPenis.applied());
        }

        assertTrue(chastityDevice.applied());
        assertTrue(onPenis.applied());

        // Instead just remove the default item
        script.item(Toys.Chastity_Device).remove();
        assertFalse(chastityDevice.applied());
        assertFalse(onPenis.applied());
    }

    @Test
    public void testItemInstanceRemoveAnyInstanceStringBased() {
        TestScript script = TestScript.getOne();

        Item chastityDevice = script.items("teaselib.Toys.Chastity_Device")
                .matching("teaselib.Toys.Chastity_Devices.Gates_of_Hell").get();
        State onPenis = script.state("teaselib.Body.OnPenis");

        chastityDevice.apply();

        assertTrue(chastityDevice.applied());
        assertTrue(onPenis.applied());

        Item otherChastityDevice = script.items("teaselib.Toys.Chastity_Device")
                .matching("teaselib.Toys.Chastity_Devices.Belt").get();

        // Removing the wrong item doesn't work
        try {
            otherChastityDevice.remove();
            fail("Removing un-applied item must throw");
        } catch (IllegalStateException e) {
            assertTrue(chastityDevice.applied());
            assertTrue(onPenis.applied());
        }

        // Instead just remove the default item
        script.item("teaselib.Toys.Chastity_Device").remove();
        assertFalse(chastityDevice.applied());
        assertFalse(onPenis.applied());
    }

    @Test
    public void testApplyLotsOfItemInstancesAndRemoveOneAfterAnother() {
        TestScript script = TestScript.getOne();
        State nipples = script.state(Body.OnNipples);
        ArrayList<Item> clothesPegsOnNipples = placeClothesPegs(script, nipples);

        for (Item peg : clothesPegsOnNipples) {
            peg.remove();
        }

        verifyAllPegsRemoved(script, nipples, clothesPegsOnNipples);
    }

    @Test
    public void testApplyLotsOfItemInstancesAndRemoveAtOnce() {
        TestScript script = TestScript.getOne();
        State nipples = script.state(Body.OnNipples);
        ArrayList<Item> clothesPegsOnNipples = placeClothesPegs(script, nipples);

        // remove just on item instance
        clothesPegsOnNipples.remove(0).remove();
        assertTrue(script.state(Household.Clothes_Pegs).applied());
        // Testing script default item doesn'twork here,
        // since we've created a lot of temporary items ourselves
        script.state(Household.Clothes_Pegs).remove();

        verifyAllPegsRemoved(script, nipples, clothesPegsOnNipples);
    }

    @Test
    public void testApplyLotsOfItemInstancesAndRemovingAlreadyRemovedDoesntRemoveAll() {
        TestScript script = TestScript.getOne();
        State nipples = script.state(Body.OnNipples);
        State clothesPinsState = script.state(Household.Clothes_Pegs);

        assertFalse(clothesPinsState.applied());
        placeClothesPegs(script, nipples);
        Item notApplied = script.item(Household.Clothes_Pegs);
        assertFalse(notApplied.applied());

        assertTrue(clothesPinsState.applied());
        notApplied.apply();
        assertTrue(notApplied.applied());
        notApplied.remove();
        assertFalse(notApplied.applied());
        assertTrue(clothesPinsState.applied());
    }

    @Test
    public void testApplyLotsOfItemInstancesAndRemovePegsAtOnce() {
        TestScript script = TestScript.getOne();

        State clothesPegs = script.state(Household.Clothes_Pegs);
        State nipples = script.state(Body.OnNipples);

        ArrayList<Item> clothesPegsOnNipples = placeClothesPegs(script, nipples);
        clothesPegs.removeFrom(Body.OnNipples);

        verifyAllPegsRemoved(script, nipples, clothesPegsOnNipples);
    }

    @Test
    public void testApplyLotsOfItemInstancesAndRemoveFromNipplesAtOnce() {
        TestScript script = TestScript.getOne();
        State nipples = script.state(Body.OnNipples);
        ArrayList<Item> clothesPegsOnNipples = placeClothesPegs(script, nipples);

        assertTrue(script.state(Body.OnNipples).applied());
        script.state(Body.OnNipples).removeFrom(Household.Clothes_Pegs);

        verifyAllPegsRemoved(script, nipples, clothesPegsOnNipples);
    }

    private static ArrayList<Item> placeClothesPegs(TestScript script, State nipples) {
        int numberOfPegs = 10;
        ArrayList<Item> clothesPegsOnNipples = getClothesPegs(script, numberOfPegs);

        for (Item peg : clothesPegsOnNipples) {
            assertFalse(peg.applied());
            peg.applyTo(Body.OnNipples);
        }
        assertTrue(nipples.applied());

        for (Item peg : clothesPegsOnNipples) {
            assertTrue(peg.applied());
            assertTrue(nipples.is(peg));
            assertTrue(peg.is(nipples));
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

    private static ItemImpl createPeg(TestScript script, String name) {
        // TODO Improve serialization to allow for white space
        QualifiedEnum kind = new QualifiedEnum(Household.Clothes_Pegs);
        return new ItemImpl(script.teaseLib, kind.value(), TeaseLib.DefaultDomain,
                ItemGuid.from(kind, name), "A_Clothes_Peg");
    }

    private static void verifyAllPegsRemoved(TestScript script, State nipples, ArrayList<Item> clothesPegsOnNipples) {
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
    public void testUserItemsInstanciatingAndCaching() {
        TestScript script = TestScript.getOne();
        Items gags = script.items(Toys.Gag);

        Item ringGag = gags.matching(Toys.Gags.Ring_Gag).get();
        Item sameRingGag = script.teaseLib.getItem(TeaseLib.DefaultDomain, Toys.Gag, "ring_gag");
        assertEquals(ringGag, sameRingGag);
        assertEquals(sameRingGag, ringGag);
        assertNotSame(ringGag, sameRingGag);
    }

    @Test
    public void testItemImplPersistance() throws ReflectiveOperationException {
        TestScript script = TestScript.getOne();
        script.debugger.freezeTime();
        ItemImpl gag = (ItemImpl) ((ItemProxy) script.item(Toys.Gag)).item;

        String persisted = Persist.persist(gag);
        Storage storage = Storage.from(persisted);
        Item restored = ItemImpl.restoreFromUserItems(script.teaseLib, TeaseLib.DefaultDomain, storage);

        assertSame(gag, restored);

        script.debugger.clearStateMaps();
        Storage storage2 = Storage.from(persisted);
        Item restored2 = ItemImpl.restoreFromUserItems(script.teaseLib, TeaseLib.DefaultDomain, storage2);

        assertNotSame(gag, restored2);
        assertEquals(gag, restored2);
    }

    @Test
    public void testItemPersistanceAndRestoreSimple() {
        TestScript script = TestScript.getOne();

        Item gag = script.item(Toys.Gag);
        gag.apply().remember(Until.Removed);
        assertTrue(gag.applied());

        script.debugger.clearStateMaps();

        Item restored = script.item(Toys.Gag);
        assertTrue(restored.applied());
        assertEquals(gag, restored);
    }

    @Test
    public void testItemInstancePersistAndRestoreWithAttributesSmallTest() {
        TestScript script = TestScript.getOne();
        script.debugger.freezeTime();

        Items gags1 = script.items(Toys.Gag);
        Item persisted = gags1.matching(Toys.Gags.Ring_Gag).get();
        persisted.apply().over(1, TimeUnit.HOURS).remember(Until.Removed);

        script.debugger.clearStateMaps();

        Items gags = script.items(Toys.Gag);
        Item restored = gags.matching(Toys.Gags.Ring_Gag).get();

        State inMouth = script.state(Body.InMouth);
        assertTrue(inMouth.is(restored));
        assertTrue(restored.applied());
    }

    @Test
    public void testItemNamespace() {
        TestScript script = TestScript.getOne();
        script.debugger.freezeTime();

        Items gags = script.items(Toys.Gag);
        Item ringGag = gags.matching(Toys.Gags.Ring_Gag).get();
        ringGag.apply();

        Item muzzleGag = gags.matching(Toys.Gags.Muzzle_Gag).get();

        assertTrue(ringGag.applied());
        assertFalse(muzzleGag.applied());

        State inMouth = script.state(Body.InMouth);
        assertTrue(inMouth.is(ringGag));
        assertFalse(inMouth.is(muzzleGag));

        assertTrue(inMouth.is(script.namespace));
        assertFalse(script.item(Body.InMouth).is(script.namespace));

        assertTrue(ringGag.is(script.namespace));
        assertFalse(muzzleGag.is(script.namespace));
    }

    @Test
    public void testItemInstancePersistAndRestoreWithAttributes() {
        TestScript script = TestScript.getOne();

        Item ringGag = applyRingGagAndRemember(script);
        verifyInMouth(script);
        verifyGagApplied(script, ringGag);

        script.debugger.clearStateMaps();

        Items gags = script.items(Toys.Gag);
        Item ringGag2 = gags.matching(Toys.Gags.Ring_Gag).get();
        assertNotSame(ringGag, ringGag2);

        verifyInMouth(script);
        verifyGagApplied(script, ringGag2);
    }

    @Test
    public void testItemRestoreSequenceWhenReferencedToysHaveBeenRealizedFirst() {
        TestScript script = TestScript.getOne();

        Item ringGag = applyRingGagAndRemember(script);
        verifyInMouth(script);
        verifyGagApplied(script, ringGag);

        script.debugger.clearStateMaps();

        verifyInMouth(script);

        Items gags = script.items(Toys.Gag);
        Item ringGag2 = gags.matching(Toys.Gags.Ring_Gag).get();
        verifyGagApplied(script, ringGag2);
    }

    private static Item applyRingGagAndRemember(TestScript script) {
        Items gags = script.items(Toys.Gag);
        Item ringGag = gags.matching(Toys.Gags.Ring_Gag).get();

        assertFalse(ringGag.is(Body.InMouth));
        ringGag.apply().remember(Until.Removed);
        return ringGag;
    }

    private static void verifyInMouth(TestScript script) {
        State inMouth = script.state(Body.InMouth);
        assertTrue(inMouth.applied());
        assertTrue(inMouth.is(Toys.Gag));
        assertTrue(inMouth.is(Toys.Gags.Ring_Gag));
    }

    private static void verifyGagApplied(TestScript script, Item gag) {
        assertTrue(gag.applied());
        assertTrue(gag.is(Body.InMouth));
        assertTrue(gag.is(script.state(Body.InMouth)));
        State inMouth = script.state(Body.InMouth);
        assertTrue(inMouth.is(gag));
    }

    @Test
    public void testCanApplyMultipleInstancesWithoutDefaultPeers() {
        TestScript script = TestScript.getOne();

        ArrayList<Item> clothesPegsOnNipples = getClothesPegs(script, 10);

        for (Item peg : clothesPegsOnNipples) {
            assertTrue(peg.canApply());
            assertFalse(peg.applied());
            peg.applyTo(Body.OnNipples);

            State pegs = script.state(Household.Clothes_Pegs);
            assertTrue(pegs.applied());
            assertTrue(peg.applied());
            assertTrue(peg.is(peg));

            assertFalse(peg.canApply());
        }
    }

    @Test
    public void testCanApplySingleInstancesWithDefaultPeers() {
        TestScript script = TestScript.getOne();

        Item nippleClamps = script.item(Toys.Nipple_Clamps);
        assertTrue(nippleClamps.canApply());
        nippleClamps.apply();
        assertTrue(nippleClamps.applied());
        assertTrue(nippleClamps.is(nippleClamps));

        assertFalse(nippleClamps.canApply());
    }

    @Test
    public void testItemIdentityEnumVsString() {
        TestScript script = TestScript.getOne();

        Item clothesPegsByEnum = script.item(Household.Clothes_Pegs);
        Item clothesPegsByString = script.item("teaselib.household.clothes_pegs");

        assertEquals(clothesPegsByEnum, clothesPegsByString);
        assertTrue(((ItemProxy) clothesPegsByEnum).state == ((ItemProxy) clothesPegsByString).state);
    }

    @Test
    public void testThatItemAppliesStateAsWell() {
        TestScript script = TestScript.getOne();
        script.debugger.freezeTime();

        script.item(Toys.Gag).apply().over(1, TimeUnit.HOURS).remember(Until.Removed);
        assertTrue(script.item(Toys.Gag).applied());
        assertTrue(script.state(Toys.Gag).applied());

        script.debugger.clearStateMaps();

        assertTrue(script.item(Toys.Gag).applied());
        assertTrue(script.state(Toys.Gag).applied());
    }

    @Test
    public void testThatQueriedItemAppliesStateAsWell2() {
        TestScript script = TestScript.getOne();
        script.debugger.freezeTime();

        Item gag = script.items(Toys.Gag).matching(Toys.Gags.Ring_Gag).get();
        gag.apply().over(1, TimeUnit.HOURS).remember(Until.Removed);
        assertTrue(gag.applied());
        assertTrue(script.state(Toys.Gag).applied());

        script.debugger.clearStateMaps();

        Item restored = script.items(Toys.Gag).matching(Toys.Gags.Ring_Gag).get();
        assertTrue(restored.applied());
        assertTrue(script.state(Toys.Gag).applied());
    }

    @Test
    public void testThatRestoredItemCanBeRemoved() {
        TestScript script = TestScript.getOne();
        script.debugger.freezeTime();

        Item gag = script.items(Toys.Gag).matching(Toys.Gags.Ring_Gag).get();
        gag.apply().over(1, TimeUnit.HOURS).remember(Until.Removed);
        assertTrue(gag.applied());
        assertTrue(script.state(Toys.Gag).applied());

        gag.remove();
        assertFalse(gag.applied());
        gag.apply().over(1, TimeUnit.HOURS).remember(Until.Removed);

        script.debugger.clearStateMaps();

        Item restored = script.item(Toys.Gag);
        assertTrue(restored.applied());
        assertTrue(script.state(Toys.Gag).applied());

        restored.remove();
        assertFalse(restored.applied());
    }

    @Test
    public void testThatItemGuidIsRestoredProperly() throws ReflectiveOperationException {
        TestScript script = TestScript.getOne();
        script.debugger.freezeTime();

        Item gag = script.items(Toys.Gag).matching(Toys.Gags.Ring_Gag).get();
        gag.apply().over(1, TimeUnit.HOURS).remember(Until.Removed);

        script.debugger.clearStateMaps();

        State gagState = script.state(Toys.Gag);

        TeaseLib.PersistentString persistentString = script.teaseLib.new PersistentString(TeaseLib.DefaultDomain,
                "Toys.Gag", "state.peers");
        String persisted = persistentString.value();
        assertNotEquals("", persisted);
        List<String> peers = new PersistedObject(ArrayList.class, persisted).toValues();
        assertEquals(3, peers.size());

        for (String persistedPeer : peers) {
            Object peer = Persist.from(persistedPeer);
            assertTrue("State is missing persisted " + peer, gagState.is(peer));
        }
    }

    @Test
    public void testThatAppliedItemCanBeTestedWithGenericQuery() {
        TestScript script = TestScript.getOne();
        script.debugger.freezeTime();

        Item gag = script.items(Toys.Gag).matching(Toys.Gags.Ring_Gag).get();
        gag.apply().over(1, TimeUnit.HOURS).remember(Until.Removed);
        assertTrue(gag.applied());
        assertTrue(script.item(Toys.Gag).applied());
        assertTrue(script.item(Toys.Gag).is(Toys.Gags.Ring_Gag));

        script.debugger.clearStateMaps();

        Item restored = script.items(Toys.Gag).matching(Toys.Gags.Ring_Gag).get();
        assertTrue(restored.applied());
        assertTrue(script.item(Toys.Gag).applied());
        assertTrue(script.item(Toys.Gag).is(Toys.Gags.Ring_Gag));
    }

    @Test
    public void testThatItemIsNotOtherItemOfSameKind() {
        TestScript script = TestScript.getOne();
        script.debugger.freezeTime();

        Item ringGag = script.items(Toys.Gag).matching(Toys.Gags.Ring_Gag).get();
        ringGag.apply();
        assertTrue(ringGag.is(ringGag));

        Item bitGag = script.items(Toys.Gag).matching(Toys.Gags.Bit_Gag).get();
        assertNotEquals(ringGag, bitGag);
        assertFalse(ringGag.is(bitGag));
        // TODO comparing items works via has(all attributes) but should check peers for item instance

        assertTrue(ringGag.applied());
        assertFalse(bitGag.applied());
    }

    @Test
    public void testThatItemIsNotOtherItem() {
        TestScript script = TestScript.getOne();
        script.debugger.freezeTime();

        Item ringGag = script.items(Toys.Gag).matching(Toys.Gags.Ring_Gag).get();
        ringGag.apply();
        assertTrue(ringGag.is(ringGag));

        Item analBeads = script.items(Toys.Buttplug).matching(Toys.Anal.Beads).get();
        assertNotEquals(ringGag, analBeads);
        assertFalse(ringGag.is(analBeads));
        // TODO comparing items works via has(all attributes) but should check peers for item instance
    }

    @Test
    public void testThatStateIsItem() {
        TestScript script = TestScript.getOne();
        script.debugger.freezeTime();

        Item item = script.item(Toys.Gag);
        item.apply();
        assertTrue(script.item(Toys.Gag).applied());
        assertTrue(script.state(Toys.Gag).applied());
        assertTrue(script.state(Body.InMouth).is(item));
        // TODO Requires item instance applied to item state but we decided against this
        // - value is only applied to peers, not to itself
        // -> can be resolved via peers
        assertTrue(script.state(Toys.Gag).is(item));
    }

    @Test
    // TODO This should work as well, but without adding item instance to item state
    public void testThatStateIsItemWhenAppliedExplicitely() {
        TestScript script = TestScript.getOne();
        script.debugger.freezeTime();

        Item item = script.item(Household.Clothes_Pegs);
        item.apply();
        assertTrue(script.item(Household.Clothes_Pegs).applied());
        assertTrue(script.state(Household.Clothes_Pegs).applied());
        // TODO Requires item instance applied to item state but we decided against this
        // - value is only applied to peers, not to itself
        // -> can be resolved via "peers must be added to item state itself"
        assertTrue(script.state(Household.Clothes_Pegs).is(item));
    }

    @Test
    public void testRetrievingItemViaString() {
        TestScript script = TestScript.getOne();
        script.debugger.freezeTime();

        Item ringGag = script.items(Toys.Gag).matching(Toys.Gags.Ring_Gag).get();
        Item bitGag = script.items(Toys.Gag).matching(Toys.Gags.Bit_Gag).get();

        QualifiedItem ringGagRef = QualifiedItem.of(ringGag);
        QualifiedItem bitGagRef = QualifiedItem.of(bitGag);
        assertNotEquals(ringGagRef, bitGagRef);
        assertNotEquals(ringGagRef.toString(), bitGagRef.toString());

        assertTrue(script.item(Toys.Gag).is(Gags.Ball_Gag));
        assertEquals(ringGag, script.item(ringGagRef.toString()));
        assertEquals(bitGag, script.item(bitGagRef.toString()));
    }

}
