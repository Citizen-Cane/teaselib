package teaselib.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static teaselib.util.Select.*;

import java.io.IOException;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import teaselib.Body;
import teaselib.Bondage;
import teaselib.Clothes;
import teaselib.Sexuality;
import teaselib.Shoes;
import teaselib.Toys;
import teaselib.core.ItemsImpl;
import teaselib.core.util.QualifiedString;
import teaselib.test.TestScript;

public class SelectTest {

    TestScript test;

    @Before
    public void setup() throws IOException {
        test = new TestScript();
    }

    @After
    public void cleanup() {
        test.close();
    }

    @Test
    public void testItemQuery() {
        Items.Query items = test.items(Toys.Humbler);

        assertEquals(1, items.inventory().size());
        assertEquals(0, items.inventory().getAvailable().size());
        assertFalse(items.anyAvailable());

        Items.Query itemsQuery = select(items, Items::getAvailable);
        assertEquals(0, itemsQuery.inventory().size());
        assertFalse(itemsQuery.anyAvailable());

        items.inventory().stream().forEach(item -> item.setAvailable(true));

        Items.Query available = select(items);
        assertEquals(1, available.inventory().size());
        assertTrue(itemsQuery.anyAvailable());
    }

    @Test
    public void testStatementItems() {
        Select.Statement all = items(Shoes.All.values, Clothes.All.values);
        assertTrue(test.items(all).inventory().size() > 0);

        Select.Statement shoes = items(Shoes.All.values);
        assertTrue(test.items(shoes).inventory().size() > 0);
    }

    @Test
    public void testStateQuery() {
        States.Query states = test.states(Toys.Humbler);
        assertEquals(1, states.get().size());

        States.Query applied = Select.select(states, States::applied);
        assertEquals(0, applied.get().size());

        test.state(Toys.Humbler).apply();
        assertEquals(1, applied.get().size());
    }

    @Test
    public void testWhereQuery() {
        test.addTestUserItems();
        assertEquals(2, test.items(Clothes.Underpants).matching(Sexuality.Gender.Masculine).inventory().size());
        assertEquals(3, test.items(Clothes.Underpants).matching(Sexuality.Gender.Feminine)
                .without(Clothes.Category.Swimwear).inventory().size());

        Items.Query maleUnderpants = test.items(items(Clothes.Underpants)
                .where(Items::matching, Sexuality.Gender.Masculine).and(Items::without, Clothes.Category.Swimwear));
        assertEquals(2, maleUnderpants.inventory().size());

        Items.Query femaleUnderpants = test.items(items(Clothes.Underpants)
                .where(Items::matching, Sexuality.Gender.Feminine).and(Items::without, Clothes.Category.Swimwear));
        assertEquals(3, femaleUnderpants.inventory().size());
    }

    @Test
    public void testMultipleQueries() {
        test.addTestUserItems();

        Select.Statement query1 = new Select.Statement(items(Clothes.Underpants)
                .where(Items::matching, Sexuality.Gender.Masculine).and(Items::without, Clothes.Category.Swimwear));
        assertEquals(2, test.items(query1).inventory().size());
        Items.Query maleUnderpants = test.items(query1);
        assertEquals(2, maleUnderpants.inventory().size());

        Select.Statement query2 = new Select.Statement(items(Clothes.Underpants)
                .where(Items::matching, Sexuality.Gender.Feminine).and(Items::without, Clothes.Category.Swimwear));
        assertEquals(3, test.items(query2).inventory().size());
        Items.Query femaleUnderpants = test.items(query2);
        assertEquals(3, femaleUnderpants.inventory().size());

        Items.Query pants = test.items(query1, query2);
        assertEquals(5, pants.inventory().size());
        assertEquals(0, pants.inventory().getAvailable().size());

        maleUnderpants.inventory().get().setAvailable(true);
        femaleUnderpants.inventory().get().setAvailable(true);
        assertEquals(2, pants.inventory().getAvailable().size());
    }

    @Test
    public void testValueSelection() {
        Select.Statement query = Clothes.Male.items(Clothes.Shirt, Clothes.Trousers);
        Items attire = test.items(query).inventory();
        assertEquals(2, elementSet(attire).size());
    }

    Select.Statement[] attire = { //
            Clothes.Male.items(Clothes.Shirt, Clothes.Trousers), //
            Select.items(Toys.Collar) //
    };

    @Test
    public void testStatementPlusStatement() {
        assertEquals(5, test.items(Clothes.Male.items(Clothes.Shirt, Clothes.Trousers)).inventory().size());
        assertEquals(1, test.items(Select.items(Toys.Collar)).inventory().size());
        assertEquals(6, test.items(attire).inventory().size());
    }

    @Test
    public void testStatementPlusStatementValueSet() {
        assertEquals(2, elementSet(test.items(Clothes.Male.items(Clothes.Shirt, Clothes.Trousers)).inventory()).size());
        assertEquals(1, elementSet(test.items(Select.items(Toys.Collar)).inventory()).size());
        assertEquals(3, elementSet(test.items(attire).inventory()).size());
    }

    private static java.util.Set<QualifiedString> elementSet(Items items) {
        return ((ItemsImpl) items).elementSet();
    }

    @Test
    public void testPredefinedStatementArray() {
        assertEquals(1, test.items(Bondage.Anklets).inventory().size());
        assertEquals(1, test.items(Bondage.Wristlets).inventory().size());
        assertEquals(1, test.items(Bondage.Ankle_Cuffs).inventory().size());
        assertEquals(1, test.items(Bondage.Wrist_Cuffs).inventory().size());

        assertEquals(2, test.items(Bondage.Cuffs).inventory().size());
        assertEquals(2, test.items(Bondage.Cufflets).inventory().size());

        assertEquals(4, test.items(Bondage.Restraints).inventory().size());
    }

    @Test
    public void testStatementApplied() {
        var plugs = Select.items(Toys.Buttplug, Toys.Dildo);
        var analDildo = plugs.where(Items::matching, Body.InButt).and(Items::getApplied);

        assertTrue(test.items(analDildo).inventory().isEmpty());
        assertTrue(test.items(analDildo).noneApplied());
        assertFalse(test.items(analDildo).anyApplied());
        assertFalse(test.items(analDildo).allApplied());

        test.setAvailable(plugs);
        assertTrue(test.items(analDildo).noneApplied());

        test.items(plugs).getAvailable().items(Toys.Dildo).to(Body.InVagina).get().apply();
        assertTrue(test.items(analDildo).noneApplied());
        assertFalse(test.items(analDildo).anyApplied());
        assertFalse(test.items(analDildo).allApplied());

        test.items(plugs).getAvailable().items(Toys.Dildo).to(Body.InButt).get().apply();
        assertEquals(3, test.items(plugs).getAvailable().size());
        assertEquals(1, test.items(analDildo).getAvailable().size());
        assertTrue(test.items(analDildo).anyApplied());
        assertTrue(test.items(analDildo).allApplied());

        assertTrue(test.items(plugs).noneApplicable());
        assertFalse(test.items(plugs).anyApplicable());
        assertFalse(test.items(plugs).allApplicable());

    }

}
