/**
 * 
 */
package teaselib.util;

import static org.junit.Assert.*;

import java.util.ArrayList;

import org.junit.Ignore;
import org.junit.Test;

import teaselib.Body;
import teaselib.Household;
import teaselib.State;
import teaselib.TeaseScript;
import teaselib.Toys;
import teaselib.core.TeaseLib;
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
        Item ringGag = gags.get(Toys.Gags.Ring_Gag);
        assertTrue(ringGag.is(Toys.Gags.Ring_Gag));

        Item sameRingGag = script.items(Toys.Gag).get(Toys.Gags.Ring_Gag);

        assertEquals(ringGag, sameRingGag);

        assertTrue(ringGag.is(Toys.Gags.Ring_Gag));
        assertTrue(sameRingGag.is(Toys.Gags.Ring_Gag));
    }

    @Test
    public void testApplyingTheIdenticalItem() {
        TeaseScript script = TestScript.getOne();
        Items gags = script.items(Toys.Gag);

        Item ringGag = gags.get(Toys.Gags.Ring_Gag);
        assertFalse(ringGag.is(Body.InMouth));
        ringGag.apply();

        assertTrue(script.state(Body.InMouth).is(Toys.Gag));
        assertTrue(script.state(Body.InMouth).is(Toys.Gags.Ring_Gag));
        assertTrue(ringGag.is(Body.InMouth));

        State mouth = script.state(Body.InMouth);
        assertTrue(mouth.is(ringGag));
    }

    @Test
    public void testComparingItemsAndStateWorks() {
        TeaseScript script = TestScript.getOne();
        Items gags = script.items(Toys.Gag);
        Item ringGag = gags.get(Toys.Gags.Ring_Gag);

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

        Item chastityDevice = script.items(Toys.Chastity_Device).get(Toys.ChastityDevices.Gates_of_Hell);
        State onPenis = script.state(Body.OnPenis);

        chastityDevice.apply();

        assertTrue(chastityDevice.applied());
        assertTrue(onPenis.applied());

        Item otherChastityDevice = script.items(Toys.Chastity_Device).get(Toys.ChastityDevices.Chastity_Belt);
        otherChastityDevice.remove();

        assertFalse(chastityDevice.applied());
        assertFalse(onPenis.applied());
    }

    @Test
    public void testItemInstanceRemoveAnyInstanceStringBased() {
        TestScript script = TestScript.getOne();

        Item chastityDevice = script.items("teaselib.Toys.Chastity_Device")
                .get("teaselib.Toys.ChastityDevices.Gates_of_Hell");
        State onPenis = script.state("teaselib.Body.OnPenis");

        chastityDevice.apply();

        assertTrue(chastityDevice.applied());
        assertTrue(onPenis.applied());

        Item otherChastityDevice = script.items("teaselib.Toys.Chastity_Device")
                .get("teaselib.Toys.ChastityDevices.Chastity_Belt");
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

    @Test
    public void testApplyLotsOfItemInstancesAndRemovePegsAtOnce() {
        TestScript script = TestScript.getOne();
        State nipples = script.state(Body.OnNipples);

        ArrayList<Item> clothesPegsOnNipples = placeClothesPegs(script, nipples);

        script.item(Household.Clothes_Pegs).removeFrom(Body.OnNipples);

        verifyAllPegsRemoved(script, nipples, clothesPegsOnNipples);
    }

    @Test
    public void testApplyLotsOfItemInstancesAndRemoveFromNipplesAtOnce() {
        TestScript script = TestScript.getOne();
        State nipples = script.state(Body.OnNipples);

        ArrayList<Item> clothesPegsOnNipples = placeClothesPegs(script, nipples);

        script.item(Body.OnNipples).removeFrom(Household.Clothes_Pegs);

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
        ArrayList<Item> clothesPegs = new ArrayList<Item>(numberOfPegs);
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
    @Ignore
    // TODO Wrong, can't just call constructor -> construct via teaselib user items
    public void testItemInstancePersistence() {
        TestScript script = TestScript.getOne();
        ItemImpl peg = createPeg(script, "test_peg");

        // TODO Rename Persist to Persistence - collision with State.Persistence interface
        String serialized = Persist.persist(peg);

        // TODO In production, we may be able to instanciate ItemImpl from cache
        ItemImpl restoredPeg = new ItemImpl(script.teaseLib, new Persist.Storage(serialized));

        assertEquals(peg, restoredPeg);
    }

    @Test
    public void testUserItemsInstanciatingAndCaching() {
        TestScript script = TestScript.getOne();

        Items gags = script.items(Toys.Gag);
        Item ringGag = gags.get(Toys.Gags.Ring_Gag);

        Item sameRingGag = script.teaseLib.getByGuid(TeaseLib.DefaultDomain, Toys.Gag, "ring_gag");

        assertEquals(ringGag, sameRingGag);
        assertEquals(sameRingGag, ringGag);

        assertFalse(ringGag == sameRingGag);
    }

    @Test
    public void testItemInstancePersistAndRestore() {
        TestScript script = TestScript.getOne();

        Item ringGag = applyRingGagAndRemember(script);
        verifyApplied(script, ringGag);

        script.debugger.clearStateMaps();

        Items gags = script.items(Toys.Gag);
        ringGag = gags.get(Toys.Gags.Ring_Gag);

        verifyApplied(script, ringGag);
    }

    @Test
    public void testItemRestoreSequenceWhenReferencedToysHaveBeenRealizedFirst() {
        TestScript script = TestScript.getOne();

        Item ringGag = applyRingGagAndRemember(script);
        verifyApplied(script, ringGag);

        script.debugger.clearStateMaps();

        State inMouth = script.state(Body.InMouth);
        assertTrue(inMouth.applied());

        Items gags = script.items(Toys.Gag);
        ringGag = gags.get(Toys.Gags.Ring_Gag);

        verifyApplied(script, ringGag);
    }

    private static Item applyRingGagAndRemember(TestScript script) {
        Items gags = script.items(Toys.Gag);
        Item ringGag = gags.get(Toys.Gags.Ring_Gag);

        assertFalse(ringGag.is(Body.InMouth));
        ringGag.apply().remember();
        return ringGag;
    }

    private static void verifyApplied(TestScript script, Item ringGag) {
        assertTrue(ringGag.applied());
        assertTrue(ringGag.is(Body.InMouth));
        assertTrue(ringGag.is(script.state(Body.InMouth)));
    }

    // TODO Changed default of wrist restraints to no defaults
    // -> test canApply with no defaults() (changed to check for instance applied to its own state)
    // canApply() changed to check whether the instance has been applied - correct since you can apply each instance
    // only
    // once
    //
    // -> change Mine to apply wrist restraints to TiedInFront

    @Test
    public void testCanApplyMultipleInstances() {
        TestScript script = TestScript.getOne();

        ArrayList<Item> clothesPegsOnNipples = getClothesPegs(script, 10);

        for (Item peg : clothesPegsOnNipples) {
            assertTrue(peg.canApply());
            peg.applyTo(Body.OnNipples);

            State pegs = script.state(Household.Clothes_Pegs);

            assertTrue(peg.applied());
            assertFalse(peg.canApply());
            assertTrue(peg.is(peg));
        }
    }
}
