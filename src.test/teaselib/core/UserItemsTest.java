package teaselib.core;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.junit.Test;

import teaselib.Clothes;
import teaselib.HouseHold;
import teaselib.Toys;
import teaselib.core.util.QualifiedEnum;
import teaselib.core.util.QualifiedItem;
import teaselib.hosts.PreDefinedItems;
import teaselib.test.TestScript;
import teaselib.util.Item;

public class UserItemsTest {

    @Test
    public void testToyDefaults() throws Exception {
        UserItems items = new AbstractUserItems() {

            @Override
            protected Item[] createUserItems(TeaseLib teaseLib, String domain, QualifiedItem<?> item) {
                throw new UnsupportedOperationException();
            }
        };

        for (Clothes item : Clothes.values()) {
            assertNotNull(items.defaults(new QualifiedEnum(item)));
        }

        for (HouseHold item : HouseHold.values()) {
            assertNotNull(items.defaults(new QualifiedEnum(item)));
        }

        for (Toys item : Toys.values()) {
            assertNotNull(items.defaults(new QualifiedEnum(item)));
        }
    }

    @Test
    public void testToyItems() throws Exception {
        TestScript script = TestScript.getOne();
        UserItems items = new PreDefinedItems();

        for (Toys item : Toys.values()) {
            testItem(script, items, item);
        }

        for (Toys item : Toys.values()) {
            testItem(script, items, item);
        }

        for (Toys item : Toys.values()) {
            testItem(script, items, item);
        }
    }

    private static void testItem(TestScript script, UserItems items, Toys item) {
        List<Item> available = items.get(script.teaseLib, TeaseLib.DefaultDomain, new QualifiedEnum(item));
        assertNotNull(available);
        assertTrue(available.size() > 0);
    }
}
