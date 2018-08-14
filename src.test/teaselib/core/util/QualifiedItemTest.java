package teaselib.core.util;

import static org.junit.Assert.*;

import org.junit.Test;

import teaselib.Material;
import teaselib.core.Script;
import teaselib.core.TeaseLib;
import teaselib.core.devices.release.ReleaseActionTest.TestReleaseActionState;
import teaselib.test.TestScript;
import teaselib.util.Item;
import teaselib.util.ItemImpl;

public class QualifiedItemTest {
    enum TestEnum {
        One;
    }

    @Test
    public void testEnum() {
        TestEnum testEnum = TestEnum.One;
        QualifiedItem one = QualifiedItem.of(testEnum);
        assertTrue(one instanceof QualifiedEnum);
        assertEquals("teaselib.core.util.QualifiedItemTest.TestEnum", one.namespace());
        assertEquals("One", one.name());
    }

    @Test
    public void testString() {
        String testString = "teaselib.core.util.QualifiedItemTest.TestEnum.One";
        QualifiedItem one = QualifiedItem.of(testString);
        assertTrue(one instanceof QualifiedObject);
        assertEquals("teaselib.core.util.QualifiedItemTest.TestEnum", one.namespace());
        assertEquals("One", one.name());
    }

    @Test
    public void testItem() {
        TestScript testScript = TestScript.getOne();
        Item testItem = testScript.item("teaselib.core.util.QualifiedItemTest.TestEnum.One");
        QualifiedItem one = QualifiedItem.of(testItem);

        assertTrue(one instanceof QualifiedItemImpl);
        assertEquals("teaselib.core.util.QualifiedItemTest.TestEnum", one.namespace());
        assertEquals("One", one.name());
    }

    @Test
    public void testIdentity() {
        TestScript testScript = TestScript.getOne();

        Item metal = testScript.item(Material.Metal);
        Item metalString = testScript.item("teaselib.Material.Metal");

        assertEquals(metal, metalString);

        Item one = testScript.item(TestEnum.One);
        Item oneString = testScript.item("teaselib.core.util.QualifiedItemTest.TestEnum.One");

        assertEquals(one, oneString);

        Item one1 = createOne(testScript, "Test_One_1");
        Item one2 = createOne(testScript, "Test_One_2");

        assertNotEquals(one1, one);
        assertNotEquals(one1, oneString);
        assertNotEquals(one1, one2);
        assertNotEquals(QualifiedItem.of(one1), QualifiedItem.of(one2));
    }

    private static ItemImpl createOne(Script script, String name) {
        return new ItemImpl(script.teaseLib, TestEnum.One, TeaseLib.DefaultDomain, name, "A_Number");
    }

    @Test
    public void testEquality() {
        TestScript script = TestScript.getOne();

        String devicePath = "KeyRelease/MyPhoton/1";

        String expected = ReflectionUtils.qualified(TestReleaseActionState.class, devicePath);
        QualifiedItem actual = QualifiedItem.of(script.state(expected));
        assertNotSame(expected, actual);

        // Works but bad coding practice
        assertEquals(actual, expected);
        assertEquals(expected, actual.toString());

        // Doesn't work this way because String can't be overloaded
        assertNotEquals(expected, actual);

        // Always wrap strings into qualified items when dealing with state in the core
        assertEquals(QualifiedItem.of(expected), actual);
    }
}
