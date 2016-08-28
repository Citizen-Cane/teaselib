package teaselib;

import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;

import teaselib.core.ResourceLoader;
import teaselib.core.texttospeech.Voice;
import teaselib.hosts.DummyHost;
import teaselib.hosts.DummyPersistence;

public class ScriptPersistenceTests {
    final DummyPersistence persistence = new DummyPersistence();
    final TeaseLib teaseLib = new TeaseLib(new DummyHost(), persistence);
    final String namespace = "JUnit test";
    final TeaseScript script = new TeaseScript(teaseLib,
            new ResourceLoader(PersistenceTest.class),
            new Actor("Test", Voice.Gender.Female, "en-us"), namespace) {
        @Override
        public void run() {
            throw new UnsupportedOperationException();
        }
    };

    private enum TestValues {
        My_Test_Value_toy,
        My_Test_Value_set_by_name,
        My_Test_Value_set_by_enum,
        My_Test_Value_item_by_name,
        My_Test_Value_item_by_enum,
    }

    @Before
    public void before() {
        persistence.storage.clear();
    }

    @Test
    public void testScriptPersistence() {
        script.toy(TestValues.My_Test_Value_toy).setAvailable(true);
        assertTrue(persistence.storage
                .containsKey("toys.testvalues.my_test_value_toy"));

        script.toy("My Test value toy").setAvailable(true);
        assertTrue(persistence.storage.containsKey("toys.my test value toy"));

        script.set(TestValues.My_Test_Value_set_by_name.name(),
                "Saved as local enum by name");
        assertTrue(persistence.storage.containsKey(
                namespace.toLowerCase() + ".my_test_value_set_by_name"));

        script.set(TestValues.My_Test_Value_set_by_enum, "Saved by local enum");
        assertTrue(persistence.storage.containsKey(namespace.toLowerCase()
                + ".testvalues.my_test_value_set_by_enum"));
    }

    @Test
    public void testScriptPersistence2() {
        script.item(TestValues.My_Test_Value_item_by_enum).setAvailable(true);
        assertTrue(persistence.storage.containsKey(namespace.toLowerCase()
                + ".testvalues.my_test_value_item_by_enum"));

        script.item("My Test Value item").setAvailable(true);
        assertTrue(persistence.storage
                .containsKey(namespace.toLowerCase() + ".my test value item"));
    }

    @Test
    public void testScriptPersistenceGlobalNamespace() {
        teaseLib.set("My global namespace",
                TestValues.My_Test_Value_set_by_name.name(),
                "Saved by global name");
        assertTrue(persistence.storage
                .containsKey("my global namespace.my_test_value_set_by_name"));

        teaseLib.set("My global namespace",
                TestValues.My_Test_Value_set_by_enum, "Saved by global enum");
        assertTrue(persistence.storage.containsKey(
                "my global namespace.testvalues.my_test_value_set_by_enum"));

        teaseLib.item("My global namespace",
                TestValues.My_Test_Value_item_by_name.name())
                .setAvailable(true);
        assertTrue(persistence.storage
                .containsKey("my global namespace.my_test_value_item_by_name"));

        teaseLib.item("My global namespace",
                TestValues.My_Test_Value_item_by_enum).setAvailable(true);
        assertTrue(persistence.storage.containsKey(
                "my global namespace.testvalues.my_test_value_item_by_enum"));
    }
}
