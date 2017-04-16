package teaselib.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import teaselib.Material;
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

}
