package teaselib;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Map;
import java.util.Map.Entry;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import teaselib.core.TeaseLib.PersistentEnum;
import teaselib.test.TestScript;

public class ScriptPersistenceTests {
    private static final Logger logger = LoggerFactory
            .getLogger(ScriptPersistenceTests.class);

    private enum TestValuesEnumClass {
        My_Test_Value_toy,
        My_Test_Value_set_by_name,
        My_Test_Value_set_by_enum,
        My_Test_Value_item_by_name,
        My_Test_Value_item_by_enum,
    }

    @Test
    public void testScriptPersistence() {
        TestScript script = TestScript.getOne();

        script.toy(TestValuesEnumClass.My_Test_Value_toy).setAvailable(true);
        assertTrue(script.persistence.storage
                .containsKey("toys.testvaluesenumclass.my_test_value_toy"));

        script.toy("My Test value toy").setAvailable(true);
        assertTrue(script.persistence.storage
                .containsKey("toys.my test value toy"));

        script.set(TestValuesEnumClass.My_Test_Value_set_by_name.name(),
                "Saved as local enum by name");
        assertTrue(script.persistence.storage.containsKey(
                script.namespace.toLowerCase() + ".my_test_value_set_by_name"));

        script.set(TestValuesEnumClass.My_Test_Value_set_by_enum,
                "Saved by local enum");
        assertTrue(script.persistence.storage
                .containsKey(script.namespace.toLowerCase()
                        + ".testvaluesenumclass.my_test_value_set_by_enum"));

        print(script.persistence.storage);
    }

    @Test
    public void testItemAvailability() {
        TestScript script = TestScript.getOne();

        script.item(TestValuesEnumClass.My_Test_Value_item_by_enum)
                .setAvailable(true);
        assertTrue(script.persistence.storage
                .containsKey(script.namespace.toLowerCase()
                        + ".testvaluesenumclass.my_test_value_item_by_enum"));

        script.item("My Test Value item").setAvailable(true);
        assertTrue(script.persistence.storage.containsKey(
                script.namespace.toLowerCase() + ".my test value item"));

        print(script.persistence.storage);
    }

    @Test
    public void testScriptPersistencenGlobalNamespace() {
        TestScript script = TestScript.getOne();

        script.teaseLib.set("My global namespace",
                TestValuesEnumClass.My_Test_Value_set_by_name.name(),
                "Saved by global name");
        assertTrue(script.persistence.storage
                .containsKey("my global namespace.my_test_value_set_by_name"));

        script.teaseLib.set("My global namespace",
                TestValuesEnumClass.My_Test_Value_set_by_enum,
                "Saved by global enum");
        assertTrue(script.persistence.storage.containsKey(
                "my global namespace.testvaluesenumclass.my_test_value_set_by_enum"));

        script.teaseLib
                .item("My global namespace",
                        TestValuesEnumClass.My_Test_Value_item_by_name.name())
                .setAvailable(true);
        assertTrue(script.persistence.storage
                .containsKey("my global namespace.my_test_value_item_by_name"));

        script.teaseLib
                .item("My global namespace",
                        TestValuesEnumClass.My_Test_Value_item_by_enum)
                .setAvailable(true);
        assertTrue(script.persistence.storage.containsKey(
                "my global namespace.testvaluesenumclass.my_test_value_item_by_enum"));

        print(script.persistence.storage);
    }

    @Test
    public void testPersistentEnum() {
        TestScript script = TestScript.getOne();
        PersistentEnum<TestValuesEnumClass> testValue = script
                .persistentEnum("test", TestValuesEnumClass.values());
        assertFalse(testValue.available());

        for (TestValuesEnumClass value : TestValuesEnumClass.values()) {
            testValue.set(value);
            assertEquals(value, testValue.value());
            print(script.persistence.storage);
        }

        assertTrue(testValue.available());
        testValue.clear();
        assertFalse(testValue.available());

    }

    private static void print(Map<String, String> storage) {
        for (Entry<String, String> entry : storage.entrySet()) {
            logger.info(entry.getKey() + "=" + entry.getValue());
        }
    }

}
