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

public class ItemsTests {

    @Test
    public void testGetAvailableItemsFirst() throws Exception {
        TeaseScript script = TestScript.getOne();
        Items gags = script.items(Toys.Gag);

        assertFalse(gags.isAvailable());
        assertFalse(gags.get(0).is(Toys.Gags.Ring_Gag));

        Item ringGag = gags.get(Toys.Gags.Ring_Gag);
        assertTrue(ringGag.is(Toys.Gags.Ring_Gag));

        ringGag.setAvailable(true);
        assertTrue(gags.isAvailable());

        Items sameGags = script.items(Toys.Gag);
        assertTrue(sameGags.isAvailable());
        assertFalse(gags.get(0).is(Toys.Gags.Ring_Gag));

        Item sameRingGag = script.item(Toys.Gag);
        assertTrue(sameRingGag.isAvailable());
        assertTrue(sameRingGag.is(Toys.Gags.Ring_Gag));

        Item againTheSameGag = script.items(Toys.Gag).available().get(0);
        assertTrue(againTheSameGag.isAvailable());
        assertTrue(againTheSameGag.is(Toys.Gags.Ring_Gag));
        assertFalse(gags.get(0).is(Toys.Gags.Ring_Gag));

        // TODO Doesn't work because of TeaseScript interception proxies
        // assertEquals(ringGag, sameRingGag);
        // assertEquals(ringGag, againTheSameGag);
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
        Item sameRingGag = gags.get(Toys.Gags.Ring_Gag);

        assertEquals(sameRingGag, ringGag);
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
        assertTrue(collars.get(Toys.Collars.Posture_Collar).isAvailable());

        Item noDogCollar = collars.prefer(Toys.Collars.Dog_Collar);
        assertEquals(postureCollar, noDogCollar);

        Item availablePostureCollar = collars.prefer(Toys.Collars.Posture_Collar);
        assertEquals(postureCollar, availablePostureCollar);
    }

    @Test
    public void testAny() {
        TeaseScript script = TestScript.getOne();
        Items collars = script.items(Toys.Collar);

        assertEquals(Item.NotAvailable, collars.get());

        assertEquals(Item.NotAvailable, script.any(Toys.Collar));

        Item postureCollar = collars.get(Toys.Collars.Posture_Collar);
        postureCollar.setAvailable(true);

        assertEquals(postureCollar, script.any(Toys.Collar));
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

    @Test
    public void showThatNamespaceAttributeIsSymetric() {
        TestScript script = TestScript.getOne();

        script.item(Toys.Nipple_Clamps).apply();
        assertTrue(script.state(Body.OnNipples).is(script.namespace));
        assertTrue(script.state(Body.OnNipples).is(Toys.Nipple_Clamps));
        assertTrue(script.item(Toys.Nipple_Clamps).is(Body.OnNipples));
        assertTrue(script.item(Toys.Nipple_Clamps).is(script.namespace));

        script.item(Toys.Nipple_Clamps).remove();

        assertFalse(script.state(Body.OnNipples).is(script.namespace));
        assertFalse(script.state(Body.OnNipples).is(Toys.Nipple_Clamps));
        assertFalse(script.state(Toys.Nipple_Clamps).is(Body.OnNipples));
        assertFalse(script.state(Toys.Nipple_Clamps).is(script.namespace));

        assertFalse(script.item(Toys.Nipple_Clamps).applied());
        assertFalse(script.state(Body.OnNipples).applied());
    }
}
