package teaselib.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import java.util.concurrent.TimeUnit;

import org.junit.Test;

import teaselib.Body;
import teaselib.Material;
import teaselib.State;
import teaselib.TeaseScript;
import teaselib.Toys;
import teaselib.test.TestScript;

public class ItemsTest {

    @Test
    public void testIsAvailable() throws Exception {
        TeaseScript script = TestScript.getOne();
        Items gags = script.items(Toys.Gag);

        assertFalse(gags.isAvailable());

        Item ringGag = gags.get(Toys.Gags.Ring_Gag);
        assertTrue(ringGag.is(Toys.Gags.Ring_Gag));

        ringGag.setAvailable(true);
        assertTrue(gags.isAvailable());

        Items sameGags = script.items(Toys.Gag);
        assertTrue(sameGags.isAvailable());

        Item sameRingGag = script.item(Toys.Gag);
        assertTrue(sameRingGag.isAvailable());
    }

    @Test
    public void testAvailable() throws Exception {
        TeaseScript script = TestScript.getOne();
        Items gags = script.items(Toys.Gag);

        assertFalse(gags.isAvailable());

        Item ringGag = gags.get(Toys.Gags.Ring_Gag);
        assertTrue(ringGag.is(Toys.Gags.Ring_Gag));

        assertEquals(0, gags.available().size());
        assertNotEquals(ringGag, script.item(Toys.Gag));
        assertTrue(script.item(Toys.Gag).is(Toys.Gags.Ball_Gag));

        ringGag.setAvailable(true);
        assertTrue(ringGag.isAvailable());

        assertEquals(ringGag, gags.available().get(0));

    }

    @Test
    public void testAll() throws Exception {
        TeaseScript script = TestScript.getOne();
        Items gags = script.items(Toys.Gag);
        Items allMetalAndLeather = gags.all(Material.Metal, Material.Leather);
        assertEquals(1, allMetalAndLeather.size());

        Item ringGag = allMetalAndLeather.get(0);
        assertEquals(gags.get(Toys.Gags.Ring_Gag), ringGag);
    }

    @Test
    public void testGet() throws Exception {
        TeaseScript script = TestScript.getOne();
        Items collars = script.items(Toys.Collar);

        assertEquals(Item.NotAvailable, collars.get());

        Item dogCollar = collars.get(Toys.Collars.Dog_Collar);
        assertTrue(dogCollar.is(Toys.Collars.Dog_Collar));

        Item postureCollar = collars.get(Toys.Collars.Posture_Collar);
        postureCollar.setAvailable(true);

        assertEquals(postureCollar, collars.get());
        assertEquals(dogCollar, collars.get(Toys.Collars.Dog_Collar));

        assertFalse(collars.get(Toys.Collars.Dog_Collar).isAvailable());
        assertTrue(collars.get(Toys.Collars.Posture_Collar).isAvailable());

        assertEquals(postureCollar, collars.get());
        assertTrue(collars.like(Toys.Collars.Posture_Collar).isAvailable());

        assertNotEquals(dogCollar, collars.like(Toys.Collars.Dog_Collar));
        assertEquals(postureCollar, collars.like(Toys.Collars.Posture_Collar));
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
}
