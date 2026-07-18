package teaselib.host.sexscripts;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
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

import java.io.IOException;

public class SexScriptsPropertyNameMappingTest {

    @Test
    public void testPathMapping() throws Exception {
        PropertyNameMapping m = new SexScriptsPropertyNameMapping();

        String household = Household.class.getSimpleName();
        Assertions.assertEquals("toys", m.map(QualifiedName.of("", household, Household.Clothes_Pegs.name())).namespace);
        Assertions.assertEquals("toys", m.map(QualifiedName.of("", household, Household.Heat_Rub.name())).namespace);
    }

    @Test
    public void testNameMapping() throws Exception {
        PropertyNameMapping m = new SexScriptsPropertyNameMapping();

        String toys = Toys.class.getSimpleName();
        Assertions.assertEquals("ballgag", m.map(QualifiedName.of("", toys, Toys.Gags.Ball_Gag.name())).name);
        Assertions.assertEquals("cockring", m.map(QualifiedName.of("", toys, Toys.Cock_Ring.name())).name);
        Assertions.assertEquals("estim", m.map(QualifiedName.of("", toys, Toys.EStim_Device.name())).name);

        String household = Household.class.getSimpleName();
        Assertions.assertEquals("cigarette", m.map(QualifiedName.of("", household, Household.Cigarettes.name())).name);
        Assertions.assertEquals("clothespins", m.map(QualifiedName.of("", household, Household.Clothes_Pegs.name())).name);
        Assertions.assertEquals("tampon", m.map(QualifiedName.of("", household, Household.Tampons.name())).name);
    }

    @Test
    public void testInventoryMapping() throws Exception {
        PropertyNameMapping m = new SexScriptsPropertyNameMapping();
        Assertions.assertEquals("toys.ring_gag", m.map(QualifiedName.of(TeaseLib.DefaultDomain, "Toys.Gag", "ring_gag.Available")).toString());
    }

    @Test
    public void testSexScriptsSexMapping_Read() throws IOException {
        try (TestScript script = new TestScript(new SexScriptsPropertyNameMapping())) {
            PersistentEnum<Sex> sex = script.persistence.newEnum(Sex.class);
            Assertions.assertFalse(sex.available());

            script.storage.put(SexScriptsPropertyNameMapping.INTRO_FEMALE, "true");

            Assertions.assertEquals(Sex.Female, sex.value());
            Assertions.assertTrue(sex.available());

            script.storage.put(SexScriptsPropertyNameMapping.INTRO_FEMALE, "false");
            Assertions.assertEquals(Sex.Male, sex.value());
            Assertions.assertTrue(sex.available());

            script.storage.remove(QualifiedName.of(TeaseLib.DefaultDomain, "intro", "female"));
            Assertions.assertFalse(sex.available());
        }
    }

    @Test
    public void testSexScriptsSexMapping_Write() throws IOException {
        try (TestScript script = new TestScript(new SexScriptsPropertyNameMapping())) {
            PersistentEnum<Sex> sex = script.persistence.newEnum(Sex.class);
            Assertions.assertFalse(sex.available());
            Assertions.assertFalse(script.storage.containsKey(SexScriptsPropertyNameMapping.INTRO_FEMALE));

            sex.set(Sex.Female);
            Assertions.assertEquals("true", script.storage.get(SexScriptsPropertyNameMapping.INTRO_FEMALE));
            Assertions.assertTrue(script.storage.containsKey(SexScriptsPropertyNameMapping.INTRO_FEMALE));

            sex.set(Sex.Male);
            Assertions.assertEquals("false", script.storage.get(SexScriptsPropertyNameMapping.INTRO_FEMALE));

            sex.clear();
            Assertions.assertFalse(script.storage.containsKey(SexScriptsPropertyNameMapping.INTRO_FEMALE));
        }
    }

    @Test
    public void testSexScriptsSexualOrientationMappingAndMappingOfNestedEnums_Read() throws IOException {
        try (TestScript script = new TestScript(new SexScriptsPropertyNameMapping())) {
            PersistentBoolean likesMales = script.persistence.newBoolean(Sexuality.Orientation.LikesMales);
            Assertions.assertFalse(likesMales.available());
            Assertions.assertEquals(false, likesMales.value());

            script.storage.put(new QualifiedName("", "intro", "likemale"), "true");
            Assertions.assertTrue(likesMales.available());
            Assertions.assertEquals(true, likesMales.value());

            likesMales.set(false);
            Assertions.assertEquals("false", script.storage.get(QualifiedName.of(TeaseLib.DefaultDomain, "intro", "likemale")));

            PersistentBoolean likesFemales = script.persistence.newBoolean(Sexuality.Orientation.LikesFemales);
            Assertions.assertFalse(likesFemales.available());
            Assertions.assertEquals(false, likesFemales.value());

            script.storage.put(new QualifiedName("", "intro", "likefemale"), "true");
            Assertions.assertTrue(likesFemales.available());
            Assertions.assertEquals(true, likesFemales.value());
        }
    }

    @Test
    public void testSexScriptsSexualOrientationMappingAndMappingOfNestedEnums_Write() throws IOException {
        try (TestScript script = new TestScript(new SexScriptsPropertyNameMapping())) {
            PersistentBoolean likesMales = script.persistence.newBoolean(Sexuality.Orientation.LikesMales);
            likesMales.set(true);
            Assertions.assertEquals("true", script.storage.get(new QualifiedName("", "intro", "likemale")));

            PersistentBoolean likesFemales = script.persistence.newBoolean(Sexuality.Orientation.LikesFemales);
            likesFemales.set(true);
            Assertions.assertEquals("true", script.storage.get(new QualifiedName("", "intro", "likefemale")));

            likesFemales.set(false);
            Assertions.assertEquals("false", script.storage.get(QualifiedName.of(TeaseLib.DefaultDomain, "intro", "likefemale")));

            likesFemales.clear();
            Assertions.assertFalse(script.storage.containsKey(QualifiedName.of(TeaseLib.DefaultDomain, "intro", "likefemale")));
        }

    }
}
