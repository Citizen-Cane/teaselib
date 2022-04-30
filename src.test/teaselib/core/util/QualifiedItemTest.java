package teaselib.core.util;

import static org.junit.Assert.*;

import java.io.IOException;

import org.junit.Test;

import teaselib.Material;
import teaselib.Toys;
import teaselib.core.ItemImpl;
import teaselib.core.Script;
import teaselib.core.TeaseLib;
import teaselib.test.TestScript;
import teaselib.util.Item;

public class QualifiedItemTest {

    @Test
    public void testEquals() {
        QualifiedString itemFromEnum = QualifiedString.of(Toys.Dildo);
        assertEquals(itemFromEnum, itemFromEnum);

        QualifiedString itemFromString = QualifiedString.of("teaselib.Toys.Dildo");
        assertEquals(itemFromEnum, itemFromString);
        assertEquals(itemFromEnum, QualifiedString.of(Toys.Dildo));
        assertEquals(itemFromEnum, QualifiedString.of("teaselib.Toys.Dildo"));

        assertNotEquals(itemFromEnum, QualifiedString.of(Toys.Buttplug));
        assertNotEquals(itemFromEnum, QualifiedString.of("teaselib.Toys.Buttplug"));

        assertEquals(itemFromString, itemFromString);
        assertEquals(itemFromString, itemFromEnum);
        assertEquals(itemFromString, QualifiedString.of(Toys.Dildo));
        assertEquals(itemFromString, QualifiedString.of("teaselib.Toys.Dildo"));

        assertNotEquals(itemFromString, QualifiedString.of("teaselib.Toys.Buttplug"));
        assertNotEquals(itemFromString, QualifiedString.of(Toys.Buttplug));
    }

    @Test
    public void testIs() {
        QualifiedString itemFromEnum = QualifiedString.of(Toys.Dildo);
        assertEquals(itemFromEnum, itemFromEnum);

        QualifiedString itemFromString = QualifiedString.of("teaselib.Toys.Dildo");
        assertEquals(itemFromEnum, itemFromString);
        assertTrue(itemFromEnum.is(Toys.Dildo));
        assertTrue(itemFromEnum.is("teaselib.Toys.Dildo"));

        assertFalse(itemFromEnum.is(Toys.Buttplug));
        assertFalse(itemFromEnum.is("teaselib.Toys.Buttplug"));

        assertEquals(itemFromString, itemFromString);
        assertEquals(itemFromString, itemFromEnum);
        assertTrue(itemFromEnum.is(Toys.Dildo));
        assertTrue(itemFromEnum.is("teaselib.Toys.Dildo"));

        assertFalse(itemFromString.is("teaselib.Toys.Buttplug"));
        assertFalse(itemFromString.is(Toys.Buttplug));
    }

    @Test
    public void testToString() throws Exception {
        assertEquals("teaselib.Toys.Dildo", QualifiedString.of(Toys.Dildo).toString());
        assertEquals("teaselib.Toys.Dildo", QualifiedString.of("teaselib.Toys.Dildo").toString());
    }

    @Test
    public void testQualifiedEnumNullConstructorArgument() throws Exception {
        assertThrows(NullPointerException.class, () -> new QualifiedString(null));
    }

    enum TestEnum {
        One;
    }

    @Test
    public void testEnum() {
        TestEnum testEnum = TestEnum.One;
        QualifiedString one = QualifiedString.of(testEnum);
        assertEquals("teaselib.core.util.QualifiedItemTest.TestEnum", one.namespace());
        assertEquals("One", one.name());
    }

    @Test
    public void testString() {
        String testString = "teaselib.core.util.QualifiedItemTest.TestEnum.One";
        QualifiedString one = QualifiedString.of(testString);
        assertEquals("teaselib.core.util.QualifiedItemTest.TestEnum", one.namespace());
        assertEquals("One", one.name());
        assertEquals("teaselib.core.util.QualifiedItemTest.TestEnum.One", one.toString());
    }

    @Test
    public void testStringGuid() {
        String testString = "teaselib.core.util.QualifiedItemTest.TestEnum.One#myGuid";
        QualifiedString one = QualifiedString.of(testString);
        assertEquals("teaselib.core.util.QualifiedItemTest.TestEnum", one.namespace());
        assertEquals("One", one.name());
        assertEquals("teaselib.core.util.QualifiedItemTest.TestEnum.One#myGuid", one.toString());
    }

    @Test
    public void testItem() throws IOException {
        try (TestScript testScript = new TestScript()) {
            Item testItem = testScript.item("teaselib.core.util.QualifiedItemTest.TestEnum.One");
            QualifiedString one = QualifiedString.of(testItem);

            assertEquals("teaselib.core.util.QualifiedItemTest.TestEnum", one.namespace());
            assertEquals("One", one.name());
            assertEquals("teaselib.core.util.QualifiedItemTest.TestEnum.One#One", one.toString());
        }
    }

    @Test
    public void testItemGuid() throws IOException {
        try (TestScript testScript = new TestScript()) {
            Item notFound = testScript.item("teaselib.core.util.QualifiedItemTest.TestEnum.One#myGuid");
            assertEquals(Item.NotFound, notFound);

            Item testItem = testScript.item("teaselib.Toys.Gag#ring_gag");
            QualifiedString gag = QualifiedString.of(testItem);

            assertEquals("teaselib.Toys", gag.namespace());
            assertEquals("Gag", gag.name());
            assertEquals("teaselib.Toys.Gag#ring_gag", gag.toString());
        }
    }

    @Test
    public void testIdentity() throws IOException {
        try (TestScript testScript = new TestScript()) {
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
            assertNotEquals(QualifiedString.of(one1), QualifiedString.of(one2));
        }
    }

    private static ItemImpl createOne(Script script, String name) {
        return new ItemImpl(script.teaseLib, TeaseLib.DefaultDomain, QualifiedString.from(TestEnum.One, name),
                "A_Number");
    }

}
