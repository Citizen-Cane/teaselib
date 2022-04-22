package teaselib.core;

import static org.junit.Assert.*;

import java.io.IOException;
import java.util.Collections;
import java.util.Iterator;
import java.util.concurrent.TimeUnit;

import org.junit.Test;
import org.junit.jupiter.api.Disabled;

import teaselib.Accessoires;
import teaselib.Body;
import teaselib.Bondage;
import teaselib.Color;
import teaselib.Household;
import teaselib.Length;
import teaselib.Material;
import teaselib.Posture;
import teaselib.Size;
import teaselib.State;
import teaselib.State.Persistence.Until;
import teaselib.Toys;
import teaselib.core.util.QualifiedString;
import teaselib.test.TestScript;
import teaselib.util.Item;
import teaselib.util.ItemImpl;
import teaselib.util.Items;

public class ItemImplTest {

    enum PeerlessItem {
        Test1
    }

    @Test
    public void testPeerlessApply() throws IOException {
        try (TestScript script = new TestScript()) {
            assertFalse(script.item(PeerlessItem.Test1).applied());

            script.item(PeerlessItem.Test1).apply();
            assertTrue(script.item(PeerlessItem.Test1).applied());

            script.item(PeerlessItem.Test1).remove();
            assertFalse(script.item(PeerlessItem.Test1).applied());
        }
    }

    @Test
    public void testAvailable() throws IOException {
        try (TestScript script = new TestScript()) {
            QualifiedString fooBar = QualifiedString.of("Foo.Bar");
            String guid = fooBar.name();
            TeaseLib.PersistentBoolean value = script.teaseLib.new PersistentBoolean(TeaseLib.DefaultDomain,
                    fooBar.toString(), guid + ".Available");
            Item item = new ItemImpl(script.teaseLib, TeaseLib.DefaultDomain, QualifiedString.from(fooBar, guid),
                    ItemImpl.createDisplayName(QualifiedString.from(fooBar, guid)));

            assertEquals(0, script.storage.size());
            item.setAvailable(false);
            assertEquals(1, script.storage.size());
            assertEquals(false, item.isAvailable());
            assertEquals(false, value.value());

            item.setAvailable(true);
            assertEquals(1, script.storage.size());
            assertEquals(true, item.isAvailable());
            assertEquals(true, value.value());
        }
    }

    enum Foo {
        Bar
    }

    @Test
    public void testIs() throws IOException {
        try (TestScript script = new TestScript()) {
            Foo[] defaultPeers = new Foo[] {};
            script.teaseLib.addUserItems(Collections.singleton(
                    new ItemImpl(script.teaseLib, TeaseLib.DefaultDomain, QualifiedString.from(Foo.Bar, "Foo_Bar"),
                            "Foo Bar", defaultPeers, new Object[] { Size.Large, Length.Long })));
            Item fooBar = script.item(Foo.Bar);

            assertTrue(fooBar.is(Size.Large));
            assertFalse(fooBar.is(Size.Small));
            assertTrue(fooBar.is(Size.Large, Length.Long));
            assertFalse(fooBar.is(Size.Large, Length.Short));
            assertFalse(fooBar.is(Size.Small, Length.Short));

            State inButt = script.state(Body.InButt);
            fooBar.applyTo(inButt);

            assertTrue(inButt.is(Foo.Bar));
            assertTrue(inButt.is(fooBar));
            assertTrue(inButt.is(Size.Large));
            assertTrue(inButt.is(Size.class));
            assertTrue(inButt.is(Length.Long));
            assertTrue(inButt.is(Length.class));
            assertTrue(inButt.is(script.namespace));

            assertFalse(inButt.is(Size.Small));
            assertFalse(inButt.is(Length.Short));
            assertFalse(inButt.is(Color.class));
        }
    }

    @Test
    public void testIsSupportsOnlyEnum() throws IOException {
        try (TestScript script = new TestScript()) {
            Item item = script.item(Toys.Blindfold);
            assertTrue(item.is(Toys.class));
            assertThrows(IllegalArgumentException.class, () -> item.is(Object.class));
        }
    }

