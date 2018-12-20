package teaselib;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import teaselib.Sexuality.Gender;
import teaselib.Sexuality.Sex;
import teaselib.core.TeaseLib;
import teaselib.core.TeaseLib.PersistentBoolean;
import teaselib.core.TeaseLib.PersistentEnum;
import teaselib.core.util.QualifiedName;
import teaselib.hosts.SexScriptsPropertyNameMapping;
import teaselib.test.TestScript;

public class GenderAndSexTest {

    @Test
    public void testSexualityEnum() {
        TestScript script = TestScript.getOne();

        PersistentEnum<Sex> sex = script.persistentEnum(Sex.class);
        assertEquals(Sex.Male, sex.value());
        sex.set(Sex.Female);
        assertEquals(Sex.Female, sex.value());
        assertTrue(
                script.persistence.storage.containsKey(QualifiedName.of(TeaseLib.DefaultDomain, "Sexuality", "Sex")));
        assertEquals(Sex.Female.name(),
                script.persistence.storage.get(QualifiedName.of(TeaseLib.DefaultDomain, "Sexuality", "Sex")));

        PersistentEnum<Gender> gender = script.persistentEnum(Gender.class).defaultValue(Gender.Feminine);
        assertEquals(Gender.Feminine, gender.value());
    }

    @Test
    public void testSexScriptsSexMapping() {
        TestScript script = TestScript.getOne(SexScriptsPropertyNameMapping::new);

        PersistentEnum<Sex> sex = script.persistentEnum(Sex.class);
        assertFalse(sex.available());

        script.persistence.storage.put(SexScriptsPropertyNameMapping.INTRO_FEMALE, "true");
        assertTrue(sex.available());
        assertEquals(Sex.Female, sex.value());

        script.persistence.storage.put(SexScriptsPropertyNameMapping.INTRO_FEMALE, "false");
        assertTrue(sex.available());
        assertEquals(Sex.Male, sex.value());

        script.persistence.storage.remove(QualifiedName.of(TeaseLib.DefaultDomain, "intro", "female"));
        assertFalse(sex.available());
    }

    @Test
    public void testSexScriptsSexualOrientationMappingAndMappingOfNestedEnums() {
        TestScript script = TestScript.getOne(SexScriptsPropertyNameMapping::new);

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
