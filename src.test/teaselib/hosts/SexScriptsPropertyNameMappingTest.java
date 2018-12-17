package teaselib.hosts;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import teaselib.Household;
import teaselib.Toys;
import teaselib.core.debug.DebugPersistence;
import teaselib.core.util.PropertyNameMapping;

public class SexScriptsPropertyNameMappingTest {
    private final PropertyNameMapping m = new SexScriptsPropertyNameMapping(new DebugPersistence());

    @Test
    public void testPathMapping() throws Exception {
        assertEquals("toys", m.mapPath("", Household.class.getSimpleName(), Household.Clothes_Pegs.name()));

        assertEquals("toys", m.mapPath("", Household.class.getSimpleName(), Household.Heat_Rub.name()));
    }

    @Test
    public void testNameMapping() throws Exception {
        assertEquals("ballgag", m.mapName("", Toys.class.getSimpleName(), Toys.Gags.Ball_Gag.name()));
        assertEquals("cockring", m.mapName("", Toys.class.getSimpleName(), Toys.Cock_Ring.name()));
        assertEquals("estim", m.mapName("", Toys.class.getSimpleName(), Toys.EStim_Device.name()));

        assertEquals("cigarette", m.mapName("", Household.class.getSimpleName(), Household.Cigarettes.name()));
        assertEquals("clothespins", m.mapName("", Household.class.getSimpleName(), Household.Clothes_Pegs.name()));
        assertEquals("tampon", m.mapName("", Household.class.getSimpleName(), Household.Tampons.name()));
    }
}