    @Test
    public void testIsConditionalAnd() throws IOException {
        try (TestScript script = new TestScript()) {
            Foo[] defaultPpeers = new Foo[] {};
            script.teaseLib.addUserItems(Collections.singleton(
                    new ItemImpl(script.teaseLib, TeaseLib.DefaultDomain, QualifiedString.from(Foo.Bar, "Foo_Bar"),
                            "Foo Bar", defaultPpeers, new Object[] { Size.Large, Length.Long })));

            Item fooBar = script.item(Foo.Bar);
            State inButt = script.state(Body.InButt);
            fooBar.applyTo(inButt);

            assertTrue(inButt.is(Foo.Bar));
            assertTrue(inButt.is(Foo.Bar, fooBar));
            assertTrue(inButt.is(Foo.Bar, fooBar, Size.Large));
            assertTrue(inButt.is(Foo.Bar, fooBar, Size.Large, Size.class));
            assertTrue(inButt.is(Foo.Bar, fooBar, Size.Large, Size.class, Length.Long));
            assertTrue(inButt.is(Foo.Bar, fooBar, Size.Large, Size.class, Length.Long, Length.class));
            assertTrue(inButt.is(Foo.Bar, fooBar, Size.Large, Size.class, Length.Long, Length.class, script.namespace));
        }
    }

    @Test
    public void testIs_EmptyArg() throws IOException {
        try (TestScript script = new TestScript()) {
            Foo[] peers = new Foo[] {};
            Item item = new ItemImpl(script.teaseLib, TeaseLib.DefaultDomain, QualifiedString.from(Foo.Bar, "Foo_Bar"),
                    "Foo Bar", peers, new Object[] { Size.Large, Length.Long });

            assertFalse(item.is());
        }
    }

    @Test
    public void testIsHandlesArrays() throws IOException {
        try (TestScript script = new TestScript()) {
            Foo[] peers = new Foo[] {};
            Item item = new ItemImpl(script.teaseLib, TeaseLib.DefaultDomain, QualifiedString.from(Foo.Bar, "Foo_Bar"),
                    "Foo Bar", peers, new Object[] { Size.Large, Length.Long });

            assertTrue(item.is(Size.Large));
            assertFalse(item.is(Size.Small));

            assertTrue(item.is(Size.Large, Length.Long));
            assertFalse(item.is(Size.Large, Length.Short));

            assertFalse(item.is(Size.Small, Length.Short));
        }
    }

    @Test
    public void testIsWithMixedPeersAndAttributes() throws IOException {
        try (TestScript script = new TestScript()) {
            Item nippleClamps = script.item(Toys.Nipple_Clamps);
            nippleClamps.apply();

            nippleClamps.is(Body.OnNipples);
            nippleClamps.is(Material.Metal);
            nippleClamps.is(Body.OnNipples, Material.Metal);
        }
    }

