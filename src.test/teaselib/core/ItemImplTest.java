package teaselib.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.junit.Test;
import org.junit.jupiter.api.Disabled;

import teaselib.Accessoires;
import teaselib.Body;
import teaselib.Bondage;
import teaselib.Color;
import teaselib.Features;
import teaselib.Household;
import teaselib.Length;
import teaselib.Material;
import teaselib.Posture;
import teaselib.Size;
import teaselib.State;
import teaselib.State.Persistence.Until;
import teaselib.Toys;
import teaselib.Toys.Gags;
import teaselib.core.state.AbstractProxy;
import teaselib.core.util.QualifiedString;
import teaselib.test.TestScript;
import teaselib.util.Item;
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

    private static Set<QualifiedString> LargeAndLong = new HashSet<>(
            Arrays.asList(QualifiedString.of(Size.Large), QualifiedString.of(Length.Long)));

    @Test
    public void testIs() throws IOException {
        try (TestScript script = new TestScript()) {
            script.teaseLib.addUserItems(Collections.singleton(
                    new ItemImpl(script.teaseLib, TeaseLib.DefaultDomain, QualifiedString.from(Foo.Bar, "Foo_Bar"),
                            "Foo Bar", Collections.emptySet(), LargeAndLong, Collections.emptySet())));
            Item fooBar = script.item(Foo.Bar);

            assertTrue(fooBar.is(Size.Large));
            assertFalse(fooBar.is(Size.Small));
            assertTrue(fooBar.is(Size.Large, Length.Long));
            assertFalse(fooBar.is(Size.Large, Length.Short));
            assertFalse(fooBar.is(Size.Small, Length.Short));

            State inButt = script.state(Body.InButt);
            fooBar.to(inButt).apply();

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
            script.teaseLib.addUserItems(Collections.singleton(
                    new ItemImpl(script.teaseLib, TeaseLib.DefaultDomain, QualifiedString.from(Foo.Bar, "Foo_Bar"),
                            "Foo Bar", Collections.emptySet(), LargeAndLong, Collections.emptySet())));

            Item fooBar = script.item(Foo.Bar);
            State inButt = script.state(Body.InButt);
            fooBar.to(inButt).apply();

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
            Item item = new ItemImpl(script.teaseLib, TeaseLib.DefaultDomain, QualifiedString.from(Foo.Bar, "Foo_Bar"),
                    "Foo Bar", Collections.emptySet(), LargeAndLong, Collections.emptySet());

            assertFalse(item.is());
        }
    }

    @Test
    public void testIsHandlesArrays() throws IOException {
        try (TestScript script = new TestScript()) {
            Item item = new ItemImpl(script.teaseLib, TeaseLib.DefaultDomain, QualifiedString.from(Foo.Bar, "Foo_Bar"),
                    "Foo Bar", Collections.emptySet(), LargeAndLong, Collections.emptySet());

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
    public void testApplyToIgnoresDefaultPeers() throws IOException {
        try (TestScript script = new TestScript()) {
            script.addTestUserItems();

            assertFalse(script.state(Bondage.Wrist_Restraints).applied());

            // Wrists are not only tied, but also tied behind back
            script.items(Bondage.Wrist_Restraints).matching(Material.Leather).inventory()
                    .applyTo(Posture.WristsTiedBehindBack);

            assertFalse(script.state(Body.WristsTied).applied());
            assertTrue(script.state(Posture.WristsTiedBehindBack).applied());
            assertTrue(script.state(Posture.WristsTiedBehindBack).is(Material.Leather));
            assertTrue(script.state(Posture.WristsTiedBehindBack).is(Bondage.Wrist_Restraints));
        }
    }

    @Test
    public void testApplyToAppliesAttributesPlusCustomPeers() throws IOException {
        try (TestScript script = new TestScript()) {
            script.addTestUserItems();

            assertFalse(script.state(Bondage.Wrist_Restraints).applied());

            script.items(Bondage.Ankle_Restraints, Bondage.Wrist_Restraints).matching(Material.Leather).inventory()
                    .applyTo(Body.AnklesCuffed, Body.AnklesTied, Body.WristsCuffed, Body.WristsTied,
                            Posture.WristsTiedToAnkles);

            assertTrue(script.state(Bondage.Ankle_Restraints).applied());
            assertTrue(script.state(Bondage.Wrist_Restraints).applied());
            assertTrue(script.state(Bondage.Ankle_Restraints).is(Material.Leather));
            assertTrue(script.state(Bondage.Wrist_Restraints).is(Material.Leather));

            assertTrue(script.state(Body.AnklesTied).applied());
            assertTrue(script.state(Body.WristsTied).applied());
            assertTrue(script.state(Posture.WristsTiedToAnkles).applied());

            assertTrue(script.state(Body.WristsTied).is(Bondage.Wrist_Restraints));
            assertTrue(script.state(Posture.WristsTiedToAnkles).is(Bondage.Wrist_Restraints));

            script.items(Bondage.Ankle_Restraints, Bondage.Wrist_Restraints).getApplied().remove();
            assertFalse(script.state(Bondage.Ankle_Restraints).applied());
            assertFalse(script.state(Bondage.Wrist_Restraints).applied());
            assertFalse(script.state(Body.WristsTied).applied());
            assertFalse(script.state(Posture.WristsTiedToAnkles).applied());
        }
    }

    private static void say(String message, boolean assertion) {
        assertTrue(message, assertion);
    }

    @Test
    public void testApply1to1AndRemoveTheOtherWayAround() throws IOException {
        try (TestScript script = new TestScript()) {
            script.item(Bondage.Wrist_Restraints).apply();
            assertTrue(script.state(Body.WristsTied).applied());
            assertTrue(script.state(Bondage.Wrist_Restraints).is(script.namespace));
            assertTrue(script.state(Body.WristsTied).is(script.namespace));

            script.state(Body.WristsTied).remove();
            script.state(Body.WristsCuffed).remove();
            assertFalse(script.state(Body.WristsTied).applied());

            assertFalse(script.state(Body.WristsTied).is(script.namespace));
            assertFalse(script.state(Bondage.Wrist_Restraints).applied());
            assertFalse(script.state(Bondage.Wrist_Restraints).is(script.namespace));
        }
    }

    @Test
    public void testApply1toNAndRemoveTheOtherWayAround() throws IOException {
        try (TestScript script = new TestScript()) {
            script.addTestUserItems();
            Items restraints = script.items(Bondage.Wrist_Restraints, Bondage.Ankle_Restraints).inventory();
            restraints.stream().forEach(item -> item.setAvailable(true));
            restraints = restraints.prefer(Material.Metal);

            restraints.apply();

            State wristRestraints = script.state(Bondage.Wrist_Restraints);
            State wristsCuffed = script.state(Body.WristsCuffed);
            State wristTied = script.state(Body.WristsTied);

            assertTrue(wristTied.applied());
            assertTrue(wristRestraints.is(script.namespace));
            assertTrue(wristTied.is(script.namespace));
            assertTrue(script.state(Body.AnklesTied).applied());
            assertTrue(script.state(Bondage.Ankle_Restraints).is(script.namespace));
            assertTrue(script.state(Body.AnklesTied).is(script.namespace));

            wristTied.remove();
            wristsCuffed.remove();
            assertFalse(wristTied.applied());

            assertFalse(wristTied.is(script.namespace));
            assertFalse(wristRestraints.applied());
            assertFalse(wristRestraints.is(script.namespace));

            assertTrue(script.state(Body.AnklesTied).applied());
            assertTrue(script.state(Bondage.Ankle_Restraints).is(script.namespace));
            assertTrue(script.state(Body.AnklesTied).is(script.namespace));
        }
    }

    @Test
    public void testCanApplyWithoutDefaultsSimulation() throws IOException {
        try (TestScript script = new TestScript()) {
            State wristRestraints = script.state(Bondage.Wrist_Restraints);

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

            analDildo.to(Body.InButt).apply();

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

            oralDildo.to(Body.InVagina).apply();

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

            String Bondage_Wrist_Restraints = "teaselib.Bondage.Wrist_Restraints";
            String Body_WristsTied = "teaselib.Body.WristsTied";
            String Body_WristsTiedBehindBack = "teaselib.Body.WristsTiedBehindBack";
            String leather = "teaselib.Material.Leather";

            assertFalse(script.state(Bondage_Wrist_Restraints).applied());

            script.items(Bondage_Wrist_Restraints).matching(leather).inventory().applyTo(Body_WristsTiedBehindBack);

            assertTrue(script.state(Bondage_Wrist_Restraints).applied());
            assertTrue(script.state(Bondage_Wrist_Restraints).is(leather));

            assertFalse(script.state(Body_WristsTied).applied());
            assertTrue(script.state(Body_WristsTiedBehindBack).applied());

            assertFalse(script.state(Body_WristsTied).is(Bondage_Wrist_Restraints));
            assertTrue(script.state(Body_WristsTiedBehindBack).is(Bondage_Wrist_Restraints));

            // This is how to comment a certain item in a certain body location
            if (script.state(Body_WristsTied).is(Bondage_Wrist_Restraints)) {
                if (script.item(Bondage_Wrist_Restraints).is(leather)) {
                    say("You're wearing leather restraints", script.state(Bondage_Wrist_Restraints).is(leather));
                }
            }
        }
    }

    @Test
    public void testStringsAndEnumsMixedApplyTo() throws IOException {
        try (TestScript script = new TestScript()) {
            script.addTestUserItems();

            String Bondage_Wrist_Restraints = "teaselib.Bondage.Wrist_Restraints";
            String Body_WristsTied = "teaselib.Body.WristsTied";
            String Posture_WristsTiedBehindBack = "teaselib.Posture.WristsTiedBehindBack";
            String leather = "teaselib.Material.Leather";

            assertFalse(script.state(Bondage.Wrist_Restraints).applied());
            assertFalse(script.state(Bondage_Wrist_Restraints).applied());

            // Wrists are not only tied, but also tied behind back

            script.items(Bondage_Wrist_Restraints).matching(leather).inventory().applyTo(Posture_WristsTiedBehindBack);

            assertTrue(script.state(Bondage.Wrist_Restraints).applied());
            assertTrue(script.state(Bondage_Wrist_Restraints).is(leather));
            assertTrue(script.state(Bondage_Wrist_Restraints).is(Material.Leather));
            assertTrue(script.state(Bondage.Wrist_Restraints).applied());
            assertTrue(script.state(Bondage_Wrist_Restraints).is(leather));
            assertTrue(script.state(Bondage_Wrist_Restraints).is(Material.Leather));

            assertTrue(script.state(Posture_WristsTiedBehindBack).applied());
            assertTrue(script.state(Posture.WristsTiedBehindBack).applied());

            assertTrue(script.state(Posture.WristsTiedBehindBack).is(Bondage.Wrist_Restraints));
            assertTrue(script.state(Posture_WristsTiedBehindBack).is(Bondage.Wrist_Restraints));

            assertTrue(script.state(Posture.WristsTiedBehindBack).is(Bondage_Wrist_Restraints));
            assertTrue(script.state(Posture_WristsTiedBehindBack).is(Bondage_Wrist_Restraints));
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

            Item wristRestraints = script.items(Bondage.Wrist_Restraints).matching(Material.Leather).item();
            wristRestraints.to(Posture.WristsTiedBehindBack).apply();
            Items temporaryItems = script.teaseLib.temporaryItems();
            Item temporaryWristRestraints = temporaryItems.matching(Bondage.Wrist_Restraints).get();
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
            var restraints = script.items(Bondage.Wrist_Restraints, Bondage.Ankle_Restraints, Toys.Collar)
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

            Item wristRestraints = restraints.get(Bondage.Wrist_Restraints);
            // TODO remove immediately removes the bell guid,
            // but the bell is still attached to ankles and collar
            wristRestraints.remove();
            assertTrue(chains.anyApplied());
            assertFalse(restraints.allApplied());

            bell.remove();
            assertFalse(chains.anyApplied());

            script.items(Bondage.Ankle_Restraints, Toys.Collar).getApplied().remove();
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

    @Test
    public void testToState() throws IOException {
        try (TestScript script = new TestScript()) {
            Item leash = script.item(Household.Leash);
            leash.to(Toys.Collar).apply();
            assertTrue(script.item(Household.Leash).applied());
            assertFalse(script.item(Toys.Collar).applied());
            assertTrue(script.state(Toys.Collar).applied());

            // Not applied to specific item
            assertFalse(script.item(Toys.Collar).is(Household.Leash));
            assertTrue(script.state(Toys.Collar).is(Household.Leash));

            script.item(Toys.Collar).apply();
            assertTrue(script.item(Household.Leash).applied());
            assertTrue(script.item(Toys.Collar).applied());
            assertTrue(script.state(Toys.Collar).applied());
            assertTrue(script.item(Toys.Collar).is(Household.Leash));
            assertTrue(script.state(Toys.Collar).is(Household.Leash));
        }
    }

    @Test
    public void testApplyToItem() throws IOException {
        try (TestScript script = new TestScript()) {
            Item leash = script.item(Household.Leash);
            Item collar = script.item(Toys.Collar);

            leash.applyTo(collar);
            assertTrue(script.item(Household.Leash).applied());

            assertTrue(collar.applied()); // s.o. applied to collar
            assertFalse(collar.is(Body.AroundNeck)); // but collar not applied to body (yet)
            assertTrue(script.state(Toys.Collar).applied());

            assertTrue(leash.is(collar));
            assertTrue(collar.is(leash));
            assertTrue(collar.is(Household.Leash));
            assertTrue(script.state(Toys.Collar).is(Household.Leash));

            collar.apply();
            assertTrue(script.item(Household.Leash).applied());
            assertTrue(collar.applied());
            assertTrue(collar.is(Body.class));
            assertTrue(leash.is(collar));
            assertTrue(collar.is(leash));
            assertTrue(collar.is(Household.Leash));
            assertTrue(script.state(Toys.Collar).is(Household.Leash));
        }
    }

    @Test
    public void testToItem() throws IOException {
        try (TestScript script = new TestScript()) {
            Item leash = script.item(Household.Leash);
            Item collar = script.item(Toys.Collar);

            leash.to(collar).apply();
            assertTrue(script.item(Household.Leash).applied());

            assertTrue(collar.applied()); // s.o. applied to collar
            assertFalse(collar.is(Body.class)); // but collar not applied to body (yet)
            assertTrue(script.state(Toys.Collar).applied());

            assertTrue(script.state(Toys.Collar).applied());
            assertTrue(leash.is(collar));
            assertTrue(collar.is(leash));
            assertTrue(collar.is(Household.Leash));
            assertTrue(script.state(Toys.Collar).is(Household.Leash));

            collar.apply();
            assertTrue(script.item(Household.Leash).applied());
            assertTrue(collar.applied());
            assertTrue(collar.is(Body.class));
            assertTrue(leash.is(collar));
            assertTrue(collar.is(leash));
            assertTrue(collar.is(Household.Leash));
            assertTrue(script.state(Toys.Collar).is(Household.Leash));
        }
    }

    @Test
    public void testApplyToNotAppliedItem() throws IOException {
        try (TestScript script = new TestScript()) {
            var gags = script.items(Toys.Gag);
            Item gag = gags.inventory().get(0);
            Item unusedGag = gags.inventory().get(1);
            assertNotEquals(gag, unusedGag);

            Item chains = script.item(Bondage.Chains);
            chains.applyTo(gag);
            assertTrue(chains.applied());

            assertTrue(gag.applied()); // <- s.o. applied to gag (leash)
            assertFalse(gag.is(Body.class)); // but gag not applied to body (yet)

            assertTrue(chains.is(gag));
            assertTrue(gag.is(chains));
            assertFalse(chains.is(unusedGag));
            assertFalse(unusedGag.is(chains));
        }
    }

    @Test
    public void testToNotAppliedItem() throws IOException {
        try (TestScript script = new TestScript()) {
            var gags = script.items(Toys.Gag);
            Item gag = gags.inventory().get(0);
            Item unusedGag = gags.inventory().get(1);
            assertNotEquals(gag, unusedGag);

            Item chains = script.item(Bondage.Chains);
            chains.to(gag).apply();
            assertTrue(chains.applied());

            assertTrue(gag.applied()); // <- s.o. applied to gag (leash)
            assertFalse(gag.is(Body.class)); // but gag not applied to body

            assertTrue(chains.is(gag));
            assertTrue(gag.is(chains));
            assertFalse(chains.is(unusedGag));
            assertFalse(unusedGag.is(chains));
        }
    }

    @Test
    public void testApplyToAppliedItem() throws IOException {
        try (TestScript script = new TestScript()) {
            var gags = script.items(Toys.Gag);
            Item ballGag = gags.matching(Gags.Ball_Gag).inventory().get();
            Item ringGag = gags.matching(Gags.Ring_Gag).inventory().get();
            assertNotEquals(ballGag, ringGag);

            ballGag.apply();
            Item chains = script.item(Bondage.Chains);
            chains.to(ballGag).apply();
            assertTrue(chains.is(ballGag));
            assertFalse(chains.is(ringGag));
            assertTrue(ballGag.is(chains));
            assertFalse(ringGag.is(chains));

            assertTrue(ballGag.is(Body.class));
            assertFalse(ringGag.is(Body.class));
            assertFalse(chains.is(Body.class)); // applied to ball gag but not to body
        }
    }

    @Test
    public void testApplyBondageCuffs() throws IOException {
        try (TestScript script = new TestScript()) {
            script.setAvailable(Bondage.Wrist_Restraints);
            var wristCuffs = script.items(Bondage.Wrist_Restraints).matching(Features.Detachable).getApplicable().get();

            // Only cuff the wrists but do not tie them together - leave that for later
            wristCuffs.applyTo(Body.WristsCuffed);
            assertTrue(wristCuffs.applied());

            var wristRestraints = script.item(Bondage.Wrist_Restraints);
            assertSame(AbstractProxy.removeProxy(wristRestraints), AbstractProxy.removeProxy(wristCuffs));

            assertTrue(wristRestraints.canApply());
            // Still applicable because we only applied the cuffs to the wrists
            // but did not tie the wrists together
            assertTrue(wristRestraints.applied());

            assertTrue(wristRestraints.is(Body.WristsCuffed));
            assertFalse(wristRestraints.is(Body.WristsTied));
            assertFalse(wristRestraints.is(Posture.WristsTiedBehindBack));
            assertFalse(wristRestraints.is(Posture.WristsTiedToAnkles));
            assertFalse(wristRestraints.is(Posture.WristsTiedInFront));

            // Tied wrist together using default & custom peers
            wristRestraints.to(Posture.WristsTiedInFront).apply();
            assertTrue(wristRestraints.is(Body.WristsTied));
            assertFalse(wristRestraints.is(Posture.WristsTiedBehindBack));
            assertFalse(wristRestraints.is(Posture.WristsTiedToAnkles));
            assertTrue(wristRestraints.is(Posture.WristsTiedInFront));
        }
    }

}
