package teaselib.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.util.List;

import org.junit.Test;

import teaselib.Clothes;
import teaselib.Features;
import teaselib.Gadgets;
import teaselib.Household;
import teaselib.Material;
import teaselib.Toys;
import teaselib.core.Host.Location;
import teaselib.core.util.QualifiedEnum;
import teaselib.core.util.QualifiedItem;
import teaselib.hosts.PreDefinedItems;
import teaselib.test.TestScript;
import teaselib.util.Item;

public class AbstractUserItemsTest {

    @Test
    public void testToyDefaults() throws Exception {
        TestScript script = TestScript.getOne();

        UserItems items = new AbstractUserItems(script.teaseLib) {
            @Override
            protected Item[] createDefaultItems(String domain, QualifiedItem item) {
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
    public void testDefaultItems() throws Exception {
        TestScript script = TestScript.getOne();
        UserItems items = configureUserItems(script);

        for (Toys item : Toys.values()) {
            List<Item> predefined = items.get(TeaseLib.DefaultDomain, new QualifiedEnum(item));
            assertNotNull(predefined);
            assertTrue(predefined.size() > 0);
            assertNotEquals("Expected defined item for " + item.name(), Item.NotFound, predefined.get(0));
        }
    }

    @Test
    public void testUserItems() {
        TestScript script = TestScript.getOne();
        UserItems userItems = configureUserItems(script);

        testToys(userItems);
        testHousehold(userItems);
        testClothes(userItems);
        testGadgets(userItems);
    }

    private UserItems configureUserItems(TestScript script) {
        configurePaths(script);

        UserItems userItems = new PreDefinedItems(script.teaseLib);
        return userItems;
    }

    private void configurePaths(TestScript script) {
        Configuration config = script.teaseLib.config;

        config.set(AbstractUserItems.Settings.ITEM_DEFAULT_STORE,
                new File(script.host.getLocation(Location.TeaseLib), "defaults/" + "items.xml").getPath());
        assertTrue(new File(config.get(AbstractUserItems.Settings.ITEM_DEFAULT_STORE)).exists());

        config.set(AbstractUserItems.Settings.ITEM_USER_STORE, getClass().getResource("useritems.xml").getPath());
        assertTrue(new File(config.get(AbstractUserItems.Settings.ITEM_USER_STORE)).exists());
    }

    @Test
    public void testUserItemsOverwriteEntry() {
        TestScript script = TestScript.getOne();
        configurePaths(script);

        // Overwrite by user items
        assertEquals(1, script.items(Toys.Humbler).size());
        assertTrue(script.item(Toys.Humbler).is(Material.Wood));

        // additional items via custom user items
        script.teaseLib
                .addUserItems(new File(ResourceLoader.getProjectPath(getClass()), "teaselib/core/useritems2.xml"));
        assertEquals(2, script.items(Toys.Humbler).size());
        for (Item item : script.items(Toys.Humbler)) {
            assertTrue(item.is(Material.Wood));
        }
    }

    private static void testToys(UserItems userItems) {
        testToys(userItems, "My Humbler");
    }

    private static void testToys(UserItems userItems, String displayName) {
        List<Item> humblers = userItems.get(TeaseLib.DefaultDomain, QualifiedItem.of(Toys.Humbler));
        assertEquals(1, humblers.size());

        for (Item humbler : humblers) {
            if (humbler.displayName().equals(displayName)) {
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
                assertTrue(catsuit.is(Material.Rubber));
                return;
            }
        }

        fail("XML-Defined item not loaded");
    }

    private static void testGadgets(UserItems userItems) {
        List<Item> estims = userItems.get(TeaseLib.DefaultDomain, QualifiedItem.of(Gadgets.EStim_Controller));
        assertEquals(1, estims.size());

        for (Item estim : estims) {
            if (estim.displayName().equals("Teaselib-controlled estim device")) {
                return;
            }
        }

        fail("XML-Defined item not loaded");
    }
}
