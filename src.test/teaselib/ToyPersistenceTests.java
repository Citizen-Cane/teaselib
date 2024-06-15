/**
 *
 */
package teaselib;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import teaselib.core.util.QualifiedName;
import teaselib.core.util.QualifiedString;
import teaselib.test.TestScript;
import teaselib.util.Item;
import teaselib.util.Items;

import java.io.IOException;

import static teaselib.core.TeaseLib.DefaultDomain;

/**
 * @author Citizen-Cane
 *
 */
public class ToyPersistenceTests {
    @Test
    public void testDomainSeparation() throws IOException {
        try (TestScript script = new TestScript()) {

            Item shoes = script.domain(Clothes.Partner).items(Shoes.High_Heels).inventory().get();
            Item shoes2 = script.domain(Clothes.Partner).items(Shoes.High_Heels).inventory().get();
            Assertions.assertNotEquals(Item.NotFound, shoes);
            Assertions.assertNotEquals(Item.NotFound, shoes2);
            Assertions.assertEquals(shoes, shoes2);

            Item shoes3 = script.domain(Clothes.Doll).items(Shoes.High_Heels).inventory().get();
            Item shoes4 = script.domain(Clothes.Doll).items(Shoes.High_Heels).inventory().get();
            Assertions.assertNotEquals(Item.NotFound, shoes3);
            Assertions.assertNotEquals(Item.NotFound, shoes4);
            Assertions.assertEquals(shoes3, shoes4);

            Assertions.assertNotEquals(shoes, shoes4);
        }
    }

    @Test
    public void testToysAndClothing() throws IOException {
        try (TestScript script = new TestScript()) {

            Item gag = script.defaultDomain.items(Toys.Gag).matching(Toys.Gags.Ball_Gag).item();
            Assertions.assertTrue(gag.is(Toys.Gags.Ball_Gag));

            gag.setAvailable(true);
            Assertions.assertTrue(script.storage.containsKey(QualifiedName.of(DefaultDomain, "Toys.Gag", "ball_gag.Available")));

            var shoes = script.domain(Clothes.Partner).items(Shoes.Feminine);
            Item highHeels = shoes.matching(Shoes.High_Heels).inventory().get();
            highHeels.setAvailable(true);
            Assertions.assertTrue(script.storage
                    .containsKey(QualifiedName.of("Partner", "Shoes.High_Heels", "high_heels.Available")));
        }
    }

    @Test
    public void testToysAndClothingAsItems() throws IOException {
        try (TestScript script = new TestScript()) {

            Item gag = script.defaultDomain.items(Toys.Gag).matching(Toys.Gags.Ball_Gag).item();
            Assertions.assertTrue(gag.is(Toys.Gags.Ball_Gag));

            gag.setAvailable(true);
            Assertions.assertTrue(script.storage.containsKey(
                    QualifiedName.of(DefaultDomain, QualifiedString.of(Toys.Gag).toString(), "ball_gag.Available")));

            script.defaultDomain.items(Shoes.Feminine.where(Items::matching, Shoes.High_Heels)).inventory().get()
                    .setAvailable(true);
            Assertions.assertTrue(script.storage.containsKey(QualifiedName.of(DefaultDomain,
                    QualifiedString.of(Shoes.High_Heels).toString(), "high_heels.Available")));
        }
    }

    @Test
    public void testAssignedToysAndClothingAsItems() throws IOException {
        try (TestScript script = new TestScript()) {

            script.domain(Clothes.Partner).item(Toys.Collar).setAvailable(true);
            Assertions.assertTrue(script.storage.containsKey(
                    QualifiedName.of(Clothes.Partner, QualifiedString.of(Toys.Collar).toString(), "collar.Available")));

            Item item = script.domain(Clothes.Doll).items(Shoes.Feminine.where(Items::matching, Shoes.High_Heels))
                    .inventory().get();
            Assertions.assertNotEquals(Item.NotFound, item);
            Assertions.assertTrue(item.is(Shoes.High_Heels));

            item.setAvailable(true);
            Item availableShoe = script.domain(Clothes.Doll).items(Shoes.Feminine).matching(Shoes.High_Heels).item();
            Assertions.assertTrue(availableShoe.is(Shoes.High_Heels));

            Item availableShoe2 = script.domain(Clothes.Doll)
                    .items(Shoes.Feminine.where(Items::matching, Shoes.High_Heels)).item();
            Assertions.assertTrue(availableShoe2.is(Shoes.High_Heels));

            Item availableShoe3 = script.domain(Clothes.Doll).items(Shoes.Feminine).matching(Shoes.High_Heels).item();
            Assertions.assertTrue(availableShoe3.is(Shoes.High_Heels));

            Assertions.assertTrue(script.storage.containsKey(QualifiedName.of(Clothes.Doll,
                    QualifiedString.of(Shoes.High_Heels).toString(), "high_heels.Available")));
        }
    }
}
