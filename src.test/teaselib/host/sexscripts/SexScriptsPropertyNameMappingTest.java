package teaselib.host.sexscripts;

import static org.junit.Assert.*;

import java.io.IOException;

import org.junit.Test;

import teaselib.Household;
import teaselib.Sexuality;
import teaselib.Sexuality.Sex;
import teaselib.Toys;
import teaselib.core.TeaseLib;
import teaselib.core.TeaseLib.PersistentBoolean;
import teaselib.core.TeaseLib.PersistentEnum;
import teaselib.core.util.PropertyNameMapping;
import teaselib.core.util.QualifiedName;
import teaselib.test.TestScript;

public class SexScriptsPropertyNameMappingTest {

    @Test
    public void testPathMapping() throws Exception {
        PropertyNameMapping m = new SexScriptsPropertyNameMapping();

        String household = Household.class.getSimpleName();
        assertEquals("toys", m.map(QualifiedName.of("", household, Household.Clothes_Pegs.name())).namespace);
        assertEquals("toys", m.map(QualifiedName.of("", household, Household.Heat_Rub.name())).namespace);
    }

    @Test
    public void testNameMapping() throws Exception {
        PropertyNameMapping m = new SexScriptsPropertyNameMapping();

        String toys = Toys.class.getSimpleName();
        assertEquals("ballgag", m.map(QualifiedName.of("", toys, Toys.Gags.Ball_Gag.name())).name);
        assertEquals("cockring", m.map(QualifiedName.of("", toys, Toys.Cock_Ring.name())).name);
        assertEquals("estim", m.map(QualifiedName.of("", toys, Toys.EStim_Device.name())).name);

        String household = Household.class.getSimpleName();
        assertEquals("cigarette", m.map(QualifiedName.of("", household, Household.Cigarettes.name())).name);
        assertEquals("clothespins", m.map(QualifiedName.of("", household, Household.Clothes_Pegs.name())).name);
        assertEquals("tampon", m.map(QualifiedName.of("", household, Household.Tampons.name())).name);
    }

    @Test
    public void testInventoryMapping() throws Exception {
        PropertyNameMapping m = new SexScriptsPropertyNameMapping();
        assertEquals("toys.ring_gag",
                m.map(QualifiedName.of(TeaseLib.DefaultDomain, "Toys.Gag", "ring_gag.Available")).toString());
    }

    @Test
    public void testSexScriptsSexMapping_Read() throws IOException {
        try (TestScript script = new TestScript(new SexScriptsPropertyNameMapping())) {
            PersistentEnum<Sex> sex = script.persistence.newEnum(Sex.class);
            assertFalse(sex.available());

            script.storage.put(SexScriptsPropertyNameMapping.INTRO_FEMALE, "true");

            assertEquals(Sex.Female, sex.value());
            assertTrue(sex.available());

            script.storage.put(SexScriptsPropertyNameMapping.INTRO_FEMALE, "false");
            assertEquals(Sex.Male, sex.value());
            assertTrue(sex.available());

            script.storage.remove(QualifiedName.of(TeaseLib.DefaultDomain, "intro", "female"));
            assertFalse(sex.available());
        }
    }

    @Test
    public void testSexScriptsSexMapping_Write() throws IOException {
        try (TestScript script = new TestScript(new SexScriptsPropertyNameMapping())) {
            PersistentEnum<Sex> sex = script.persistence.newEnum(Sex.class);
            assertFalse(sex.available());
            assertFalse(script.storage.containsKey(SexScriptsPropertyNameMapping.INTRO_FEMALE));

            sex.set(Sex.Female);
            assertEquals("true", script.storage.get(SexScriptsPropertyNameMapping.INTRO_FEMALE));
            assertTrue(script.storage.containsKey(SexScriptsPropertyNameMapping.INTRO_FEMALE));

            sex.set(Sex.Male);
            assertEquals("false", script.storage.get(SexScriptsPropertyNameMapping.INTRO_FEMALE));

            sex.clear();
            assertFalse(script.storage.containsKey(SexScriptsPropertyNameMapping.INTRO_FEMALE));
        }
    }

    @Test
    public void testSexScriptsSexualOrientationMappingAndMappingOfNestedEnums_Read() throws IOException {
        try (TestScript script = new TestScript(new SexScriptsPropertyNameMapping())) {
            PersistentBoolean likesMales = script.persistence.newBoolean(Sexuality.Orientation.LikesMales);
            assertFalse(likesMales.available());
            assertEquals(false, likesMales.value());

            script.storage.put(new QualifiedName("", "intro", "likemale"), "true");
            assertTrue(likesMales.available());
            assertEquals(true, likesMales.value());

            likesMales.set(false);
            assertEquals("false", script.storage.get(QualifiedName.of(TeaseLib.DefaultDomain, "intro", "likemale")));

            PersistentBoolean likesFemales = script.persistence.newBoolean(Sexuality.Orientation.LikesFemales);
            assertFalse(likesFemales.available());
            assertEquals(false, likesFemales.value());

            script.storage.put(new QualifiedName("", "intro", "likefemale"), "true");
            assertTrue(likesFemales.available());
            assertEquals(true, likesFemales.value());
        }
    }

    @Test
    public void testSexScriptsSexualOrientationMappingAndMappingOfNestedEnums_Write() throws IOException {
        try (TestScript script = new TestScript(new SexScriptsPropertyNameMapping())) {
            PersistentBoolean likesMales = script.persistence.newBoolean(Sexuality.Orientation.LikesMales);
            likesMales.set(true);
            assertEquals("true", script.storage.get(new QualifiedName("", "intro", "likemale")));

            PersistentBoolean likesFemales = script.persistence.newBoolean(Sexuality.Orientation.LikesFemales);
            likesFemales.set(true);
            assertEquals("true", script.storage.get(new QualifiedName("", "intro", "likefemale")));

            likesFemales.set(false);
            assertEquals("false", script.storage.get(QualifiedName.of(TeaseLib.DefaultDomain, "intro", "likefemale")));

            likesFemales.clear();
            assertFalse(script.storage.containsKey(QualifiedName.of(TeaseLib.DefaultDomain, "intro", "likefemale")));
        }

    }
}
