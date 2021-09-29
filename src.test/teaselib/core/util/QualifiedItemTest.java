package teaselib.core.util;

import static org.junit.Assert.*;

import org.junit.Test;

import teaselib.Material;
import teaselib.core.Script;
import teaselib.core.TeaseLib;
import teaselib.test.TestScript;
import teaselib.util.Item;
import teaselib.util.ItemGuid;
import teaselib.util.ItemImpl;

public class QualifiedItemTest {
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
    public void testItem() {
        TestScript testScript = TestScript.getOne();
        Item testItem = testScript.item("teaselib.core.util.QualifiedItemTest.TestEnum.One");
        QualifiedString one = QualifiedString.of(testItem);

        assertEquals("teaselib.core.util.QualifiedItemTest.TestEnum", one.namespace());
        assertEquals("One", one.name());
        assertEquals("teaselib.core.util.QualifiedItemTest.TestEnum.One#One", one.toString());
    }

    @Test
    public void testItemGuid() {
        TestScript testScript = TestScript.getOne();
        Item notFound = testScript.item("teaselib.core.util.QualifiedItemTest.TestEnum.One#myGuid");
        assertEquals(Item.NotFound, notFound);

        Item testItem = testScript.item("teaselib.Toys.Gag#ring_gag");
        QualifiedString gag = QualifiedString.of(testItem);

        assertEquals("teaselib.Toys", gag.namespace());
        assertEquals("Gag", gag.name());
        assertEquals("teaselib.Toys.Gag#ring_gag", gag.toString());
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
        assertNotEquals(QualifiedString.of(one1), QualifiedString.of(one2));
    }

    private static ItemImpl createOne(Script script, String name) {
        return new ItemImpl(script.teaseLib, TeaseLib.DefaultDomain,
                ItemGuid.from(QualifiedString.of(TestEnum.One), name), "A_Number");
    }

}
