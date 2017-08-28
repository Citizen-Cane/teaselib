/**
 * 
 */
package teaselib.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import teaselib.Body;
import teaselib.State;
import teaselib.TeaseScript;
import teaselib.Toys;
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

        assertTrue(script.state(Body.InMouth).is(ringGag));
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

    // TODO Remove item instance via string

    // TODO Item Persistence

    @Test
    public void testItemInstancePersistence() {
        TestScript script = TestScript.getOne();
        Items gags = script.items(Toys.Gag);
        Item ringGag = gags.get(Toys.Gags.Ring_Gag);

        assertFalse(ringGag.is(Body.InMouth));
        ringGag.apply().remember();

        assertTrue(ringGag.is(Body.InMouth));
        assertTrue(ringGag.is(script.state(Body.InMouth)));

        script.persistence.printStorage();
    }
}
