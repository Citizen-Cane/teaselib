package teaselib;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.Optional;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import teaselib.core.TeaseLib;
import teaselib.core.TeaseLib.PersistentEnum;
import teaselib.core.configuration.ConfigurationFile;
import teaselib.core.util.QualifiedName;
import teaselib.core.util.ReflectionUtils;
import teaselib.test.TestScript;
import teaselib.util.Item;

public class ScriptPersistenceTests {
    private static final Logger logger = LoggerFactory.getLogger(ScriptPersistenceTests.class);

    private enum TestValuesEnumClass {
        My_Test_Value_toy,
        My_Test_Value_set_by_name,
        My_Test_Value_set_by_enum,
        My_Test_Value_item_by_name,
        My_Test_Value_item_by_enum,
    }

    @Test
    public void testScriptPersistenceStringItemStorage() {
        TestScript script = TestScript.getOne();
        script.item("My test namespace.My Test value toy").setAvailable(true);
        QualifiedName key = QualifiedName.of(TeaseLib.DefaultDomain, "My test namespace.My Test value toy",
                "My Test value toy.Available");
        assertTrue(script.storage.containsKey(key));
    }

    @Test
    public void testScriptPersistenceEnumItemStorage() {
        TestScript script = TestScript.getOne();
        script.item(TestValuesEnumClass.My_Test_Value_toy).setAvailable(true);
        QualifiedName key = QualifiedName.of(TeaseLib.DefaultDomain,
                ReflectionUtils.qualified(TestValuesEnumClass.My_Test_Value_toy.getClass(),
                        TestValuesEnumClass.My_Test_Value_toy.name()),
                TestValuesEnumClass.My_Test_Value_toy.name() + ".Available");
        assertTrue(script.storage.containsKey(key));
    }

    @Test
    public void testScriptNamespacePersistence() {
        TestScript script = TestScript.getOne();
        assertNull(script.getString(TestValuesEnumClass.My_Test_Value_set_by_name.name()));

        script.set(TestValuesEnumClass.My_Test_Value_set_by_name.name(), "Saved as local enum by name");
        Optional<ConfigurationFile> scriptSettings = script.teaseLib.config.getUserSettings(script.namespace);
        assertTrue(scriptSettings.isPresent());
        assertTrue(scriptSettings.get().has(
                QualifiedName.of(TeaseLib.DefaultDomain, script.namespace, "My_Test_Value_set_by_name").toString()));
        assertEquals("Saved as local enum by name",
                script.getString(TestValuesEnumClass.My_Test_Value_set_by_name.name()));

    }

    @Test
    public void testGlobalPersistence() {
        TestScript script = TestScript.getOne();
        assertNull(script.getString(TestValuesEnumClass.My_Test_Value_set_by_enum));

        script.set(TestValuesEnumClass.My_Test_Value_set_by_enum, "Saved by local enum");
        QualifiedName expected = QualifiedName.of(TeaseLib.DefaultDomain, "ScriptPersistenceTests.TestValuesEnumClass",
                "My_Test_Value_set_by_enum");
        assertTrue(script.storage.containsKey(expected));
    }

    @Test
    public void testItemAvailability() {
        TestScript script = TestScript.getOne();

        script.item(TestValuesEnumClass.My_Test_Value_item_by_enum).setAvailable(true);
        assertTrue(script.storage.containsKey(QualifiedName.of(TeaseLib.DefaultDomain,
                "ScriptPersistenceTests.TestValuesEnumClass.My_Test_Value_item_by_enum",
                "My_Test_Value_item_by_enum.Available")));

        script.item("My test namespace.My Test Value item").setAvailable(true);
        assertTrue(script.storage.containsKey(QualifiedName.of(TeaseLib.DefaultDomain,
                "My test namespace.My Test Value item", "My Test Value item.Available")));
    }

    @Test
    public void testDomainSeparation() {
        TestScript script = TestScript.getOne();

        script.item(Clothes.Underpants).setAvailable(true);
        assertTrue(script.item(Clothes.Underpants).isAvailable());
        // TODO Support domain names for user items
        assertFalse("Item domains not separated", script.domain(Clothes.Doll).item(Clothes.Underpants).isAvailable());

        script.domain(Clothes.Doll).item(Clothes.Underpants).setAvailable(true);
        assertTrue(script.domain(Clothes.Doll).item(Clothes.Underpants).isAvailable());

        assertEquals(2, script.storage.size());
    }

