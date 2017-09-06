package teaselib.util;

import static org.junit.Assert.*;

import org.junit.Test;

import teaselib.Body;
import teaselib.Features;
import teaselib.Length;
import teaselib.Material;
import teaselib.Size;
import teaselib.State;
import teaselib.TeaseScript;
import teaselib.Toys;
import teaselib.core.TeaseLib;
import teaselib.core.util.QualifiedItem;
import teaselib.test.TestScript;

public class ItemImplTest {

    @Test
    public void testAvailable() throws Exception {
        TeaseScript script = TestScript.getOne();
        QualifiedItem<?> fooBar = QualifiedItem.of("Foo.Bar");
        String storageName = fooBar.name();
        TeaseLib.PersistentBoolean value = script.teaseLib.new PersistentBoolean(TeaseLib.DefaultDomain,
                fooBar.namespace(), storageName);
        Item item = new ItemImpl(script.teaseLib, fooBar, TeaseLib.DefaultDomain, storageName,
                ItemImpl.createDisplayName(fooBar.name()));

        item.setAvailable(false);
        assertEquals(false, item.isAvailable());
        assertEquals(false, value.value());

        item.setAvailable(true);
        assertEquals(true, item.isAvailable());
        assertEquals(true, value.value());
    }

    enum Foo {
        Bar
    }

    @Test
    public void testIs() throws Exception {
        TeaseScript script = TestScript.getOne();
        Foo[] peers = new Foo[] {};
        Item item = new ItemImpl(script.teaseLib, Foo.Bar, TeaseLib.DefaultDomain, "Foo.Bar", "Foo Bar", peers,
                new Object[] { Size.Large, Length.Long });

        assertTrue(item.is(Size.Large));
        assertFalse(item.is(Size.Small));

        assertTrue(item.is(Size.Large, Length.Long));
        assertFalse(item.is(Size.Large, Length.Short));

        assertFalse(item.is(Size.Small, Length.Short));
    }

    @Test
    public void testIs_EmptyArg() throws Exception {
        TeaseScript script = TestScript.getOne();
        Foo[] peers = new Foo[] {};
        Item item = new ItemImpl(script.teaseLib, Foo.Bar, TeaseLib.DefaultDomain, "Foo.Bar", "Foo Bar", peers,
                new Object[] { Size.Large, Length.Long });

        assertFalse(item.is());
    }

    @Test
    public void testIsHandlesArrays() throws Exception {
        TeaseScript script = TestScript.getOne();
        Foo[] peers = new Foo[] {};
        Item item = new ItemImpl(script.teaseLib, Foo.Bar, TeaseLib.DefaultDomain, "Foo.Bar", "Foo Bar", peers,
                new Object[] { Size.Large, Length.Long });

        assertTrue(item.is(new Object[] { Size.Large }));
        assertFalse(item.is(new Object[] { Size.Small }));

        assertTrue(item.is(new Object[] { Size.Large, Length.Long }));
        assertFalse(item.is(new Object[] { Size.Large, Length.Short }));

        assertFalse(item.is(new Object[] { Size.Small, Length.Short }));
    }

    @Test
    public void testIsWithMixedPeersAndAttributes() throws Exception {
        TeaseScript script = TestScript.getOne();

        Item nippleClamps = script.item(Toys.Nipple_Clamps);
        nippleClamps.apply();

        nippleClamps.is(Body.OnNipples);
        nippleClamps.is(Material.Metal);
        nippleClamps.is(Body.OnNipples, Material.Metal);
    }

