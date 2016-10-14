/**
 * 
 */
package teaselib;

import static org.junit.Assert.assertTrue;

import org.junit.Test;

import teaselib.core.TeaseLib;
import teaselib.hosts.SexScriptsPropertyNameMapping;
import teaselib.test.TestScript;

/**
 * @author someone
 *
 */
public class ToyPersistenceTests {
    @Test
    public void testToysAndClothing() {
        TestScript script = TestScript.getOne();

        script.teaseLib.getToy(TeaseLib.DefaultDomain, Toys.Ball_Gag)
                .setAvailable(true);
        if (script.persistence
                .getNameMapping() instanceof SexScriptsPropertyNameMapping) {
            assertTrue(script.persistence.storage.containsKey("Toys.ballgag"));
        } else {
            assertTrue(script.persistence.storage.containsKey("Toys.Ball_Gag"));
        }

        script.teaseLib.getClothing(Clothes.Female, Clothes.High_Heels)
                .setAvailable(true);
        assertTrue(script.persistence.storage
                .containsKey("Female.Clothes.High_Heels"));
    }

    @Test
    public void testToysAndClothingAsItems() {
        TestScript script = TestScript.getOne();

        script.teaseLib.item(TeaseLib.DefaultDomain, Toys.Ball_Gag)
                .setAvailable(true);
        if (script.persistence
                .getNameMapping() instanceof SexScriptsPropertyNameMapping) {
            assertTrue(script.persistence.storage
                    .containsKey(Toys.class.getSimpleName() + ".ballgag"));
        } else {
            assertTrue(script.persistence.storage
                    .containsKey(Toys.class.getSimpleName() + ".Ball_Gag"));
        }

        script.teaseLib.item(TeaseLib.DefaultDomain, Clothes.High_Heels)
                .setAvailable(true);
        assertTrue(script.persistence.storage
                .containsKey(Clothes.class.getSimpleName() + ".High_Heels"));
    }

    public void testAssignedToysAndClothingAsItems() {
        TestScript script = TestScript.getOne();

        script.teaseLib.item(Clothes.Maid, Toys.Ball_Gag).setAvailable(true);
        assertTrue(script.persistence.storage.containsKey(
                "Maid." + Toys.class.getSimpleName() + ".Ball_Gag"));

        script.teaseLib.item(Clothes.Female, Clothes.High_Heels)
                .setAvailable(true);

        assertTrue(script.persistence.storage.containsKey(
                "Female." + Clothes.class.getSimpleName() + ".High_Heels"));
    }
}
