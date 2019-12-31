package teaselib.util;

import static org.junit.Assert.*;

import org.junit.Test;

import teaselib.Accessoires;
import teaselib.Body;
import teaselib.Household;
import teaselib.Length;
import teaselib.Material;
import teaselib.Posture;
import teaselib.Size;
import teaselib.State;
import teaselib.TeaseScript;
import teaselib.Toys;
import teaselib.core.TeaseLib;
import teaselib.core.util.QualifiedItem;
import teaselib.test.TestScript;

public class ItemImplTest {

    enum PeerlessItem {
        Test1
    }

    @Test
    public void testPeerlessApply() {
        TestScript script = TestScript.getOne();
        assertFalse(script.item(PeerlessItem.Test1).applied());

        script.item(PeerlessItem.Test1).apply();
        assertTrue(script.item(PeerlessItem.Test1).applied());

        script.item(PeerlessItem.Test1).remove();
        assertFalse(script.item(PeerlessItem.Test1).applied());
    }

    @Test
    public void testAvailable() {
        TeaseScript script = TestScript.getOne();
        QualifiedItem fooBar = QualifiedItem.of("Foo.Bar");
        String storageName = fooBar.name();
        TeaseLib.PersistentBoolean value = script.teaseLib.new PersistentBoolean(TeaseLib.DefaultDomain,
                fooBar.namespace(), storageName);
        Item item = new ItemImpl(script.teaseLib, fooBar, TeaseLib.DefaultDomain, new ItemGuid(storageName),
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
    public void testIs() {
        TeaseScript script = TestScript.getOne();
        Foo[] peers = new Foo[] {};
        Item item = new ItemImpl(script.teaseLib, Foo.Bar, TeaseLib.DefaultDomain, new ItemGuid("Foo_Bar"), "Foo Bar",
                peers, new Object[] { Size.Large, Length.Long });

        assertTrue(item.is(Size.Large));
        assertFalse(item.is(Size.Small));

        assertTrue(item.is(Size.Large, Length.Long));
        assertFalse(item.is(Size.Large, Length.Short));

        assertFalse(item.is(Size.Small, Length.Short));
    }

    @Test
    public void testIs_EmptyArg() {
        TeaseScript script = TestScript.getOne();
        Foo[] peers = new Foo[] {};
        Item item = new ItemImpl(script.teaseLib, Foo.Bar, TeaseLib.DefaultDomain, new ItemGuid("Foo_Bar"), "Foo Bar",
                peers, new Object[] { Size.Large, Length.Long });

        assertFalse(item.is());
    }

    @Test
    public void testIsHandlesArrays() {
        TeaseScript script = TestScript.getOne();
        Foo[] peers = new Foo[] {};
        Item item = new ItemImpl(script.teaseLib, Foo.Bar, TeaseLib.DefaultDomain, new ItemGuid("Foo_Bar"), "Foo Bar",
                peers, new Object[] { Size.Large, Length.Long });

        assertTrue(item.is(Size.Large));
        assertFalse(item.is(Size.Small));

        assertTrue(item.is(Size.Large, Length.Long));
        assertFalse(item.is(Size.Large, Length.Short));

        assertFalse(item.is(Size.Small, Length.Short));
    }

    @Test
    public void testIsWithMixedPeersAndAttributes() {
        TeaseScript script = TestScript.getOne();

        Item nippleClamps = script.item(Toys.Nipple_Clamps);
        nippleClamps.apply();

        nippleClamps.is(Body.OnNipples);
        nippleClamps.is(Material.Metal);
        nippleClamps.is(Body.OnNipples, Material.Metal);
    }

    @Test
    public void testApplyToAutomaticDefaultsAndAttributes() {
        TestScript script = TestScript.getOne();

        assertFalse(script.state(Toys.Buttplug).applied());
        assertFalse(script.state(Toys.Buttplug).is(Toys.Anal.Beads));
        assertFalse(script.state(Body.InButt).is(Toys.Anal.Beads));
        assertFalse(script.state(Body.InButt).is(Body.Orifice.Anal));

        Item analBeads1 = script.items(Toys.Buttplug).matching(Toys.Anal.Beads).get();
        analBeads1.apply();

        assertTrue(script.state(Toys.Buttplug).applied());
        assertTrue(script.state(Toys.Buttplug).is(Toys.Anal.Beads));
        assertTrue(script.state(Toys.Buttplug).is(Body.Orifice.Anal));

        assertTrue(script.state(Body.InButt).applied());
        assertTrue(script.state(Body.InButt).is(Toys.Buttplug));

        assertTrue(script.state(Body.InButt).is(Toys.Anal.Beads));
        assertTrue(script.state(Body.InButt).is(Body.Orifice.Anal));

        Item analBeads2 = script.items(Toys.Buttplug).matching(Toys.Anal.Beads).get();
        assertTrue(analBeads2.is(Toys.Anal.Beads));
        analBeads2.remove();

        assertFalse(script.state(Toys.Buttplug).is(Toys.Anal.Beads));
        assertFalse(script.state(Toys.Buttplug).is(Body.Orifice.Anal));
        assertFalse(script.state(Body.InButt).is(Toys.Anal.Beads));
        assertFalse(script.state(Body.InButt).is(Body.Orifice.Anal));

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
        TestScript script = TestScript.getOne();
        script.addTestUserItems();

        assertFalse(script.state(Toys.Wrist_Restraints).applied());

        // Wrists are not only tied, but also tied behind back
        script.items(Toys.Wrist_Restraints).matching(Material.Leather).get().applyTo(Posture.WristsTiedBehindBack);

        assertTrue(script.state(Toys.Wrist_Restraints).applied());
        assertTrue(script.state(Toys.Wrist_Restraints).is(Material.Leather));

        assertTrue(script.state(Body.WristsTied).applied());
        assertTrue(script.state(Posture.WristsTiedBehindBack).applied());

        assertTrue(script.state(Body.WristsTied).is(Toys.Wrist_Restraints));
        assertTrue(script.state(Posture.WristsTiedBehindBack).is(Toys.Wrist_Restraints));

        script.item(Toys.Wrist_Restraints).remove();
        assertFalse(script.state(Toys.Wrist_Restraints).applied());
        assertFalse(script.state(Body.WristsTied).applied());
        assertFalse(script.state(Posture.WristsTiedBehindBack).applied());
    }

    private static void say(String message, boolean assertion) {
        assertTrue(message, assertion);
    }

    @Test
    public void testApply1to1AndRemoveTheOtherWayAround() {
        TeaseScript script = TestScript.getOne();

        script.item(Toys.Wrist_Restraints).apply();
        assertTrue(script.state(Body.WristsTied).applied());
        assertTrue(script.state(Toys.Wrist_Restraints).is(script.namespace));
        assertTrue(script.state(Body.WristsTied).is(script.namespace));

        script.state(Body.WristsTied).remove();
        assertFalse(script.state(Body.WristsTied).applied());

        assertFalse(script.state(Body.WristsTied).is(script.namespace));
        assertFalse(script.state(Toys.Wrist_Restraints).applied());
        assertFalse(script.state(Toys.Wrist_Restraints).is(script.namespace));
    }

    @Test
    public void testApply1toNAndRemoveTheOtherWayAround() {
        TestScript script = TestScript.getOne();
        script.addTestUserItems();
        Items restraints = script.items(Toys.Wrist_Restraints, Toys.Ankle_Restraints);
        restraints.stream().forEach(item -> item.setAvailable(true));
        restraints = restraints.prefer(Material.Metal);

        restraints.apply();
        assertTrue(script.state(Body.WristsTied).applied());
        assertTrue(script.state(Toys.Wrist_Restraints).is(script.namespace));
        assertTrue(script.state(Body.WristsTied).is(script.namespace));
        assertTrue(script.state(Body.AnklesTied).applied());
        assertTrue(script.state(Toys.Ankle_Restraints).is(script.namespace));
        assertTrue(script.state(Body.AnklesTied).is(script.namespace));

        script.state(Body.WristsTied).remove();
        assertFalse(script.state(Body.WristsTied).applied());

        assertFalse(script.state(Body.WristsTied).is(script.namespace));
        assertFalse(script.state(Toys.Wrist_Restraints).applied());
        assertFalse(script.state(Toys.Wrist_Restraints).is(script.namespace));

        assertTrue(script.state(Body.AnklesTied).applied());
        assertTrue(script.state(Toys.Ankle_Restraints).is(script.namespace));
        assertTrue(script.state(Body.AnklesTied).is(script.namespace));
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
        TestScript script = TestScript.getOne();
        script.addTestUserItems();

        assertFalse(script.state(Toys.Wrist_Restraints).applied());

        Item wristRestraints = script.items(Toys.Wrist_Restraints).matching(Material.Leather).get();

        assertFalse(wristRestraints.applied());
        assertTrue(wristRestraints.canApply());
        assertTrue(wristRestraints.is(wristRestraints));

        // Perfectly legal for detachable restraints
        wristRestraints.apply();

        assertTrue(wristRestraints.applied());
        assertFalse(wristRestraints.canApply());
        assertTrue(wristRestraints.is(wristRestraints));
    }

    @Test
    public void testCanApplyWithDefaults() {
        TestScript script = TestScript.getOne();

        assertFalse(script.state(Toys.Gag).applied());

        Item gag = script.items(Toys.Gag).matching(Toys.Gags.Ball_Gag).get();

        assertFalse(gag.applied());
        assertTrue(gag.canApply());
        assertTrue(gag.is(gag));

        gag.apply();

        assertTrue(gag.applied());
        assertFalse(gag.canApply());
        assertTrue(gag.is(gag));
    }

    @Test(expected = IllegalStateException.class)
    public void applyingTwiceRuinsPlausibilityOfActor() {
        TestScript script = TestScript.getOne();
        assertFalse(script.state(Toys.Gag).applied());

        script.item(Toys.Gag).apply();
        assertTrue(script.state(Toys.Gag).applied());

        script.item(Toys.Gag).apply();
    }

    @Test
    public void testApplyToAppliesDefaultsAndAttributesPlusCustomPeersWithStrings() {
        TestScript script = TestScript.getOne();
        script.addTestUserItems();

        String Toys_Wrist_Restraints = "teaselib.Toys.Wrist_Restraints";
        String Body_WristsTied = "teaselib.Body.WristsTied";
        String Body_WristsTiedBehindBack = "teaselib.Body.WristsTiedBehindBack";
        String leather = "teaselib.Material.Leather";

        assertFalse(script.state(Toys_Wrist_Restraints).applied());

        // Wrists are not only tied, but also tied behind back

        script.items(Toys_Wrist_Restraints).matching(leather).get().applyTo(Body_WristsTiedBehindBack);

        assertTrue(script.state(Toys_Wrist_Restraints).applied());
        assertTrue(script.state(Toys_Wrist_Restraints).is(leather));

        assertTrue(script.state(Body_WristsTied).applied());
        assertTrue(script.state(Body_WristsTiedBehindBack).applied());

        assertTrue(script.state(Body_WristsTied).is(Toys_Wrist_Restraints));
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
        TestScript script = TestScript.getOne();
        script.addTestUserItems();

        String Toys_Wrist_Restraints = "teaselib.Toys.Wrist_Restraints";
        String Body_WristsTied = "teaselib.Body.WristsTied";
        String Posture_WristsTiedBehindBack = "teaselib.Posture.WristsTiedBehindBack";
        String leather = "teaselib.Material.Leather";

        assertFalse(script.state(Toys.Wrist_Restraints).applied());
        assertFalse(script.state(Toys_Wrist_Restraints).applied());

        // Wrists are not only tied, but also tied behind back

        script.items(Toys_Wrist_Restraints).matching(leather).get().applyTo(Posture_WristsTiedBehindBack);

        assertTrue(script.state(Toys.Wrist_Restraints).applied());
        assertTrue(script.state(Toys_Wrist_Restraints).is(leather));
        assertTrue(script.state(Toys_Wrist_Restraints).is(Material.Leather));
        assertTrue(script.state(Toys.Wrist_Restraints).applied());
        assertTrue(script.state(Toys_Wrist_Restraints).is(leather));
        assertTrue(script.state(Toys_Wrist_Restraints).is(Material.Leather));

        assertTrue(script.state(Body_WristsTied).applied());
        assertTrue(script.state(Posture_WristsTiedBehindBack).applied());
        assertTrue(script.state(Body.WristsTied).applied());
        assertTrue(script.state(Posture.WristsTiedBehindBack).applied());

        assertTrue(script.state(Body.WristsTied).is(Toys_Wrist_Restraints));
        assertTrue(script.state(Posture.WristsTiedBehindBack).is(Toys.Wrist_Restraints));
        assertTrue(script.state(Body_WristsTied).is(Toys.Wrist_Restraints));
        assertTrue(script.state(Posture_WristsTiedBehindBack).is(Toys.Wrist_Restraints));

        assertTrue(script.state(Body.WristsTied).is(Toys_Wrist_Restraints));
        assertTrue(script.state(Posture.WristsTiedBehindBack).is(Toys_Wrist_Restraints));
        assertTrue(script.state(Body_WristsTied).is(Toys_Wrist_Restraints));
        assertTrue(script.state(Posture_WristsTiedBehindBack).is(Toys_Wrist_Restraints));

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

    @Test
    public void testTemporaryItems() {
        TestScript script = TestScript.getOne();
        script.addTestUserItems();

        assertFalse(script.state(Toys.Wrist_Restraints).applied());

        Item wristRestraints = script.items(Toys.Wrist_Restraints).matching(Material.Leather).get();
        wristRestraints.applyTo(Posture.WristsTiedBehindBack);

        Items temporaryItems = script.teaseLib.temporaryItems();
        Item temporaryWristRestraints = temporaryItems.matching(Toys.Wrist_Restraints).get();

        assertEquals(wristRestraints, temporaryWristRestraints);
        assertTrue(temporaryItems.contains(Toys.Wrist_Restraints));
    }

    @Test
    public void testRemoveOneOfMultipleItemsToSamePeer() {
        TestScript script = TestScript.getOne();

        Items restraints = script.items(Toys.Wrist_Restraints, Toys.Ankle_Restraints, Toys.Collar)
                .prefer(Material.Leather);
        restraints.apply();

        Items chains = script.items(Toys.Chains, Accessoires.Bells);
        chains.applyTo(restraints);
        assertTrue(chains.allApplied());
        assertTrue(restraints.allApplied());

        Item singleChainItem = chains.get(Toys.Chains);
        Item bell = script.item(Accessoires.Bells);
        assertTrue(bell.applied());
        singleChainItem.remove();
        assertTrue(bell.applied());

        assertTrue(chains.anyApplied());
        assertFalse(chains.allApplied());
        assertTrue(restraints.allApplied());

        Item wristRestraints = restraints.get(Toys.Wrist_Restraints);
        // TODO remove immediately removes the bell guid,
        // but the bell is still attached to ankles and collar
        wristRestraints.remove();
        assertTrue(chains.anyApplied());
        assertFalse(restraints.allApplied());

        bell.remove();
        assertFalse(chains.anyApplied());

        script.items(Toys.Ankle_Restraints, Toys.Collar).remove();
        assertFalse(restraints.anyApplied());
    }

    @Test
    public void testIsClass() {
        TestScript script = TestScript.getOne();

        Item woodenSpoon = script.item(Household.Wooden_Spoon);
        assertTrue(woodenSpoon.is(Material.Wood));
        assertTrue(woodenSpoon.is(Material.class));
    }

}