    @Test
    public void testSetVersusItem() {
        TestScript script = TestScript.getOne();

        QualifiedName qualified = QualifiedName.of(TeaseLib.DefaultDomain, //
                "ScriptPersistenceTests.TestValuesEnumClass.My_Test_Value_item_by_enum", //
                "My_Test_Value_item_by_enum.Available");
        Item item = script.item(TestValuesEnumClass.My_Test_Value_item_by_enum);
        assertEquals(0, script.storage.size());

        item.setAvailable(true);
        assertEquals(1, script.storage.size());
        assertEquals(true, item.isAvailable());
        assertEquals(true, script.storage.containsKey(qualified));
        TeaseLib.PersistentBoolean persistentBoolean = script.teaseLib.new PersistentBoolean("",
                "ScriptPersistenceTests.TestValuesEnumClass.My_Test_Value_item_by_enum",
                "My_Test_Value_item_by_enum.Available");

        // state is:
        // Domain:ScriptPersistenceTests.TestValuesEnumClass.My_Test_Value_item_by_enum/state.applied=true
        //
        // TODO state should be
        // "Domain:ScriptPersistenceTests.TestValuesEnumClass/My_Test_Value_item_by_enum.state.xxxx" as well
        // as before -> split qualified item in state impl
        // makes more sense because states can be grouped by item class
        // - on the other hand, items are referenced as "Toys.Gag", not "Toys/Gag" - needs splitting and introduces
        // namespace confusion
        // -> this is why it's split this way
        //
        // so the inventory should be the same as state without splitting the namespace
        // "Domain:ScriptPersistenceTests.TestValuesEnumClass.My_Test_Value_item_by_enum/Inventory.Available" as well
        // By using Inventory path element, it's easier to add additional information groups

        assertEquals(true, persistentBoolean.value());

        item.setAvailable(false);
        assertEquals(1, script.storage.size());
        assertEquals(false, item.isAvailable());
        assertEquals(false, persistentBoolean.value());
    }

    @Test
    public void testScriptPersistencenGlobalNamespace() {
        TestScript script = TestScript.getOne();

        script.teaseLib.set(TeaseLib.DefaultDomain, "My namespace",
                TestValuesEnumClass.My_Test_Value_set_by_name.name(), "Saved by global name");
        assertTrue(script.storage
                .containsKey(QualifiedName.of(TeaseLib.DefaultDomain, "My namespace", "My_Test_Value_set_by_name")));

        script.teaseLib.set("My domain", TestValuesEnumClass.My_Test_Value_set_by_enum, "Saved by global enum");
        assertTrue(script.storage.containsKey(QualifiedName.of("My domain",
                "ScriptPersistenceTests.TestValuesEnumClass", "My_Test_Value_set_by_enum")));

        script.teaseLib
                .item(TeaseLib.DefaultDomain,
                        "My namespace" + "." + TestValuesEnumClass.My_Test_Value_item_by_name.name())
                .setAvailable(true);
        assertTrue(script.storage.containsKey(QualifiedName.of(TeaseLib.DefaultDomain,
                "My namespace.My_Test_Value_item_by_name", "My_Test_Value_item_by_name.Available")));

        script.teaseLib.item(TeaseLib.DefaultDomain, TestValuesEnumClass.My_Test_Value_item_by_enum).setAvailable(true);
        assertTrue(script.storage.containsKey(QualifiedName.of(TeaseLib.DefaultDomain,
                "ScriptPersistenceTests.TestValuesEnumClass.My_Test_Value_item_by_enum",
                "My_Test_Value_item_by_enum.Available")));
    }

    @Test
    public void testPersistentEnum() {
        TestScript script = TestScript.getOne();

        PersistentEnum<TestValuesEnumClass> testValue = script.persistentEnum("test", TestValuesEnumClass.class);
        assertFalse(testValue.available());

        for (TestValuesEnumClass value : TestValuesEnumClass.values()) {
            testValue.set(value);
            assertEquals(value, testValue.value());
            script.storage.printTo(logger);
        }

        assertTrue(testValue.available());
        testValue.clear();
        assertFalse(testValue.available());
    }
}
