package teaselib;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import teaselib.Sexuality.Gender;
import teaselib.Sexuality.Sex;
import teaselib.core.TeaseLib;
import teaselib.core.TeaseLib.PersistentEnum;
import teaselib.core.util.QualifiedName;
import teaselib.test.TestScript;

import java.io.IOException;

public class GenderAndSexTest {

    @Test
    public void testSexualityEnum() throws IOException {
        try (TestScript script = new TestScript()) {
            PersistentEnum<Sex> sex = script.persistence.newEnum(Sex.class);
            Assertions.assertEquals(Sex.Male, sex.value());
            sex.set(Sex.Female);
            Assertions.assertEquals(Sex.Female, sex.value());
            Assertions.assertTrue(script.storage.containsKey(QualifiedName.of(TeaseLib.DefaultDomain, "Sexuality", "Sex")));
            Assertions.assertEquals(Sex.Female.name(), script.storage.get(QualifiedName.of(TeaseLib.DefaultDomain, "Sexuality", "Sex")));

            PersistentEnum<Gender> gender = script.persistence.newEnum(Gender.class).defaultValue(Gender.Feminine);
            Assertions.assertEquals(Gender.Feminine, gender.value());
        }
    }

}
