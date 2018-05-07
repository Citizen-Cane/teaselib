package teaselib.core.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;

import org.junit.Test;

import teaselib.Toys;

public class AbstractQualifiedItemTest {

    @Test
    public void testEquals() {
        assertEquals(new QualifiedEnum(Toys.Dildo), new QualifiedEnum(Toys.Dildo));
        assertEquals(new QualifiedEnum(Toys.Dildo), new QualifiedObject("teaselib.Toys.Dildo"));
        assertEquals(new QualifiedEnum(Toys.Dildo), Toys.Dildo);
        assertEquals(new QualifiedEnum(Toys.Dildo), "teaselib.Toys.Dildo");

        assertNotEquals(new QualifiedEnum(Toys.Dildo), new QualifiedEnum(Toys.Buttplug));
        assertNotEquals(new QualifiedEnum(Toys.Dildo),
                new QualifiedObject("teaselib.Toys.Buttplug"));
        assertNotEquals(new QualifiedEnum(Toys.Dildo), Toys.Buttplug);
        assertNotEquals(new QualifiedEnum(Toys.Dildo), "teaselib.Toys.Buttplug");

        assertEquals(new QualifiedObject("teaselib.Toys.Dildo"),
                new QualifiedObject("teaselib.Toys.Dildo"));
        assertEquals(new QualifiedObject("teaselib.Toys.Dildo"), new QualifiedEnum(Toys.Dildo));
        assertEquals(new QualifiedObject("teaselib.Toys.Dildo"), Toys.Dildo);
        assertEquals(new QualifiedObject("teaselib.Toys.Dildo"), "teaselib.Toys.Dildo");

        assertNotEquals(new QualifiedObject("teaselib.Toys.Dildo"),
                new QualifiedObject("teaselib.Toys.Buttplug"));
        assertNotEquals(new QualifiedObject("teaselib.Toys.Dildo"),
                new QualifiedEnum(Toys.Buttplug));
        assertNotEquals(new QualifiedObject("teaselib.Toys.Dildo"), "teaselib.Toys.Buttplug");
        assertNotEquals(new QualifiedObject("teaselib.Toys.Dildo"), Toys.Buttplug);
    }

    @Test
    public void testToString() throws Exception {
        assertNotEquals(new QualifiedEnum(Toys.Dildo).toString(), "Dildo");
        assertNotEquals(new QualifiedEnum(Toys.Dildo).toString(), "Toys.Dildo");

        assertEquals(new QualifiedEnum(Toys.Dildo).toString(), "teaselib.Toys.Dildo");

        assertEquals(new QualifiedObject("teaselib.Toys.Dildo").toString(), "teaselib.Toys.Dildo");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testQualifiedEnumNullConstructorArgument() throws Exception {
        assertNotNull(new QualifiedEnum(null));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testQualifiedObjectNullConstructorArgument() throws Exception {
        assertNotNull(new QualifiedObject(null));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testQualifiedObjectEnumlConstructorArgument() throws Exception {
        assertNotNull(new QualifiedObject(Toys.Dildo));
    }
}