    @Test
    public void testApplyToAutomaticDefaultsAndAttributes() throws Exception {
        TeaseScript script = TestScript.getOne();

        assertFalse(script.state(Toys.Buttplug).applied());
        assertFalse(script.state(Toys.Buttplug).is(Toys.Anal.Beads));
        assertFalse(script.state(Body.InButt).is(Toys.Anal.Beads));
        assertFalse(script.state(Body.InButt).is(Features.Anal));

        Item analBeads1 = script.items(Toys.Buttplug).get(Toys.Anal.Beads);
        analBeads1.apply();

        assertTrue(script.state(Toys.Buttplug).applied());
        assertTrue(script.state(Toys.Buttplug).is(Toys.Anal.Beads));
        assertTrue(script.state(Toys.Buttplug).is(Features.Anal));

        assertTrue(script.state(Body.InButt).applied());
        assertTrue(script.state(Body.InButt).is(Toys.Buttplug));

        assertTrue(script.state(Body.InButt).is(Toys.Anal.Beads));
        assertTrue(script.state(Body.InButt).is(Features.Anal));

        Item analBeads2 = script.items(Toys.Buttplug).get(Toys.Anal.Beads);
        assertTrue(analBeads2.is(Toys.Anal.Beads));
        analBeads2.remove();

        assertFalse(script.state(Toys.Buttplug).is(Toys.Anal.Beads));
        assertFalse(script.state(Toys.Buttplug).is(Features.Anal));
        assertFalse(script.state(Body.InButt).is(Toys.Anal.Beads));
        assertFalse(script.state(Body.InButt).is(Features.Anal));

        Item buttPlug = script.item(Toys.Buttplug);
        assertFalse(buttPlug.applied());
        State inButt = script.state(Body.InButt);
        assertFalse(inButt.applied());

        // This is how to comment a certain item in a certain body location
        if (script.state(Body.InButt).is(Toys.Buttplug)) {
            if (script.item(Toys.Buttplug).is(Toys.Anal.Beads)) {
                say("You're wearing anal beads", script.state(Toys.Buttplug).is(Toys.Anal.Beads));
            }
        }
    }

    @Test
    public void testApplyToAppliesDefaultsAndAttributesPlusCustomPeers() {
        TeaseScript script = TestScript.getOne();

        assertFalse(script.state(Toys.Wrist_Restraints).applied());

        // Wrists are not only tied, but also tied behind back
        script.items(Toys.Wrist_Restraints).get(Material.Leather).applyTo(Body.WristsTiedBehindBack);

        assertTrue(script.state(Toys.Wrist_Restraints).applied());
        assertTrue(script.state(Toys.Wrist_Restraints).is(Material.Leather));

        assertFalse(script.state(Body.WristsTied).applied());
        assertTrue(script.state(Body.WristsTiedBehindBack).applied());

        assertFalse(script.state(Body.WristsTied).is(Toys.Wrist_Restraints));
        assertTrue(script.state(Body.WristsTiedBehindBack).is(Toys.Wrist_Restraints));

        // This is how to comment a certain item in a certain body location
        if (script.state(Body.WristsTied).is(Toys.Wrist_Restraints)) {
            if (script.item(Toys.Wrist_Restraints).is(Material.Leather)) {
                say("You're wearing leather restraints", script.state(Toys.Wrist_Restraints).is(Material.Leather));
            }
        }
    }

    private static void say(String message, boolean assertion) {
        assertTrue(message, assertion);
    }

    @Test
    public void testCanApplyWithoutDefaultsSimulation() {
        TeaseScript script = TestScript.getOne();
        State wristRestraints = script.state(Toys.Wrist_Restraints);

        wristRestraints.apply();

        assertTrue(wristRestraints.applied());
        assertFalse(wristRestraints.is(Toys.Wrist_Restraints));
    }

    @Test
    public void testCanApplyWithDefaultsSimulation() {
        TeaseScript script = TestScript.getOne();
        State gag = script.state(Toys.Gag);

        gag.applyTo(Body.InMouth);

        assertTrue(gag.applied());
        assertFalse(gag.is(Toys.Gag));
    }

    @Test
    public void testCanApplyWithoutDefaults() {
        TeaseScript script = TestScript.getOne();

        assertFalse(script.state(Toys.Wrist_Restraints).applied());

        Item wristRestraints = script.items(Toys.Wrist_Restraints).get(Material.Leather);

        assertFalse(wristRestraints.applied());
        assertTrue(wristRestraints.canApply());
        assertFalse(wristRestraints.is(wristRestraints));

        // Perfectly legal for detachable restraints
        wristRestraints.apply();

        assertTrue(wristRestraints.applied());
        assertFalse(wristRestraints.canApply());
        assertTrue(wristRestraints.is(wristRestraints));

        // Apply should work even if applied already, since after an item has been applied,
        // applying it again results in a correctly modeled setup
        wristRestraints.apply();
    }

    @Test
    public void testCanApplyWithDefaults() {
        TeaseScript script = TestScript.getOne();

        assertFalse(script.state(Toys.Gag).applied());

        Item gag = script.items(Toys.Gag).get(Material.Leather);

        assertFalse(gag.applied());
        assertTrue(gag.canApply());
        assertFalse(gag.is(gag));

        gag.apply();

        assertTrue(gag.applied());
        assertFalse(gag.canApply());
        assertTrue(gag.is(gag));

        // Apply should work even if applied already, since after an item has been applied,
        // applying it again results in a correctly modeled setup
        gag.apply();
    }

