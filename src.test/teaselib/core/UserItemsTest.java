package teaselib.core;

import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.junit.Test;

import teaselib.Clothes;
import teaselib.Household;
import teaselib.Toys;
import teaselib.core.util.QualifiedEnum;
import teaselib.core.util.QualifiedItem;
import teaselib.hosts.PreDefinedItems;
import teaselib.test.TestScript;
import teaselib.util.Item;

public class UserItemsTest {

    @Test
    public void testToyDefaults() throws Exception {
        TestScript script = TestScript.getOne();

        UserItems items = new AbstractUserItems(script.teaseLib) {
            @Override
            protected Item[] createUserItems(String domain, QualifiedItem<?> item) {
                throw new UnsupportedOperationException();
            }
        };

        for (Clothes item : Clothes.values()) {
            assertNotNull(items.defaults(new QualifiedEnum(item)));
        }

        for (Household item : Household.values()) {
            assertNotNull(items.defaults(new QualifiedEnum(item)));
        }

        for (Toys item : Toys.values()) {
            assertNotNull(items.defaults(new QualifiedEnum(item)));
        }
    }

    @Test
    public void testToyItems() throws Exception {
        TestScript script = TestScript.getOne();
        UserItems items = new PreDefinedItems(script.teaseLib);

        for (Household item : Household.values()) {
            testItem(items, item);
        }

        for (Clothes item : Clothes.values()) {
            testItem(items, item);
        }

        for (Toys item : Toys.values()) {
            testItem(items, item);
        }
    }

    private static void testItem(UserItems items, Enum<?> item) {
        List<Item> predefined = items.get(TeaseLib.DefaultDomain, new QualifiedEnum(item));
        assertNotNull(predefined);
        assertTrue(predefined.size() > 0);
    }

    @Test
    public void testPredefinedItems() throws Exception {

        class TestablePredefinedItems extends PreDefinedItems {

            public TestablePredefinedItems(TeaseLib teaseLib) {
                super(teaseLib);
            }

            public Item[] createItems(String domain, QualifiedItem<?> item) {
                return createUserItems(domain, item);
            }

            @Override
            public Item[] getDefaultItem(String domain, QualifiedItem<?> item) {
                return new Item[] { Item.NotAvailable };
            }

        }

        TestScript script = TestScript.getOne();
        TestablePredefinedItems items = new TestablePredefinedItems(script.teaseLib);

        for (Toys item : Toys.values()) {
            Item[] predefined = items.createItems(TeaseLib.DefaultDomain, new QualifiedEnum(item));
            assertNotNull(predefined);
            assertTrue(predefined.length > 0);
            assertNotEquals("Expected defined item for " + item.name(), Item.NotAvailable, predefined[0]);
        }

    }
}
