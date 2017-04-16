package teaselib.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import teaselib.Length;
import teaselib.Size;
import teaselib.TeaseScript;
import teaselib.core.TeaseLib;
import teaselib.test.TestScript;

public class ItemImplTest {

    @Test
    public void testAvailable() throws Exception {
        TeaseScript script = TestScript.getOne();
        TeaseLib.PersistentBoolean foobar = script.teaseLib.new PersistentBoolean(
                TeaseLib.DefaultDomain, "Foo", "Bar");
        Item item = new ItemImpl("test", foobar);

        item.setAvailable(false);
        assertEquals(false, item.isAvailable());
        assertEquals(false, foobar.value());

        item.setAvailable(true);
        assertEquals(true, item.isAvailable());
        assertEquals(true, foobar.value());
    }

    enum Foo {
        Bar
    }

    @Test
    public void testIs() throws Exception {
        TeaseScript script = TestScript.getOne();
        TeaseLib.PersistentBoolean foobar = script.teaseLib.new PersistentBoolean(
                TeaseLib.DefaultDomain, "Foo", "Bar");
        Item item = new ItemImpl(Foo.Bar, foobar, "Foo Bar", Size.Large, Length.Long);

        assertTrue(item.is(Size.Large));
        assertFalse(item.is(Size.Small));

        assertTrue(item.is(Size.Large, Length.Long));
        assertFalse(item.is(Size.Large, Length.Short));

        assertFalse(item.is(Size.Small, Length.Short));
    }

    @Test
    public void testIsEmptyArg() throws Exception {
        TeaseScript script = TestScript.getOne();
        TeaseLib.PersistentBoolean foobar = script.teaseLib.new PersistentBoolean(
                TeaseLib.DefaultDomain, "Foo", "Bar");
        Item item = new ItemImpl(Foo.Bar, foobar, "Foo Bar", Size.Large, Length.Long);

        assertFalse(item.is());
    }

    @Test
    public void testIsHAndlesArrays() throws Exception {
        TeaseScript script = TestScript.getOne();
        TeaseLib.PersistentBoolean foobar = script.teaseLib.new PersistentBoolean(
                TeaseLib.DefaultDomain, "Foo", "Bar");
        Item item = new ItemImpl(Foo.Bar, foobar, "Foo Bar", Size.Large, Length.Long);

        assertTrue(item.is(new Size[] { Size.Large }));
        assertFalse(item.is(new Size[] { Size.Small }));

        assertTrue(item.is(new Enum<?>[] { Size.Large, Length.Long }));
        assertFalse(item.is(new Enum<?>[] { Size.Large, Length.Short }));

        assertFalse(item.is(new Enum<?>[] { Size.Small, Length.Short }));
    }
}
