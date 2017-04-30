package teaselib.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import teaselib.Body;
import teaselib.Features;
import teaselib.Length;
import teaselib.Material;
import teaselib.Size;
import teaselib.TeaseScript;
import teaselib.Toys;
import teaselib.core.TeaseLib;
import teaselib.test.TestScript;

public class ItemImplTest {

    @Test
    public void testAvailable() throws Exception {
        TeaseScript script = TestScript.getOne();
        TeaseLib.PersistentBoolean foobar = script.teaseLib.new PersistentBoolean(
                TeaseLib.DefaultDomain, "Foo", "Bar");
        Item item = new ItemImpl(script.teaseLib, "test", foobar);

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
        Foo[] peers = new Foo[] {};
        Item item = new ItemImpl(script.teaseLib, Foo.Bar, foobar, "Foo Bar", peers,
                new Enum<?>[] { Size.Large, Length.Long });

        assertTrue(item.is(Size.Large));
        assertFalse(item.is(Size.Small));

        assertTrue(item.is(Size.Large, Length.Long));
        assertFalse(item.is(Size.Large, Length.Short));

        assertFalse(item.is(Size.Small, Length.Short));
    }

    @Test
    public void testIs_EmptyArg() throws Exception {
        TeaseScript script = TestScript.getOne();
        TeaseLib.PersistentBoolean foobar = script.teaseLib.new PersistentBoolean(
                TeaseLib.DefaultDomain, "Foo", "Bar");
        Foo[] peers = new Foo[] {};
        Item item = new ItemImpl(script.teaseLib, Foo.Bar, foobar, "Foo Bar", peers,
                new Enum<?>[] { Size.Large, Length.Long });

        assertFalse(item.is());
    }

    @Test
    public void testIsHAndlesArrays() throws Exception {
        TeaseScript script = TestScript.getOne();
        TeaseLib.PersistentBoolean foobar = script.teaseLib.new PersistentBoolean(
                TeaseLib.DefaultDomain, "Foo", "Bar");
        Foo[] peers = new Foo[] {};
        Item item = new ItemImpl(script.teaseLib, Foo.Bar, foobar, "Foo Bar", peers,
                new Enum<?>[] { Size.Large, Length.Long });

        assertTrue(item.is(new Size[] { Size.Large }));
        assertFalse(item.is(new Size[] { Size.Small }));

        assertTrue(item.is(new Enum<?>[] { Size.Large, Length.Long }));
        assertFalse(item.is(new Enum<?>[] { Size.Large, Length.Short }));

        assertFalse(item.is(new Enum<?>[] { Size.Small, Length.Short }));
    }

    @Test
    public void testToAutomaticDefaultsAndAttributes() throws Exception {
        TeaseScript script = TestScript.getOne();

        assertFalse(script.state(Toys.Buttplug).applied());
        assertFalse(script.state(Toys.Buttplug).is(Toys.Anal.Beads));

        script.items(Toys.Buttplug).get(Toys.Anal.Beads).apply();

        assertTrue(script.state(Toys.Buttplug).applied());
        assertTrue(script.state(Toys.Buttplug).is(Toys.Anal.Beads));
        assertTrue(script.state(Toys.Buttplug).is(Features.Anal));

        assertTrue(script.state(Body.SomethingInButt).applied());
        assertTrue(script.state(Body.SomethingInButt).is(Toys.Buttplug));

        assertFalse(script.state(Body.SomethingInButt).is(Toys.Anal.Beads));
        assertFalse(script.state(Body.SomethingInButt).is(Features.Anal));

        // This is how to comment a certain item in a certain body location
        if (script.state(Body.SomethingInButt).is(Toys.Buttplug)) {
            if (script.item(Toys.Buttplug).is(Toys.Anal.Beads)) {
                say("You're wearing anal beads", script.state(Toys.Buttplug).is(Toys.Anal.Beads));
            }
        }
    }

    @Test
    public void testToAppliesDefaultsAndAttributesPlusCustomPeers() {
        TeaseScript script = TestScript.getOne();

        assertFalse(script.state(Toys.Wrist_Restraints).applied());

        // Wrists are not only tied, but also tied behind back
        script.items(Toys.Wrist_Restraints).get(Material.Leather).to(Body.WristsTiedBehindBack);

        assertTrue(script.state(Toys.Wrist_Restraints).applied());
        assertTrue(script.state(Toys.Wrist_Restraints).is(Material.Leather));

        assertTrue(script.state(Body.WristsTied).applied());
        assertTrue(script.state(Body.WristsTiedBehindBack).applied());

        assertTrue(script.state(Body.WristsTied).is(Toys.Wrist_Restraints));
        assertTrue(script.state(Body.WristsTiedBehindBack).is(Toys.Wrist_Restraints));

        // This is how to comment a certain item in a certain body location
        if (script.state(Body.WristsTied).is(Toys.Wrist_Restraints)) {
            if (script.item(Toys.Wrist_Restraints).is(Material.Leather)) {
                say("You're wearing leather restraints",
                        script.state(Toys.Wrist_Restraints).is(Material.Leather));
            }
        }
    }

    private static void say(String message, boolean assertion) {
        assertTrue(message, assertion);
    }

    @Test
    public void testCanApply() throws Exception {
        TeaseScript script = TestScript.getOne();

        assertFalse(script.state(Toys.Wrist_Restraints).applied());

        Item wristRestraints = script.items(Toys.Wrist_Restraints).get(Material.Leather);

        assertTrue(wristRestraints.canApply());
        wristRestraints.apply();
        assertFalse(wristRestraints.canApply());

        // Bend the rules, for when we have custom toys, scripts should also be
        // flexible
        wristRestraints.apply();
        assertFalse(wristRestraints.canApply());
    }
}
