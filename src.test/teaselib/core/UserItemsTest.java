package teaselib.core;

import static org.junit.Assert.*;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.junit.Test;

import teaselib.Clothes;
import teaselib.Features;
import teaselib.Gadgets;
import teaselib.Household;
import teaselib.Material;
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

            public TestablePredefinedItems(TeaseLib teaseLib) throws IOException {
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

    @Test
    public void testUserItems() throws IOException {
        TestScript script = TestScript.getOne();
        Configuration config = script.teaseLib.config;

        config.set(AbstractUserItems.Settings.ITEM_STORE, getClass().getResource("UserItems.xml").getPath());
        assertTrue(new File(config.get(AbstractUserItems.Settings.ITEM_STORE)).exists());

        UserItems userItems = new PreDefinedItems(script.teaseLib);

        testToys(userItems);
        testHousehold(userItems);
        testClothes(userItems);
        testGadgets(userItems);
    }

    private static void testToys(UserItems userItems) {
        List<Item> humblers = userItems.get(TeaseLib.DefaultDomain, QualifiedItem.of(Toys.Humbler));
        assertEquals(1, humblers.size());

        for (Item humbler : humblers) {
            if (humbler.displayName().equals("My Humbler")) {
                assertTrue(humbler.isAvailable());
                assertTrue(humbler.is(Material.Wood));
                assertTrue(humbler.is(Features.Lockable));
                return;
            }
        }

        fail("XML-Defined item not loaded");
    }

    private static void testHousehold(UserItems userItems) {
        List<Item> spoons = userItems.get(TeaseLib.DefaultDomain, QualifiedItem.of(Household.Wooden_Spoon));
        assertEquals(1, spoons.size());

        for (Item spoon : spoons) {
            if (spoon.displayName().equals("Wooden spoon")) {
                assertFalse(spoon.isAvailable());
                assertTrue(spoon.is(Material.Wood));
                return;
            }
        }

        fail("XML-Defined item not loaded");
    }

    private static void testClothes(UserItems userItems) {
        List<Item> catsuits = userItems.get(TeaseLib.DefaultDomain, QualifiedItem.of(Clothes.Catsuit));
        assertEquals(1, catsuits.size());

        for (Item catsuit : catsuits) {
            if (catsuit.displayName().equals("Catsuit")) {
                assertTrue(catsuit.isAvailable());
                assertTrue(catsuit.is(Material.Rubber));
                return;
            }
        }

        fail("XML-Defined item not loaded");
    }

    private static void testGadgets(UserItems userItems) {
        List<Item> estims = userItems.get(TeaseLib.DefaultDomain, QualifiedItem.of(Gadgets.Computer_Controlled_EStim));
        assertEquals(1, estims.size());

        for (Item estim : estims) {
            if (estim.displayName().equals("EStim")) {
                assertFalse(estim.isAvailable());
                return;
            }
        }

        fail("XML-Defined item not loaded");
    }
}
