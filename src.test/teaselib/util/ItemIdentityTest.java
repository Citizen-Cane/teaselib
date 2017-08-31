/**
 * 
 */
package teaselib.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;

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
    public void testItemInstancePersistence() {
        TestScript script = TestScript.getOne();
        ItemImpl peg = createPeg(script, "test_peg");

        String serialized = Persist.persist(peg);
        ItemImpl restoredPeg = new ItemImpl(script.teaseLib, new Persist.Persisted(serialized));

        assertEquals(peg, restoredPeg);
    }

    @Test
    public void testItemInstancePersistAndRestore() {
        TestScript script = TestScript.getOne();
        Items gags = script.items(Toys.Gag);
        Item ringGag = gags.get(Toys.Gags.Ring_Gag);

        assertFalse(ringGag.is(Body.InMouth));
        ringGag.apply().remember();

        assertTrue(ringGag.is(Body.InMouth));
        assertTrue(ringGag.is(script.state(Body.InMouth)));

        script.persistence.printStorage();

        // TODO clear state map, restore from storage
    }
}
