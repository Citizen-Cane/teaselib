package teaselib;

import static org.junit.Assert.*;

import org.junit.Test;

import teaselib.Sexuality.Gender;
import teaselib.Sexuality.Sex;
import teaselib.core.TeaseLib.PersistentBoolean;
import teaselib.core.TeaseLib.PersistentEnum;
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
        assertTrue(script.persistence.storage.containsKey("Sexuality.Sex"));
        assertEquals(Sex.Female.name(), script.persistence.storage.get("Sexuality.Sex"));

        PersistentEnum<Gender> gender = script.persistentEnum(Gender.class).defaultValue(Gender.Feminine);
        assertEquals(Gender.Feminine, gender.value());
    }

    @Test
    public void testSexScriptsSexMapping() {
        TestScript script = TestScript.getOne(new SexScriptsPropertyNameMapping());

        PersistentEnum<Sex> sex = script.persistentEnum(Sex.class);
        assertFalse(sex.available());

        script.persistence.storage.put("intro.female", "true");
        assertTrue(sex.available());
        assertEquals(Sex.Female, sex.value());

        script.persistence.storage.put("intro.female", "false");
        assertTrue(sex.available());
        assertEquals(Sex.Male, sex.value());

        script.persistence.storage.remove("intro.female");
        assertFalse(sex.available());
    }

    @Test
    public void testSexScriptsSexualOrientationMappingAndMappingOfNestedEnums() {
        TestScript script = TestScript.getOne(new SexScriptsPropertyNameMapping());

        PersistentBoolean likesMales = script.persistentBoolean(Sexuality.Orientation.LikesMales);
        assertFalse(likesMales.available());
        assertEquals(false, likesMales.value());

        script.persistence.storage.put("intro.likemale", "true");
        assertTrue(likesMales.available());
        assertEquals(true, likesMales.value());

        likesMales.set(false);
        assertEquals("false", script.persistence.storage.get("intro.likemale"));

        PersistentBoolean likesFemales = script.persistentBoolean(Sexuality.Orientation.LikesFemales);
        assertFalse(likesFemales.available());
        assertEquals(false, likesFemales.value());

        script.persistence.storage.put("intro.likefemale", "true");
        assertTrue(likesFemales.available());
        assertEquals(true, likesFemales.value());

        likesFemales.set(false);
        assertEquals("false", script.persistence.storage.get("intro.likefemale"));
    }
}
