/**
 * 
 */
package teaselib;

import static org.junit.Assert.*;

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
    public void testDomainSeparation() {
        TestScript script = TestScript.getOne();

        Item shoes = script.teaseLib.items(Clothes.Partner, Clothes.Shoes).get();
        Item shoes2 = script.teaseLib.item(Clothes.Partner, Clothes.Shoes);

        assertEquals(shoes, shoes2);

        Item shoes3 = script.teaseLib.items(Clothes.Doll, Clothes.Shoes).get();
        Item shoes4 = script.teaseLib.item(Clothes.Doll, Clothes.Shoes);

        assertEquals(shoes3, shoes4);

        assertNotEquals(shoes, shoes4);
    }

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

        Item highHeels = script.teaseLib.items(Clothes.Partner, Clothes.Shoes).query(Clothes.Footwear.High_Heels).get();
        highHeels.setAvailable(true);

        assertTrue("Domain item storage unsupported",
                script.persistence.storage.containsKey("Partner.Clothes.high_heels"));
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

        script.teaseLib.items(TeaseLib.DefaultDomain, Clothes.Shoes).query(Clothes.Footwear.High_Heels).get()
                .setAvailable(true);
        assertTrue(script.persistence.storage.containsKey(Clothes.class.getSimpleName() + ".high_heels"));
    }

    @Test
    public void testAssignedToysAndClothingAsItems() {
        TestScript script = TestScript.getOne();

        script.teaseLib.item(Clothes.Partner, Toys.Collar).setAvailable(true);
        assertTrue(script.persistence.storage.containsKey("Partner." + Toys.class.getSimpleName() + ".collar"));

        script.teaseLib.items(Clothes.Doll, Clothes.Shoes).query(Clothes.Footwear.High_Heels).get().setAvailable(true);
        assertTrue(script.persistence.storage.containsKey("Doll." + Clothes.class.getSimpleName() + ".high_heels"));
    }
}
