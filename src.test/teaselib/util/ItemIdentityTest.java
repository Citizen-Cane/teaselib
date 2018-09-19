package teaselib.util;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

import org.junit.Test;

import teaselib.Body;
import teaselib.Household;
import teaselib.State;
import teaselib.TeaseScript;
import teaselib.Toys;
import teaselib.core.TeaseLib;
import teaselib.core.state.ItemProxy;
import teaselib.core.util.Persist;
import teaselib.test.TestScript;

/**
 * @author Citizen-Cane
 *
 */
public class ItemIdentityTest {

    @Test
    public void testRetrievingTheIdenticalItem() {
        TeaseScript script = TestScript.getOne();

        Items gags = script.items(Toys.Gag);
        Item ringGag = gags.query(Toys.Gags.Ring_Gag).get();
        assertTrue(ringGag.is(Toys.Gags.Ring_Gag));

        Item sameRingGag = script.items(Toys.Gag).query(Toys.Gags.Ring_Gag).get();

        assertEquals(ringGag, sameRingGag);

        assertTrue(ringGag.is(Toys.Gags.Ring_Gag));
        assertTrue(sameRingGag.is(Toys.Gags.Ring_Gag));
    }

    @Test
    public void testApplyingTheIdenticalItem() {
        TeaseScript script = TestScript.getOne();
        Items gags = script.items(Toys.Gag);

        Item ringGag = gags.query(Toys.Gags.Ring_Gag).get();
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
        TeaseScript script = TestScript.getOne();
        Items gags = script.items(Toys.Gag);
        Item ringGag = gags.query(Toys.Gags.Ring_Gag).get();

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

        Item chastityDevice = script.items(Toys.Chastity_Device).query(Toys.Chastity_Devices.Gates_of_Hell).get();
        State onPenis = script.state(Body.OnPenis);

        chastityDevice.apply();

        assertTrue(chastityDevice.applied());
        assertTrue(onPenis.applied());

        Item otherChastityDevice = script.items(Toys.Chastity_Device).query(Toys.Chastity_Devices.Belt).get();
        otherChastityDevice.remove();

        assertFalse(chastityDevice.applied());
        assertFalse(onPenis.applied());
    }

    @Test
    public void testItemInstanceRemoveAnyInstanceStringBased() {
        TestScript script = TestScript.getOne();

        Item chastityDevice = script.items("teaselib.Toys.Chastity_Device")
                .query("teaselib.Toys.Chastity_Devices.Gates_of_Hell").get();
        State onPenis = script.state("teaselib.Body.OnPenis");

        chastityDevice.apply();

        assertTrue(chastityDevice.applied());
        assertTrue(onPenis.applied());

        Item otherChastityDevice = script.items("teaselib.Toys.Chastity_Device")
                .query("teaselib.Toys.Chastity_Devices.Belt").get();
        otherChastityDevice.remove();

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

        script.item(Household.Clothes_Pegs).remove();

        verifyAllPegsRemoved(script, nipples, clothesPegsOnNipples);
    }

    @Test(expected = AssertionError.class)
    public void testApplyLotsOfItemInstancesAndRemovePegsAtOnceDoesntWork() {
        TestScript script = TestScript.getOne();
        State nipples = script.state(Body.OnNipples);
        ArrayList<Item> clothesPegsOnNipples = placeClothesPegs(script, nipples);

        // Doesn't work because the specific {@code item(Household.Clothes_Pegs)} instance has ever been added
        // When adding specific instances, those have to be removed explicitly or all at once
        script.item(Household.Clothes_Pegs).removeFrom(Body.OnNipples);

        @SuppressWarnings("unused")
        State pegs = script.state(Household.Clothes_Pegs);
        verifyAllPegsRemoved(script, nipples, clothesPegsOnNipples);
    }

    @Test(expected = AssertionError.class)
    public void testApplyLotsOfItemInstancesAndRemoveFromNipplesAtOnceDoesntWork() {
        TestScript script = TestScript.getOne();
        State nipples = script.state(Body.OnNipples);
        ArrayList<Item> clothesPegsOnNipples = placeClothesPegs(script, nipples);

        // Doesn't work because no Body.OnNipples instance has ever been added,
        // and Body.OnNipples isn't an item anyway - trying to remove the wrong way
        script.item(Body.OnNipples).removeFrom(Household.Clothes_Pegs);

        @SuppressWarnings("unused")
        State pegs = script.state(Household.Clothes_Pegs);
        verifyAllPegsRemoved(script, nipples, clothesPegsOnNipples);
    }

