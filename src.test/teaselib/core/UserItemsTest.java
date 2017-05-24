package teaselib.core;

import static org.junit.Assert.assertNotEquals;
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

        for (HouseHold item : HouseHold.values()) {
            testItem(script, items, item);
        }

        for (Clothes item : Clothes.values()) {
            testItem(script, items, item);
        }

        for (Toys item : Toys.values()) {
            testItem(script, items, item);
        }
    }

    private static void testItem(TestScript script, UserItems items, Enum<?> item) {
        List<Item> predefined = items.get(script.teaseLib, TeaseLib.DefaultDomain, new QualifiedEnum(item));
        assertNotNull(predefined);
        assertTrue(predefined.size() > 0);
    }

    @Test
    public void testPredefinedItems() throws Exception {

        class TestablePredefinedItems extends PreDefinedItems {
            public Item[] createItems(TeaseLib teaseLib, String domain, QualifiedItem<?> item) {
                return createUserItems(teaseLib, domain, item);
            }

            @Override
            public Item[] onlyTheOriginalItem(TeaseLib teaseLib, String domain, QualifiedItem<?> item) {
                return new Item[] { Item.NotAvailable };
            }

        }

        TestScript script = TestScript.getOne();
        TestablePredefinedItems items = new TestablePredefinedItems();

        for (Toys item : Toys.values()) {
            Item[] predefined = items.createItems(script.teaseLib, TeaseLib.DefaultDomain, new QualifiedEnum(item));
            assertNotNull(predefined);
            assertTrue(predefined.length > 0);
            assertNotEquals("Expected defined item for " + item.name(), Item.NotAvailable, predefined[0]);
        }

    }
}
