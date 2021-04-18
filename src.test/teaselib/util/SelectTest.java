package teaselib.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static teaselib.util.Select.items;
import static teaselib.util.Select.select;

import org.junit.Test;

import teaselib.Clothes;
import teaselib.Sexuality;
import teaselib.Toys;
import teaselib.test.TestScript;

public class SelectTest {
    TestScript test = TestScript.getOne();

    @Test
    public void testItemQuery() {
        Items.Query items = test.query(Toys.Humbler);

        assertEquals(1, items.get().size());
        assertEquals(0, items.get().getAvailable().size());
        assertFalse(items.get().anyAvailable());

        Items.Query itemsQuery = select(items, Items::getAvailable);
        assertEquals(0, itemsQuery.get().size());
        assertFalse(itemsQuery.get().anyAvailable());
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
        assertEquals(2, test.items(Clothes.Underpants).matching(Sexuality.Gender.Masculine).size());
        assertEquals(3, test.items(Clothes.Underpants).matching(Sexuality.Gender.Feminine)
                .without(Clothes.Category.Swimwear).size());

        Items.Query maleUnderpants = test.select(items(Clothes.Underpants)
                .where(Items::matching, Sexuality.Gender.Masculine).and(Items::without, Clothes.Category.Swimwear));
        assertEquals(2, maleUnderpants.get().size());

        Items.Query femaleUnderpants = test.select(items(Clothes.Underpants)
                .where(Items::matching, Sexuality.Gender.Feminine).and(Items::without, Clothes.Category.Swimwear));
        assertEquals(3, femaleUnderpants.get().size());
    }

    @Test
    public void testMultipleQueries() {
        test.addTestUserItems();

        Select.Statement query1 = new Select.Statement(items(Clothes.Underpants)
                .where(Items::matching, Sexuality.Gender.Masculine).and(Items::without, Clothes.Category.Swimwear));
        assertEquals(2, test.items(query1).size());
        Items.Query maleUnderpants = test.select(query1);
        assertEquals(2, maleUnderpants.get().size());

        Select.Statement query2 = new Select.Statement(items(Clothes.Underpants)
                .where(Items::matching, Sexuality.Gender.Feminine).and(Items::without, Clothes.Category.Swimwear));
        assertEquals(3, test.items(query2).size());
        Items.Query femaleUnderpants = test.select(query2);
        assertEquals(3, femaleUnderpants.get().size());

        Items.Query pants = test.select(query1, query2);
        assertEquals(5, pants.get().size());
    }

    @Test
    public void testValueSelection() {
        Select.Statement query = Clothes.Male.items(Clothes.Shirt, Clothes.Trousers);
        Items attire = test.items(query);
        assertEquals(2, attire.valueSet().size());
    }

    @Test
    public void testStatementPlusStatement() {
        Select.Statement[] query = { //
                Clothes.Male.items(Clothes.Shirt, Clothes.Trousers), //
                Select.items(Toys.Collar) //
        };
        Items attire = test.items(query);
        assertEquals(3, attire.valueSet().size());
    }

}
