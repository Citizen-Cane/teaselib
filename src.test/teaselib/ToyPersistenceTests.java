/**
 * 
 */
package teaselib;

import static org.junit.Assert.*;

import org.junit.Test;

import teaselib.core.TeaseLib;
import teaselib.core.util.QualifiedName;
import teaselib.test.TestScript;
import teaselib.util.Item;
import teaselib.util.Items;

/**
 * @author Citizen-Cane
 *
 */
public class ToyPersistenceTests {
    @Test
    public void testDomainSeparation() {
        TestScript script = TestScript.getOne();

        Item shoes = script.domain(Clothes.Partner).items(Shoes.All).get();
        Item shoes2 = script.domain(Clothes.Partner).items(Shoes.All).get();
        assertEquals(shoes, shoes2);

        Item shoes3 = script.domain(Clothes.Doll).items(Shoes.All).get();
        Item shoes4 = script.domain(Clothes.Doll).items(Shoes.All).get();
        assertEquals(shoes3, shoes4);

        assertNotEquals(shoes, shoes4);
    }

    @Test
    public void testToysAndClothing() {
        TestScript script = TestScript.getOne();

        Item gag = script.defaultDomain.item(Toys.Gag);
        assertTrue(gag.is(Toys.Gags.Ball_Gag));

        gag.setAvailable(true);
        assertTrue(script.storage.containsKey(QualifiedName.of(TeaseLib.DefaultDomain, "Toys", "ball_gag")));

        Items shoes = script.domain(Clothes.Partner).items(Shoes.Feminine);
        Item highHeels = shoes.matching(Shoes.High_Heels).get();
        highHeels.setAvailable(true);

        assertTrue("Domain item storage unsupported",
                script.storage.containsKey(QualifiedName.of("Partner", "Shoes", "high_heels")));
    }

    @Test
    public void testToysAndClothingAsItems() {
        TestScript script = TestScript.getOne();

        Item gag = script.defaultDomain.item(Toys.Gag);
        assertTrue(gag.is(Toys.Gags.Ball_Gag));

        gag.setAvailable(true);
        assertTrue(script.storage
                .containsKey(QualifiedName.of(TeaseLib.DefaultDomain, Toys.class.getSimpleName(), "ball_gag")));

        script.defaultDomain.items(Shoes.Feminine).matching(Shoes.High_Heels).get().setAvailable(true);
        assertTrue(script.storage
                .containsKey(QualifiedName.of(TeaseLib.DefaultDomain, Shoes.class.getSimpleName(), "high_heels")));
    }

    @Test
    public void testAssignedToysAndClothingAsItems() {
        TestScript script = TestScript.getOne();

        script.domain(Clothes.Partner).item(Toys.Collar).setAvailable(true);
        assertTrue(script.storage.containsKey(QualifiedName.of(Clothes.Partner, Toys.class.getSimpleName(), "collar")));

        script.domain(Clothes.Doll).items(Shoes.Feminine).matching(Shoes.High_Heels).get().setAvailable(true);
        assertTrue(
                script.storage.containsKey(QualifiedName.of(Clothes.Doll, Shoes.class.getSimpleName(), "high_heels")));
    }
}
