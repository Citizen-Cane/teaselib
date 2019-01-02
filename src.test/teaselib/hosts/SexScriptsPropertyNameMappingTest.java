package teaselib.hosts;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

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
    public void testSexScriptsSexMapping_Read() {
        TestScript script = TestScript.getOne(new SexScriptsPropertyNameMapping());

        PersistentEnum<Sex> sex = script.persistentEnum(Sex.class);
        assertFalse(sex.available());

        script.persistence.storage.put(SexScriptsPropertyNameMapping.INTRO_FEMALE, "true");

        assertEquals(Sex.Female, sex.value());
        assertTrue(sex.available());

        script.persistence.storage.put(SexScriptsPropertyNameMapping.INTRO_FEMALE, "false");
        assertEquals(Sex.Male, sex.value());
        assertTrue(sex.available());

        script.persistence.storage.remove(QualifiedName.of(TeaseLib.DefaultDomain, "intro", "female"));
        assertFalse(sex.available());
    }

    @Test
    public void testSexScriptsSexMapping_Write() {
        TestScript script = TestScript.getOne(new SexScriptsPropertyNameMapping());

        PersistentEnum<Sex> sex = script.persistentEnum(Sex.class);
        assertFalse(sex.available());
        assertFalse(script.persistence.storage.containsKey(SexScriptsPropertyNameMapping.INTRO_FEMALE));

        sex.set(Sex.Female);
        assertEquals("true", script.persistence.storage.get(SexScriptsPropertyNameMapping.INTRO_FEMALE));
        assertTrue(script.persistence.storage.containsKey(SexScriptsPropertyNameMapping.INTRO_FEMALE));

        sex.set(Sex.Male);
        assertEquals("false", script.persistence.storage.get(SexScriptsPropertyNameMapping.INTRO_FEMALE));

        sex.clear();
        assertFalse(script.persistence.storage.containsKey(SexScriptsPropertyNameMapping.INTRO_FEMALE));
    }

    @Test
    public void testSexScriptsSexualOrientationMappingAndMappingOfNestedEnums() {
        TestScript script = TestScript.getOne(new SexScriptsPropertyNameMapping());

        PersistentBoolean likesMales = script.persistentBoolean(Sexuality.Orientation.LikesMales);
        assertFalse(likesMales.available());
        assertEquals(false, likesMales.value());

        script.persistence.storage.put(new QualifiedName("", "intro", "likemale"), "true");
        assertTrue(likesMales.available());
        assertEquals(true, likesMales.value());

        likesMales.set(false);
        assertEquals("false",
                script.persistence.storage.get(QualifiedName.of(TeaseLib.DefaultDomain, "intro", "likemale")));

        PersistentBoolean likesFemales = script.persistentBoolean(Sexuality.Orientation.LikesFemales);
        assertFalse(likesFemales.available());
        assertEquals(false, likesFemales.value());

        script.persistence.storage.put(new QualifiedName("", "intro", "likefemale"), "true");
        assertTrue(likesFemales.available());
        assertEquals(true, likesFemales.value());

        likesFemales.set(false);
        assertEquals("false",
                script.persistence.storage.get(QualifiedName.of(TeaseLib.DefaultDomain, "intro", "likefemale")));
    }

}