    @Test
    public void testApplyToAppliesDefaultsAndAttributesPlusCustomPeersWithStrings() {
        TeaseScript script = TestScript.getOne();

        String Toys_Wrist_Restraints = "teaselib.Toys.Wrist_Restraints";
        String Body_WristsTied = "teaselib.Body.WristsTied";
        String Body_WristsTiedBehindBack = "teaselib.Body.WristsTiedBehindBack";
        String leather = "teaselib.Material.Leather";

        assertFalse(script.state(Toys_Wrist_Restraints).applied());

        // Wrists are not only tied, but also tied behind back

        script.items(Toys_Wrist_Restraints).get(leather).applyTo(Body_WristsTiedBehindBack);

        assertTrue(script.state(Toys_Wrist_Restraints).applied());
        assertTrue(script.state(Toys_Wrist_Restraints).is(leather));

        assertFalse(script.state(Body_WristsTied).applied());
        assertTrue(script.state(Body_WristsTiedBehindBack).applied());

        assertFalse(script.state(Body_WristsTied).is(Toys_Wrist_Restraints));
        assertTrue(script.state(Body_WristsTiedBehindBack).is(Toys_Wrist_Restraints));

        // This is how to comment a certain item in a certain body location
        if (script.state(Body_WristsTied).is(Toys_Wrist_Restraints)) {
            if (script.item(Toys_Wrist_Restraints).is(leather)) {
                say("You're wearing leather restraints", script.state(Toys_Wrist_Restraints).is(leather));
            }
        }
    }

    @Test
    public void testStringsAndEnumsMixed() {
        TeaseScript script = TestScript.getOne();

        String Toys_Wrist_Restraints = "teaselib.Toys.Wrist_Restraints";
        String Body_WristsTied = "teaselib.Body.WristsTied";
        String Body_WristsTiedBehindBack = "teaselib.Body.WristsTiedBehindBack";
        String leather = "teaselib.Material.Leather";

        assertFalse(script.state(Toys.Wrist_Restraints).applied());
        assertFalse(script.state(Toys_Wrist_Restraints).applied());

        // Wrists are not only tied, but also tied behind back

        script.items(Toys_Wrist_Restraints).get(leather).applyTo(Body_WristsTiedBehindBack);

        assertTrue(script.state(Toys.Wrist_Restraints).applied());
        assertTrue(script.state(Toys_Wrist_Restraints).is(leather));
        assertTrue(script.state(Toys_Wrist_Restraints).is(Material.Leather));
        assertTrue(script.state(Toys.Wrist_Restraints).applied());
        assertTrue(script.state(Toys_Wrist_Restraints).is(leather));
        assertTrue(script.state(Toys_Wrist_Restraints).is(Material.Leather));

        assertFalse(script.state(Body_WristsTied).applied());
        assertTrue(script.state(Body_WristsTiedBehindBack).applied());
        assertFalse(script.state(Body.WristsTied).applied());
        assertTrue(script.state(Body.WristsTiedBehindBack).applied());

        assertFalse(script.state(Body.WristsTied).is(Toys_Wrist_Restraints));
        assertTrue(script.state(Body.WristsTiedBehindBack).is(Toys.Wrist_Restraints));
        assertFalse(script.state(Body_WristsTied).is(Toys.Wrist_Restraints));
        assertTrue(script.state(Body_WristsTiedBehindBack).is(Toys.Wrist_Restraints));

        assertFalse(script.state(Body.WristsTied).is(Toys_Wrist_Restraints));
        assertTrue(script.state(Body.WristsTiedBehindBack).is(Toys_Wrist_Restraints));
        assertFalse(script.state(Body_WristsTied).is(Toys_Wrist_Restraints));
        assertTrue(script.state(Body_WristsTiedBehindBack).is(Toys_Wrist_Restraints));

        // This is how to comment an item in a certain body location
        if (script.state(Body_WristsTied).is(Toys_Wrist_Restraints)) {
            if (script.item(Toys_Wrist_Restraints).is(leather)) {
                say("You're wearing leather restraints", script.state(Toys_Wrist_Restraints).is(leather));
            }
        }

        // Better
        if (script.state(Toys_Wrist_Restraints).is(Body.WristsTied)) {
            if (script.state(Toys_Wrist_Restraints).is(leather)) {
                say("You're wearing leather restraints", script.state(Toys_Wrist_Restraints).is(leather));
            }
        }

        // Even better
        if (script.item(Toys_Wrist_Restraints).is(Body.WristsTied, leather)) {
            say("You're wearing leather restraints", script.state(Toys_Wrist_Restraints).is(Body.WristsTied, leather));
        }
    }
}
