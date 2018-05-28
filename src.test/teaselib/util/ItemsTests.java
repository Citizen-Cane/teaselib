package teaselib.util;

import static org.junit.Assert.*;

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
    public void testGetAvailableItemsFirst() {
        TeaseScript script = TestScript.getOne();
        Items gags = script.items(Toys.Gag);

        assertFalse(gags.anyAvailable());
        assertFalse(gags.get(0).is(Toys.Gags.Ring_Gag));

        Item ringGag = gags.get(Toys.Gags.Ring_Gag);
        assertTrue(ringGag.is(Toys.Gags.Ring_Gag));

        ringGag.setAvailable(true);
        assertTrue(gags.anyAvailable());

        Items sameGags = script.items(Toys.Gag);
        assertTrue(sameGags.anyAvailable());
        assertFalse(gags.get(0).is(Toys.Gags.Ring_Gag));

        Item sameRingGag = script.item(Toys.Gag);
        assertTrue(sameRingGag.isAvailable());
        assertTrue(sameRingGag.is(Toys.Gags.Ring_Gag));

        Item againTheSameGag = script.items(Toys.Gag).getAvailable().get(0);
        assertTrue(againTheSameGag.isAvailable());
        assertTrue(againTheSameGag.is(Toys.Gags.Ring_Gag));
        assertFalse(gags.get(0).is(Toys.Gags.Ring_Gag));

        assertEquals(ringGag, sameRingGag);
        assertEquals(ringGag, againTheSameGag);
    }

    @Test
    public void testAvailable() {
        TeaseScript script = TestScript.getOne();
        Items gags = script.items(Toys.Gag);

        assertFalse(gags.anyAvailable());

        Item ringGag = gags.get(Toys.Gags.Ring_Gag);
        assertTrue(ringGag.is(Toys.Gags.Ring_Gag));

        assertEquals(0, gags.getAvailable().size());
        assertNotEquals(ringGag, script.item(Toys.Gag));
        assertTrue(script.item(Toys.Gag).is(Toys.Gags.Ball_Gag));

        ringGag.setAvailable(true);
        assertTrue(ringGag.isAvailable());

        assertEquals(ringGag, gags.getAvailable().get(0));

    }

    @Test
    public void testAll() {
        TeaseScript script = TestScript.getOne();
        Items gags = script.items(Toys.Gag);
        // TODO test that all() is AND
        Items allMetalAndLeather = gags.getAll(Material.Metal, Material.Leather);
        assertEquals(1, allMetalAndLeather.size());

        Item ringGag = allMetalAndLeather.get(0);
        Item sameRingGag = gags.get(Toys.Gags.Ring_Gag);

        assertEquals(sameRingGag, ringGag);
    }

    @Test
    public void testGet() {
        TeaseScript script = TestScript.getOne();
        Items collars = script.items(Toys.Collar);
        assertEquals(Toys.Collars.values().length, collars.size());

        assertNotEquals(Item.NotFound, collars.get());
        assertFalse(collars.get().isAvailable());

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

        Item noDogCollar = collars.prefer(Toys.Collars.Dog_Collar).get();
        assertEquals(postureCollar, noDogCollar);

        Item availablePostureCollar = collars.prefer(Toys.Collars.Posture_Collar).get();
        assertEquals(postureCollar, availablePostureCollar);
    }

    // TODO Add more tests for prefer() in order to find out if the method makes sense
    // TODO Add tests for selectAppliableSet() in order to find out if the method makes sense

    @Test
    public void testContains() {
        TeaseScript script = TestScript.getOne();
        Items collars = script.items(Toys.Collar);
        assertEquals(Toys.Collars.values().length, collars.size());

        assertTrue(collars.contains(Toys.Collar));
        assertFalse(collars.contains(Toys.Buttplug));

        assertTrue(collars.contains("teaselib.Toys.Collar"));
        assertFalse(collars.contains("teaselib.Toys.Buttplug"));
    }

    @Test
    public void testAny() {
        TeaseScript script = TestScript.getOne();
        Items collars = script.items(Toys.Collar);
        assertEquals(Toys.Collars.values().length, collars.size());

        assertNotEquals(Item.NotFound, collars.get());
        assertFalse(collars.get().isAvailable());
        for (Item item : script.items(Toys.Collar)) {
            assertTrue(!item.isAvailable());
        }
        assertTrue(script.items(Toys.Collar).getAvailable().isEmpty());

        Item postureCollar = collars.get(Toys.Collars.Posture_Collar);
        postureCollar.setAvailable(true);

        assertEquals(1, script.items(Toys.Collar).getAvailable().size());
        assertEquals(postureCollar, script.items(Toys.Collar).getAvailable().get(0));
    }

    @Test
    public void testRetainIsLogicalAnd() {
        TeaseScript script = TestScript.getOne();
        Items buttPlugs = script.items(Toys.Buttplug);
        assertTrue(buttPlugs.size() > 1);

        Item analBeads = buttPlugs.get(Toys.Anal.Beads);
        assertNotEquals(Item.NotFound, analBeads);

        Items allAnalbeads = script.items(Toys.Buttplug).getAll(Toys.Anal.Beads);
        assertTrue(allAnalbeads.size() == 1);
        assertEquals(analBeads, allAnalbeads.get(0));
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
}
