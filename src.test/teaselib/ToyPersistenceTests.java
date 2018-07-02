/**
 * 
 */
package teaselib;

import static org.junit.Assert.assertTrue;

import org.junit.Test;

import teaselib.core.TeaseLib;
import teaselib.hosts.SexScriptsPropertyNameMapping;
import teaselib.test.TestScript;
import teaselib.util.Item;

/**
 * @author Citizen-Cane
 *
 */
public class ToyPersistenceTests {
    @Test
    public void testToysAndClothing() {
        TestScript script = TestScript.getOne();

        Item gag = script.teaseLib.item(TeaseLib.DefaultDomain, Toys.Gag);
        assertTrue(gag.is(Toys.Gags.Ball_Gag));

        gag.setAvailable(true);
        if (script.persistence.getNameMapping() instanceof SexScriptsPropertyNameMapping) {
            assertTrue(script.persistence.storage.containsKey("Toys.ballgag"));
        } else {
            assertTrue(script.persistence.storage.containsKey("Toys.ball_gag"));
        }

        script.teaseLib.item(Clothes.Female, Clothes.Shoes).setAvailable(true);
        assertTrue(script.persistence.storage.containsKey("Female.Clothes.High_Heels"));
    }

    @Test
    public void testToysAndClothingAsItems() {
        TestScript script = TestScript.getOne();

        Item gag = script.teaseLib.item(TeaseLib.DefaultDomain, Toys.Gag);
        assertTrue(gag.is(Toys.Gags.Ball_Gag));

        gag.setAvailable(true);
        if (script.persistence.getNameMapping() instanceof SexScriptsPropertyNameMapping) {
            assertTrue(script.persistence.storage.containsKey(Toys.class.getSimpleName() + ".ballgag"));
        } else {
            assertTrue(script.persistence.storage.containsKey(Toys.class.getSimpleName() + ".ball_gag"));
        }

        script.teaseLib.item(TeaseLib.DefaultDomain, Clothes.Shoes).setAvailable(true);
        assertTrue(script.persistence.storage.containsKey(Clothes.class.getSimpleName() + ".High_Heels"));
    }

    public void testAssignedToysAndClothingAsItems() {
        TestScript script = TestScript.getOne();

        script.teaseLib.item(Clothes.Maid, Toys.Gag).setAvailable(true);
        assertTrue(script.persistence.storage.containsKey("Maid." + Toys.class.getSimpleName() + ".ball_gag"));

        script.teaseLib.item(Clothes.Female, Clothes.Shoes).setAvailable(true);

        assertTrue(script.persistence.storage.containsKey("Female." + Clothes.class.getSimpleName() + ".high_heels"));
    }
}
