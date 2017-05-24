package teaselib.hosts;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import teaselib.Household;
import teaselib.Toys;
import teaselib.core.util.PropertyNameMapping;

public class SexScriptsPropertyNameMappingTest {
    final PropertyNameMapping m = new SexScriptsPropertyNameMapping();

    @Test
    public void testPathMapping() throws Exception {
        assertEquals("toys",
                m.mapPath("", Household.class.getSimpleName(), Household.Clothes_Pegs.name()));

        assertEquals("toys",
                m.mapPath("", Household.class.getSimpleName(), Household.Heat_Rub.name()));
    }

    @Test
    public void testNameMapping() throws Exception {
        assertEquals("ballgag",
                m.mapName("", Toys.class.getSimpleName(), Toys.Gags.Ball_Gag.name()));

        assertEquals("clothespins",
                m.mapName("", Household.class.getSimpleName(), Household.Clothes_Pegs.name()));

        assertEquals("heat_rub",
                m.mapName("", Household.class.getSimpleName(), Household.Heat_Rub.name()));
    }
}