    @Test
    public void testApplyToAutomaticDefaultsAndAttributes() throws IOException {
        try (TestScript script = new TestScript()) {
            assertFalse(script.state(Toys.Buttplug).applied());
            assertFalse(script.state(Toys.Buttplug).is(Toys.Anal.Beads));
            assertFalse(script.state(Body.InButt).is(Toys.Anal.Beads));
            assertFalse(script.state(Body.InButt).is(Body.Orifice.Anal));

            Item analBeads1 = script.items(Toys.Buttplug).matching(Toys.Anal.Beads).inventory().get();
            analBeads1.apply();

            assertTrue(script.state(Toys.Buttplug).applied());
            assertTrue(script.state(Toys.Buttplug).is(Toys.Anal.Beads));
            assertTrue(script.state(Toys.Buttplug).is(Body.Orifice.Anal));

            assertTrue(script.state(Body.InButt).applied());
            assertTrue(script.state(Body.InButt).is(Toys.Buttplug));

            assertTrue(script.state(Body.InButt).is(Toys.Anal.Beads));
            assertTrue(script.state(Body.InButt).is(Body.Orifice.Anal));

            Item analBeads2 = script.items(Toys.Buttplug).matching(Toys.Anal.Beads).inventory().get();
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
    }

    @Test
    public void testApplyToAppliesDefaultsAndAttributesPlusCustomPeers() throws IOException {
        try (TestScript script = new TestScript()) {
            script.addTestUserItems();

            assertFalse(script.state(Toys.Wrist_Restraints).applied());

            // Wrists are not only tied, but also tied behind back
            script.items(Toys.Wrist_Restraints).matching(Material.Leather).inventory()
                    .applyTo(Posture.WristsTiedBehindBack);

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
    }

    private static void say(String message, boolean assertion) {
        assertTrue(message, assertion);
    }

    @Test
    public void testApply1to1AndRemoveTheOtherWayAround() throws IOException {
        try (TestScript script = new TestScript()) {
            script.item(Toys.Wrist_Restraints).apply();
            assertTrue(script.state(Body.WristsTied).applied());
            assertTrue(script.state(Toys.Wrist_Restraints).is(script.namespace));
            assertTrue(script.state(Body.WristsTied).is(script.namespace));

            script.state(Body.WristsTied).remove();
            script.state(Body.WristsCuffed).remove();
            assertFalse(script.state(Body.WristsTied).applied());

            assertFalse(script.state(Body.WristsTied).is(script.namespace));
            assertFalse(script.state(Toys.Wrist_Restraints).applied());
            assertFalse(script.state(Toys.Wrist_Restraints).is(script.namespace));
        }
    }

    @Test
    public void testApply1toNAndRemoveTheOtherWayAround() throws IOException {
        try (TestScript script = new TestScript()) {
            script.addTestUserItems();
            Items restraints = script.items(Toys.Wrist_Restraints, Toys.Ankle_Restraints).inventory();
            restraints.stream().forEach(item -> item.setAvailable(true));
            restraints = restraints.prefer(Material.Metal);

            restraints.apply();

            State wristRestraints = script.state(Toys.Wrist_Restraints);
            State wristsCuffed = script.state(Body.WristsCuffed);
            State wristTied = script.state(Body.WristsTied);

            assertTrue(wristTied.applied());
            assertTrue(wristRestraints.is(script.namespace));
            assertTrue(wristTied.is(script.namespace));
            assertTrue(script.state(Body.AnklesTied).applied());
            assertTrue(script.state(Toys.Ankle_Restraints).is(script.namespace));
            assertTrue(script.state(Body.AnklesTied).is(script.namespace));

            wristTied.remove();
            wristsCuffed.remove();
            assertFalse(wristTied.applied());

            assertFalse(wristTied.is(script.namespace));
            assertFalse(wristRestraints.applied());
            assertFalse(wristRestraints.is(script.namespace));

            assertTrue(script.state(Body.AnklesTied).applied());
            assertTrue(script.state(Toys.Ankle_Restraints).is(script.namespace));
            assertTrue(script.state(Body.AnklesTied).is(script.namespace));
        }
    }

    @Test
    public void testCanApplyWithoutDefaultsSimulation() throws IOException {
        try (TestScript script = new TestScript()) {
            State wristRestraints = script.state(Toys.Wrist_Restraints);

            wristRestraints.apply();
            assertTrue(wristRestraints.applied());
        }
    }

    @Test
    public void testCanApplyWithDefaultsSimulation() throws IOException {
        try (TestScript script = new TestScript()) {
            State gag = script.state(Toys.Gag);

            gag.applyTo(Body.InMouth);
            assertTrue(gag.applied());
        }
    }

    @Test
    public void testCanApplyWithoutDefaults() throws IOException {
        try (TestScript script = new TestScript()) {
            script.addTestUserItems();

            assertFalse(script.state(Toys.Dildo).applied());

            Item dildo = script.items(Toys.Dildo).item();
            assertFalse(dildo.applied());

            assertFalse(dildo.canApply());
            dildo.setAvailable(true);
            assertTrue(dildo.canApply());
            assertTrue(dildo.is(dildo));

            dildo.apply();

            assertTrue(dildo.applied());
            assertFalse(dildo.canApply());
            assertTrue(dildo.is(dildo));
        }
    }

    @Test
    public void testCanApplyWithDefaults() throws IOException {
        try (TestScript script = new TestScript()) {
            assertFalse(script.state(Toys.Gag).applied());
            assertFalse(script.state(Toys.Gag).is(Body.InMouth));

            Item gag = script.items(Toys.Gag).matching(Toys.Gags.Ball_Gag).item();
            assertNotEquals(Item.NotFound, gag);
            assertFalse(gag.applied());
            assertFalse(gag.canApply());
            gag.setAvailable(true);
            assertTrue(gag.canApply());
            assertFalse(gag.is(Body.InMouth));

            gag.apply();

            assertTrue(gag.applied());
            assertFalse("Already applied", gag.canApply());
            assertTrue(gag.is(Body.InMouth));
        }
    }

    @Test
    public void testCanApplyTo() throws IOException {
        try (TestScript script = new TestScript()) {
            script.addTestUserItems();
            script.addTestUserItems2();
            assertFalse(script.state(Toys.Dildo).applied());

            Item analDildo = script.items(Toys.Dildo).item();
            assertNotEquals(Item.NotFound, analDildo);
            assertFalse(analDildo.applied());

            assertFalse(analDildo.canApply());
            analDildo.setAvailable(true);
            assertTrue(analDildo.canApply());
            assertTrue(analDildo.is(analDildo));

            analDildo.applyTo(Body.InButt);

            assertTrue(analDildo.applied());
            assertFalse(analDildo.canApply());
            assertTrue(analDildo.is(Body.InButt));

            Item oralDildo = script.items(Toys.Dildo).getApplicable().get();
            assertEquals(Item.NotFound, oralDildo);
            script.items(Toys.Dildo).inventory().forEach(item -> item.setAvailable(true));
            oralDildo = script.items(Toys.Dildo).getApplicable().get();
            assertNotEquals(Item.NotFound, oralDildo);
            assertNotEquals(analDildo, oralDildo);
            assertFalse(oralDildo.applied());

            assertTrue(oralDildo.canApply());
            assertTrue(oralDildo.is(oralDildo));

            oralDildo.applyTo(Body.InVagina);

            assertTrue(oralDildo.applied());
            assertFalse(oralDildo.canApply());
            assertTrue(oralDildo.is(Body.InVagina));
        }
    }

    @Test
    public void testApplyExtraStateToState() throws IOException {
        try (TestScript script = new TestScript()) {
            State fooBar = script.state(Foo.Bar);
            State buttPlug = script.state(Toys.Buttplug);

            assertFalse(buttPlug.applied());

            buttPlug.applyTo(Body.InButt);
            fooBar.applyTo(buttPlug);
            assertTrue(buttPlug.applied());
            assertTrue(fooBar.applied());
            assertTrue(buttPlug.is(Foo.Bar));

            buttPlug.remove();
            assertFalse(buttPlug.applied());
            assertFalse(fooBar.applied());
        }
    }

    @Test
    public void testApplyToRemoveFromExtraStateToItem() throws IOException {
        try (TestScript script = new TestScript()) {
            State fooBar = script.state(Foo.Bar);
            Item buttPlug = script.items(Toys.Buttplug).item();

            assertFalse(buttPlug.applied());

            buttPlug.apply();
            fooBar.applyTo(buttPlug);

            assertTrue(buttPlug.applied());
            assertTrue(fooBar.applied());
            assertTrue(buttPlug.is(Foo.Bar));

            fooBar.removeFrom(buttPlug);
            assertFalse(fooBar.applied());

            buttPlug.remove();
            assertFalse(buttPlug.applied());
            State buttPlugState = script.state(Toys.Buttplug);
            assertFalse(buttPlugState.applied());
        }
    }

    @Test
    public void testApplyExtraStateToItemRemoveAll() throws IOException {
        try (TestScript script = new TestScript()) {
            State fooBar = script.state(Foo.Bar);
            Item buttPlug = script.items(Toys.Buttplug).item();

            assertFalse(buttPlug.applied());

            buttPlug.apply();
            fooBar.applyTo(buttPlug);

            assertTrue(buttPlug.applied());
            assertTrue(fooBar.applied());
            assertTrue(buttPlug.is(Foo.Bar));

            buttPlug.remove();
            assertFalse(buttPlug.applied());
            State buttPlugState = script.state(Toys.Buttplug);
            assertFalse(buttPlugState.applied());

            assertFalse(fooBar.applied());
        }
    }

    @Disabled("decide when to throw - possible in dev mode")
    // @Test(expected = IllegalStateException.class)
    public void applyingTwiceRuinsPlausibilityOfActor() throws IOException {
        try (TestScript script = new TestScript()) {
            assertFalse(script.state(Toys.Gag).applied());

            script.item(Toys.Gag).apply();
            assertTrue(script.state(Toys.Gag).applied());

            script.item(Toys.Gag).apply();
            assertTrue(script.state(Toys.Gag).applied());
        }
    }

    @Test
    public void applyingTwiceWorks() throws IOException {
        try (TestScript script = new TestScript()) {
            assertFalse(script.state(Toys.Gag).applied());

            script.item(Toys.Gag).apply();
            assertTrue(script.state(Toys.Gag).applied());

            script.item(Toys.Gag).apply();
            assertTrue(script.state(Toys.Gag).applied());
        }
    }

    @Test
    public void testApplyToAppliesDefaultsAndAttributesPlusCustomPeersWithStrings() throws IOException {
        try (TestScript script = new TestScript()) {
            script.addTestUserItems();

            String Toys_Wrist_Restraints = "teaselib.Toys.Wrist_Restraints";
            String Body_WristsTied = "teaselib.Body.WristsTied";
            String Body_WristsTiedBehindBack = "teaselib.Body.WristsTiedBehindBack";
            String leather = "teaselib.Material.Leather";

            assertFalse(script.state(Toys_Wrist_Restraints).applied());

            // Wrists are not only tied, but also tied behind back

            script.items(Toys_Wrist_Restraints).matching(leather).inventory().applyTo(Body_WristsTiedBehindBack);

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
    }

    @Test
    public void testStringsAndEnumsMixed() throws IOException {
        try (TestScript script = new TestScript()) {
            script.addTestUserItems();

            String Toys_Wrist_Restraints = "teaselib.Toys.Wrist_Restraints";
            String Body_WristsTied = "teaselib.Body.WristsTied";
            String Posture_WristsTiedBehindBack = "teaselib.Posture.WristsTiedBehindBack";
            String leather = "teaselib.Material.Leather";

            assertFalse(script.state(Toys.Wrist_Restraints).applied());
            assertFalse(script.state(Toys_Wrist_Restraints).applied());

            // Wrists are not only tied, but also tied behind back

            script.items(Toys_Wrist_Restraints).matching(leather).inventory().applyTo(Posture_WristsTiedBehindBack);

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
                say("You're wearing leather restraints",
                        script.state(Toys_Wrist_Restraints).is(Body.WristsTied, leather));
            }
        }
    }

    @Test
    public void testTemporaryItems() throws IOException {
        try (TestScript script = new TestScript()) {
            script.addTestUserItems();

            assertFalse(script.state(Toys.Gag).applied());
            assertEquals(0, script.teaseLib.temporaryItems().size());

            Item gag = script.items(Toys.Gag).matching(Toys.Gags.Ball_Gag).item();
            gag.apply();
            assertEquals(1, script.teaseLib.temporaryItems().size());
            gag.remove();
            assertEquals(0, script.teaseLib.temporaryItems().size());
        }
    }

    @Test
    public void testTemporaryItemsRemember() throws IOException {
        try (TestScript script = new TestScript()) {
            script.addTestUserItems();

            assertFalse(script.state(Toys.Gag).applied());
            assertEquals(0, script.teaseLib.temporaryItems().size());

            Item gag = script.item(Toys.Gag);
            gag.apply().over(0, TimeUnit.HOURS).remember(Until.Removed);
            assertTrue(gag.is(Until.Removed));
            // TODO fails because StateImpl.appliedToClass() fails
            assertTrue(gag.is(Until.class));
            assertEquals(0, script.teaseLib.temporaryItems().size());
            gag.remove();
            assertEquals(0, script.teaseLib.temporaryItems().size());
        }
    }

    @Test
    public void testTemporaryItemsIdentity() throws IOException {
        try (TestScript script = new TestScript()) {
            script.addTestUserItems();

            Item wristRestraints = script.items(Toys.Wrist_Restraints).matching(Material.Leather).item();
            wristRestraints.applyTo(Posture.WristsTiedBehindBack);
            Items temporaryItems = script.teaseLib.temporaryItems();
            Item temporaryWristRestraints = temporaryItems.matching(Toys.Wrist_Restraints).get();
            assertEquals(wristRestraints, temporaryWristRestraints);
            assertEquals(1, script.teaseLib.temporaryItems().size());

            wristRestraints.remove();
            assertEquals(0, script.teaseLib.temporaryItems().size());
        }
    }

    @Test
    public void testRemoveOneOfMultipleItemsToSamePeer() throws IOException {
        try (TestScript script = new TestScript()) {
            script.setAvailable(Toys.All, Bondage.All, Accessoires.All);
            var restraints = script.items(Toys.Wrist_Restraints, Toys.Ankle_Restraints, Toys.Collar)
                    .prefer(Material.Leather).getApplicableSet();
            restraints.apply();

            var chains = script.items(Bondage.Chains, Accessoires.Bells).getApplicableSet();
            chains.applyTo(restraints);
            assertTrue(chains.allApplied());
            assertTrue(restraints.allApplied());

            Item singleChainItem = chains.get(Bondage.Chains);
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

            script.items(Toys.Ankle_Restraints, Toys.Collar).getApplied().remove();
            assertFalse(restraints.anyApplied());
        }
    }

    @Test
    public void testIsClass() throws IOException {
        try (TestScript script = new TestScript()) {
            Item woodenSpoon = script.item(Household.Wooden_Spoon);
            assertTrue(woodenSpoon.is(Material.Wood));
            assertTrue(woodenSpoon.is(Material.class));
        }
    }

    @Test
    public void testIsNestedClass() throws IOException {
        try (TestScript script = new TestScript()) {
            script.addTestUserItems();

            Item woodenSpoon = script.item(Household.Wooden_Spoon);
            woodenSpoon.apply().over(0, TimeUnit.HOURS).remember(Until.Removed);
            assertTrue(woodenSpoon.is(Until.Removed));
            assertTrue(woodenSpoon.is(Until.class));
        }
    }

    @Test
    public void testUntilAppliedToStateAndItem() throws IOException {
        try (TestScript script = new TestScript()) {
            Item woodenSpoon = script.item(Household.Wooden_Spoon);
            woodenSpoon.apply().over(1, TimeUnit.HOURS).remember(Until.Expired);
            assertTrue(script.state(Household.Wooden_Spoon).is(Until.Expired));
            assertTrue(woodenSpoon.is(Until.Expired));
        }
    }

    @Test
    public void testRemoveUserItem() throws IOException {
        try (TestScript script = new TestScript()) {
            script.addTestUserItems2();
            Iterator<Item> humblers = script.items(Toys.Humbler).inventory().iterator();
            Item item1 = humblers.next();
            assertEquals("Humbler", item1.displayName());
            Item item2 = humblers.next();
            assertEquals("My Humbler", item2.displayName());
            assertFalse(humblers.hasNext());

            item2.apply().over(1, TimeUnit.HOURS).remember(Until.Removed);
            assertTrue(script.state(Toys.Humbler).is(Until.Removed));
            assertTrue(item2.is(Until.Removed));
            assertFalse(item1.is(Until.Removed));
        }
    }

}
