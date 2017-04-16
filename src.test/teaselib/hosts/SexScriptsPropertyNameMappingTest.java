package teaselib.hosts;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import teaselib.HouseHold;
import teaselib.Toys;
import teaselib.core.util.PropertyNameMapping;

public class SexScriptsPropertyNameMappingTest {
    final PropertyNameMapping m = new SexScriptsPropertyNameMapping();

    @Test
    public void testPathMapping() throws Exception {
        assertEquals("toys",
                m.mapPath("", HouseHold.class.getSimpleName(), HouseHold.Clothes_Pegs.name()));

        assertEquals("toys",
                m.mapPath("", HouseHold.class.getSimpleName(), HouseHold.Heat_Rub.name()));
    }

    @Test
    public void testNameMapping() throws Exception {
        assertEquals("ballgag",
                m.mapName("", Toys.class.getSimpleName(), Toys.Gags.Ball_Gag.name()));

        assertEquals("clothespins",
                m.mapName("", HouseHold.class.getSimpleName(), HouseHold.Clothes_Pegs.name()));

        assertEquals("heat_rub",
                m.mapName("", HouseHold.class.getSimpleName(), HouseHold.Heat_Rub.name()));
    }
}
