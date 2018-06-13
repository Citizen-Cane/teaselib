package teaselib.util;

import static org.junit.Assert.*;

import org.junit.Test;

import teaselib.Features;
import teaselib.Material;
import teaselib.Toys;
import teaselib.test.TestScript;
import teaselib.util.math.Varieties;

public class ItemsTest {
    @Test
    public void testVarieties() {
        TestScript script = TestScript.getOne();
        Items inventory = script.items(Toys.Collar, Toys.Ankle_Restraints, Toys.Wrist_Restraints, Toys.Chains);
        Varieties<Items> varieties = inventory.prefer(Features.Lockable, Material.Leather).varieties();
        Items restraints = varieties.reduce(Items::best);
        assertEquals(4, restraints.size());

        Item collar = restraints.item(Toys.Collar);
        Item anklecuffs = restraints.item(Toys.Ankle_Restraints);
        Item wristCuffs = restraints.item(Toys.Wrist_Restraints);
        Item chains = restraints.item(Toys.Chains);

        assertNotEquals(Item.NotFound, collar);
        assertNotEquals(Item.NotFound, anklecuffs);
        assertNotEquals(Item.NotFound, wristCuffs);
        assertNotEquals(Item.NotFound, chains);
    }
}
