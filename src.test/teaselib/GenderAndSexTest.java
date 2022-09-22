package teaselib;

import static org.junit.Assert.*;

import java.io.IOException;

import org.junit.Test;

import teaselib.Sexuality.Gender;
import teaselib.Sexuality.Sex;
import teaselib.core.TeaseLib;
import teaselib.core.TeaseLib.PersistentEnum;
import teaselib.core.util.QualifiedName;
import teaselib.test.TestScript;

public class GenderAndSexTest {

    @Test
    public void testSexualityEnum() throws IOException {
        try (TestScript script = new TestScript()) {
            PersistentEnum<Sex> sex = script.persistence.newEnum(Sex.class);
            assertEquals(Sex.Male, sex.value());
            sex.set(Sex.Female);
            assertEquals(Sex.Female, sex.value());
            assertTrue(script.storage.containsKey(QualifiedName.of(TeaseLib.DefaultDomain, "Sexuality", "Sex")));
            assertEquals(Sex.Female.name(),
                    script.storage.get(QualifiedName.of(TeaseLib.DefaultDomain, "Sexuality", "Sex")));

            PersistentEnum<Gender> gender = script.persistence.newEnum(Gender.class).defaultValue(Gender.Feminine);
            assertEquals(Gender.Feminine, gender.value());
        }
    }

}
