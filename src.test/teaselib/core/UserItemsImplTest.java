package teaselib.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Iterator;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.junit.Test;

import teaselib.Body;
import teaselib.Clothes;
import teaselib.Features;
import teaselib.Gadgets;
import teaselib.Household;
import teaselib.Material;
import teaselib.State.Persistence.Until;
import teaselib.Toys;
import teaselib.core.util.QualifiedEnum;
import teaselib.core.util.QualifiedItem;
import teaselib.test.TestScript;
import teaselib.util.Item;

public class UserItemsImplTest {

    @Test
    public void testToyDefaults() throws Exception {
        TestScript script = TestScript.getOne();

        UserItems items = new UserItemsImpl(script.teaseLib);

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
        UserItems items = new UserItemsImpl(script.teaseLib);

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

    private static UserItems configureUserItems(TestScript script) {
        UserItems userItems = new UserItemsImpl(script.teaseLib);
        userItems.addItems(script.getClass().getResource("useritems.xml"));
        return userItems;
    }

    @Test
    public void testUserItemsOverwriteEntry() {
        TestScript script = TestScript.getOne();

        // Default
        Iterator<Item> humblers = script.items(Toys.Humbler).iterator();
        assertFalse(humblers.next().is(Material.Wood));
        assertFalse(humblers.hasNext());

        // Overwrite by user items
        script.addTestUserItems();
        Iterator<Item> humblers1 = script.items(Toys.Humbler).iterator();
        assertTrue(humblers1.next().is(Material.Wood));
        assertFalse(humblers1.hasNext());

        // additional items via custom user items
        script.addTestUserItems2();
        Iterator<Item> humblers2 = script.items(Toys.Humbler).iterator();
        assertTrue(humblers2.next().is(Material.Wood));
        assertTrue(humblers2.next().is(Material.Metal));
        assertFalse(humblers2.hasNext());
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
        List<Item> estimController = userItems.get(TeaseLib.DefaultDomain, QualifiedItem.of(Gadgets.EStim_Controller));
        assertEquals(1, estimController.size());

        for (Item estim : estimController) {
            if (estim.displayName().equals("EStim-Test-Entry")) {
                return;
            }
        }

        fail("XML-Defined item not loaded");
    }

    @Test
    public void testRemoveUserItemUntilExpired() {
        testRemoveUserItem(Until.Expired);
    }

    @Test
    public void testRemoveUserItemUntilRemoved() {
        testRemoveUserItem(Until.Removed);
    }

    private void testRemoveUserItem(Until until) {
        TestScript script = TestScript.getOne();

        // Default
        {
            Iterator<Item> humblers = script.items(Toys.Humbler).iterator();
            assertEquals("Humbler", humblers.next().displayName());
            assertFalse(humblers.hasNext());
        }

        // additional items via custom user items
        {
            script.addTestUserItems2();
            Iterator<Item> humblers = script.items(Toys.Humbler).iterator();
            assertEquals("Humbler", humblers.next().displayName());
            Item myHhumbler = humblers.next();
            assertEquals("My Humbler", myHhumbler.displayName());
            assertFalse(humblers.hasNext());

            myHhumbler.apply().over(1, TimeUnit.HOURS).remember(until);
        }

        script.debugger.clearStateMaps();
        {
            Iterator<Item> humblers = script.items(Toys.Humbler).iterator();
            Item notMyHumber = humblers.next();
            assertFalse(notMyHumber.applied());
            Item persistedHumbler = humblers.next();
            assertTrue(persistedHumbler.applied());
            assertEquals("My Humbler", persistedHumbler.displayName());
            assertFalse(humblers.hasNext());
        }

        // Does not work at runtime as the item disappears, but peers are still applied
        // TODO -> removing toys at runtime may break scripts -> stop if removed item is applied
        script.debugger.resetUserItems();
        script.debugger.clearStateMaps();
        script.addTestUserItems();
        {
            // The following test code shows the wrong state - handled by auto-removing those items
            Iterator<Item> humblers = script.items(Toys.Humbler).iterator();
            Item notMyHumber = humblers.next();
            assertFalse("User items not reset", humblers.hasNext());
            assertTrue(script.state(Toys.Humbler).applied());
            try {
                assertFalse(notMyHumber.applied());
                fail("Removed item shouldn't be accessible anymore");
            } catch (IllegalArgumentException e) {
                // Expected
            }

            try {
                script.item(Toys.Humbler).remove();
                fail("Removed item shouldn't be accessible anymore");
            } catch (IllegalArgumentException e) {
                // Expected
            }

            script.handleAutoRemove();
            assertFalse(script.state(Toys.Humbler).applied());
            assertFalse(script.state(Body.OnBalls).applied());
            assertFalse(notMyHumber.applied());
        }
    }

}