    private static ArrayList<Item> placeClothesPegs(TestScript script, State nipples) {
        int numberOfPegs = 10;
        ArrayList<Item> clothesPegsOnNipples = getClothesPegs(script, numberOfPegs);

        for (Item peg : clothesPegsOnNipples) {
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
        return new ItemImpl(script.teaseLib, Household.Clothes_Pegs, TeaseLib.DefaultDomain, name, "A_Clothes_Peg");
    }

    private static void verifyAllPegsRemoved(TestScript script, State nipples, ArrayList<Item> clothesPegsOnNipples) {
        assertFalse(script.item(Household.Clothes_Pegs).applied());
        assertFalse(nipples.applied());

        for (Item peg : clothesPegsOnNipples) {
            assertFalse(peg.applied());
        }
    }

    @Test
    public void testUserItemsInstanciatingAndCaching() {
        TestScript script = TestScript.getOne();

        Items gags = script.items(Toys.Gag);
        Item ringGag = gags.query(Toys.Gags.Ring_Gag).get();

        Item sameRingGag = script.teaseLib.getByGuid(TeaseLib.DefaultDomain, Toys.Gag, "ring_gag");

        assertEquals(ringGag, sameRingGag);
        assertEquals(sameRingGag, ringGag);

        assertFalse(ringGag == sameRingGag);
    }

    @Test
    public void testItemImplPersistance() {
        TestScript script = TestScript.getOne();
        script.debugger.freezeTime();
        ItemImpl gag = (ItemImpl) ((ItemProxy) script.item(Toys.Gag)).item;

        String persisted = Persist.persist(gag);
        Persist.Storage storage = new Persist.Storage(Persist.persistedValue((persisted)));
        Item restored = ItemImpl.restoreFromUserItems(script.teaseLib, TeaseLib.DefaultDomain, storage);

        assertSame(gag, restored);

        script.debugger.clearStateMaps();
        Persist.Storage storage2 = new Persist.Storage(Persist.persistedValue((persisted)));
        Item restored2 = ItemImpl.restoreFromUserItems(script.teaseLib, TeaseLib.DefaultDomain, storage2);

        assertNotSame(gag, restored2);
        assertEquals(gag, restored2);
    }

    @Test
    public void testItemPersistanceAndRestoreSimple() {
        TestScript script = TestScript.getOne();
        script.debugger.freezeTime();

        Item gag = script.item(Toys.Gag);
        gag.apply().over(1, TimeUnit.HOURS);
        assertTrue(gag.applied());

        script.debugger.clearStateMaps();

        Item restored = script.item(Toys.Gag);
        assertTrue(restored.applied());
        assertEquals(gag, restored);
    }

    @Test
    public void testItemInstancePersistAndRestoreWithAttributes() {
        TestScript script = TestScript.getOne();
        script.debugger.freezeTime();

        Item ringGag = applyRingGagAndRemember(script);
        verifyApplied(script, ringGag);

        script.debugger.clearStateMaps();

        Items gags = script.items(Toys.Gag);
        ringGag = gags.query(Toys.Gags.Ring_Gag).get();

        State inMouth = script.state(Body.InMouth);
        assertTrue(inMouth.applied());
        assertTrue(inMouth.is(Toys.Gag));
        assertTrue(inMouth.is(Toys.Gags.Ring_Gag));
        assertTrue(inMouth.is(script.namespace));

        verifyApplied(script, ringGag);
    }

    @Test
    public void testItemRestoreSequenceWhenReferencedToysHaveBeenRealizedFirst() {
        TestScript script = TestScript.getOne();
        script.debugger.freezeTime();

        Item ringGag = applyRingGagAndRemember(script);
        verifyApplied(script, ringGag);

        script.debugger.clearStateMaps();

        State inMouth = script.state(Body.InMouth);
        assertTrue(inMouth.applied());
        assertTrue(inMouth.is(Toys.Gag));
        assertTrue(inMouth.is(Toys.Gags.Ring_Gag));
        assertTrue(inMouth.is(script.namespace));

        Items gags = script.items(Toys.Gag);
        ringGag = gags.query(Toys.Gags.Ring_Gag).get();

        verifyApplied(script, ringGag);
    }

    private static Item applyRingGagAndRemember(TestScript script) {
        Items gags = script.items(Toys.Gag);
        Item ringGag = gags.query(Toys.Gags.Ring_Gag).get();

        assertFalse(ringGag.is(Body.InMouth));
        ringGag.apply().over(1, TimeUnit.HOURS);
        return ringGag;
    }

    private static void verifyApplied(TestScript script, Item ringGag) {
        assertTrue(ringGag.applied());
        assertTrue(ringGag.is(Body.InMouth));
        assertTrue(ringGag.is(script.state(Body.InMouth)));
    }

    @Test
    public void testCanApplyMultipleInstances() {
        TestScript script = TestScript.getOne();

        ArrayList<Item> clothesPegsOnNipples = getClothesPegs(script, 10);

        for (Item peg : clothesPegsOnNipples) {
            assertTrue(peg.canApply());
            peg.applyTo(Body.OnNipples);

            @SuppressWarnings("unused")
            State pegs = script.state(Household.Clothes_Pegs);

            assertTrue(peg.applied());
            assertFalse(peg.canApply());
            assertTrue(peg.is(peg));
        }
    }

    @Test
    public void testItemIdentity() {
        TestScript script = TestScript.getOne();

        Item clothesPegsByEnum = script.item(Household.Clothes_Pegs);
        Item clothesPegsByString = script.item("teaselib.household.clothes_pegs");

        assertEquals(clothesPegsByEnum, clothesPegsByString);
        assertTrue(((ItemProxy) clothesPegsByEnum).state == ((ItemProxy) clothesPegsByString).state);
    }
}
