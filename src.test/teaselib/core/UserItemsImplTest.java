package teaselib.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.junit.Test;

import teaselib.Clothes;
import teaselib.Features;
import teaselib.Gadgets;
import teaselib.Household;
import teaselib.Material;
import teaselib.State;
import teaselib.State.Persistence.Until;
import teaselib.Toys;
import teaselib.core.util.QualifiedString;
import teaselib.test.TestScript;
import teaselib.util.Item;

public class UserItemsImplTest {

    @Test
    public void testToyDefaults() throws IOException {
        try (TestScript script = new TestScript()) {
            UserItems items = new UserItemsImpl(script.teaseLib);

            for (Clothes item : Clothes.values()) {
                assertNotNull(items.defaults(QualifiedString.of(item)));
            }

            for (Household item : Household.values()) {
                assertNotNull(items.defaults(QualifiedString.of(item)));
            }

            for (Toys item : Toys.values()) {
                assertNotNull(items.defaults(QualifiedString.of(item)));
            }
        }
    }

    @Test
    public void testToyItems() throws IOException {
        try (TestScript script = new TestScript()) {
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
    }

    private static void testItem(UserItems items, Enum<?> item) {
        List<Item> predefined = items.get(TeaseLib.DefaultDomain, QualifiedString.of(item));
        assertNotNull(predefined);
        assertFalse(predefined.isEmpty());
    }

    @Test
    public void testDefaultItems() throws Exception {
        try (TestScript script = new TestScript()) {
            UserItems items = configureUserItems(script);

            for (Toys item : Toys.values()) {
                List<Item> predefined = items.get(TeaseLib.DefaultDomain, QualifiedString.of(item));
                assertNotNull(predefined);
                assertFalse(predefined.isEmpty());
                assertNotEquals("Expected defined item for " + item.name(), Item.NotFound, predefined.get(0));
            }
        }
    }

    @Test
    public void testUserItems() throws IOException {
        try (TestScript script = new TestScript()) {
            UserItems userItems = configureUserItems(script);

            testToys(userItems);
            testHousehold(userItems);
            testClothes(userItems);
            testGadgets(userItems);
        }
    }

    private static UserItems configureUserItems(TestScript script) throws IOException {
        UserItems userItems = new UserItemsImpl(script.teaseLib);
        userItems.addItems(script.getClass().getResource("useritems.xml"));
        return userItems;
    }

    @Test
    public void testUserItemsOverwriteEntry() throws IOException {
        try (TestScript script = new TestScript()) {
            // Default
            Iterator<Item> humblers = script.items(Toys.Humbler).inventory().iterator();
            assertFalse(humblers.next().is(Material.Wood));
            assertFalse(humblers.hasNext());

            // Overwrite by user items
            script.addTestUserItems();
            Iterator<Item> humblers1 = script.items(Toys.Humbler).inventory().iterator();
            assertTrue(humblers1.next().is(Material.Wood));
            assertFalse(humblers1.hasNext());

            // additional items via custom user items
            script.addTestUserItems2();
            Iterator<Item> humblers2 = script.items(Toys.Humbler).inventory().iterator();
            assertTrue(humblers2.next().is(Material.Wood));
            assertTrue(humblers2.next().is(Material.Metal));
            assertFalse(humblers2.hasNext());
        }
    }

    private static void testToys(UserItems userItems) {
        testToys(userItems, "My Humbler");
    }

    private static void testToys(UserItems userItems, String displayName) {
        List<Item> humblers = userItems.get(TeaseLib.DefaultDomain, QualifiedString.of(Toys.Humbler));
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
        List<Item> spoons = userItems.get(TeaseLib.DefaultDomain, QualifiedString.of(Household.Wooden_Spoon));
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
        List<Item> catsuits = userItems.get(TeaseLib.DefaultDomain, QualifiedString.of(Clothes.Catsuit));
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
        List<Item> estimController = userItems.get(TeaseLib.DefaultDomain,
                QualifiedString.of(Gadgets.EStim_Controller));
        assertEquals(1, estimController.size());

        for (Item estim : estimController) {
            if (estim.displayName().equals("EStim-Test-Entry")) {
                return;
            }
        }

        fail("XML-Defined item not loaded");
    }

    @Test
    public void testRemoveUserItemUntilExpired() throws IOException {
        testRemoveUserItem(Until.Expired);
    }

    @Test
    public void testRemoveUserItemUntilRemoved() throws IOException {
        testRemoveUserItem(Until.Removed);
    }

    private static void testRemoveUserItem(Until until) throws IOException {
        try (TestScript script = new TestScript()) {
            // Default
            {
                Iterator<Item> humblers = script.items(Toys.Humbler).inventory().iterator();
                assertEquals("Humbler", humblers.next().displayName());
                assertFalse(humblers.hasNext());
            }

            // additional items via custom user items
            {
                script.addTestUserItems2();
                Iterator<Item> humblers = script.items(Toys.Humbler).inventory().iterator();
                Item notMyHumbler = humblers.next();
                assertEquals("Humbler", notMyHumbler.displayName());

                Item myHumbler = humblers.next();
                assertEquals("My Humbler", myHumbler.displayName());
                assertFalse(humblers.hasNext());

                myHumbler.apply().over(1, TimeUnit.HOURS).remember(until);
                assertTrue(script.state(Toys.Humbler).is(until));
                assertTrue(myHumbler.is(until));
                assertFalse(notMyHumbler.is(until));
            }

            script.debugger.clearStateMaps();
            {
                Iterator<Item> humblers = script.items(Toys.Humbler).inventory().iterator();
                Item notMyHumbler = humblers.next();
                assertFalse(notMyHumbler.applied());

                Item myHumbler = humblers.next();
                assertEquals("My Humbler", myHumbler.displayName());
                assertTrue(myHumbler.applied());
                assertTrue(script.state(Toys.Humbler).is(until));
                assertTrue(myHumbler.is(until));
                assertFalse(humblers.hasNext());

                assertTrue(script.state(Toys.Humbler).is(until));
                assertFalse(notMyHumbler.is(until));
            }

            script.debugger.resetUserItems();
            script.debugger.clearStateMaps();
            script.addTestUserItems();
            {
                Iterator<Item> humblers = script.items(Toys.Humbler).inventory().iterator();
                Item notMyHumbler = humblers.next();
                // assertEquals("Humbler", notMyHumbler.displayName());
                assertFalse("User items not reset", humblers.hasNext());

                // With the persisted item removed the state is still applied
                State state = script.state(Toys.Humbler);
                assertTrue(state.applied());
                assertTrue(state.is(until));
                assertFalse(notMyHumbler.applied());
                assertFalse(notMyHumbler.is(until));
            }

            script.debugger.clearStateMaps();
            {
                script.handleAutoRemove();
                // remove state with reference to unavailable guid
                State state = script.state(Toys.Humbler);
                assertFalse(state.is(until));
                assertFalse(state.applied());

                Item item = script.item(Toys.Humbler);
                assertFalse(item.applied());
                assertFalse(item.is(until));
            }
        }
    }

}
