// package teaselib.core.util;
//
// import static org.junit.Assert.assertEquals;
// import static org.junit.Assert.assertNotEquals;
// import static org.junit.jupiter.api.Assertions.assertThrows;
//
// import org.junit.Test;
//
// import teaselib.Toys;
//
// public class AbstractQualifiedItemTest {
//
// @Test
// public void testEquals() {
// QualifiedItem dildoAsEnum = new QualifiedEnum(Toys.Dildo);
// assertEquals(dildoAsEnum, dildoAsEnum);
// QualifiedObject dildoAsString = new QualifiedObject("teaselib.Toys.Dildo");
// assertEquals(dildoAsEnum, dildoAsString);
// assertEquals(dildoAsEnum, Toys.Dildo);
// assertEquals(dildoAsEnum, "teaselib.Toys.Dildo");
//
// assertNotEquals(dildoAsEnum, new QualifiedEnum(Toys.Buttplug));
// assertNotEquals(dildoAsEnum, new QualifiedObject("teaselib.Toys.Buttplug"));
// assertNotEquals(dildoAsEnum, Toys.Buttplug);
// assertNotEquals(dildoAsEnum, "teaselib.Toys.Buttplug");
//
// assertEquals(dildoAsString, dildoAsString);
// assertEquals(dildoAsString, dildoAsEnum);
// assertEquals(dildoAsString, Toys.Dildo);
// assertEquals(dildoAsString, "teaselib.Toys.Dildo");
//
// assertNotEquals(dildoAsString, new QualifiedObject("teaselib.Toys.Buttplug"));
// assertNotEquals(dildoAsString, new QualifiedEnum(Toys.Buttplug));
// assertNotEquals(dildoAsString, "teaselib.Toys.Buttplug");
// assertNotEquals(dildoAsString, Toys.Buttplug);
// }
//
// @Test
// public void testToString() throws Exception {
// assertNotEquals(new QualifiedEnum(Toys.Dildo).toString(), "Dildo");
// assertNotEquals(new QualifiedEnum(Toys.Dildo).toString(), "Toys.Dildo");
//
// assertEquals("teaselib.Toys.Dildo", new QualifiedEnum(Toys.Dildo).toString());
// assertEquals("teaselib.Toys.Dildo", new QualifiedObject("teaselib.Toys.Dildo").toString());
// }
//
// @Test
// public void testQualifiedEnumNullConstructorArgument() throws Exception {
// assertThrows(NullPointerException.class, () -> new QualifiedEnum(null));
// }
//
// @Test
// public void testQualifiedObjectNullConstructorArgument() throws Exception {
// assertThrows(NullPointerException.class, () -> new QualifiedObject(null));
// }
//
// @Test
// public void testQualifiedObjectEnumConstructorArgument() throws Exception {
// assertThrows(IllegalArgumentException.class, () -> new QualifiedObject(Toys.Dildo));
// }
//
// }
