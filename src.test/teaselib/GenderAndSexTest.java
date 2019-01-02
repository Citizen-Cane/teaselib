package teaselib;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import teaselib.Sexuality.Gender;
import teaselib.Sexuality.Sex;
import teaselib.core.TeaseLib;
import teaselib.core.TeaseLib.PersistentEnum;
import teaselib.core.util.QualifiedName;
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

}
