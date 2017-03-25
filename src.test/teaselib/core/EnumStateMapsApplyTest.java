package teaselib.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static teaselib.Toys.Clothespins;

import java.util.concurrent.TimeUnit;

import org.junit.Test;

import teaselib.Body;
import teaselib.State;
import teaselib.Toys;
import teaselib.test.TestScript;

public class EnumStateMapsApplyTest extends EnumStateMaps {

    public EnumStateMapsApplyTest() {
        super(TestScript.getOne().teaseLib);
    }

    @Test
    public void testCannotJerkOff() {
        State chastityCage = state(Toys.Chastity_Cage);
        State wristRestraints = state(Toys.Leather_Wrist_Cuffs);

        assertFalse(chastityCage.applied());
        assertFalse(wristRestraints.applied());

        State somethingOnPenis = state(Body.SomethingOnPenis);
        State cannotJerkOff = state(Body.CannotJerkOff);
        State handsTiedBehindBack = state(Body.WristsTiedBehindBack);

        chastityCage.apply(Body.SomethingOnPenis, Body.CannotJerkOff);
        wristRestraints.apply(Body.WristsTiedBehindBack, Body.CannotJerkOff);

        assertTrue(chastityCage.applied());
        assertTrue(somethingOnPenis.applied());
        assertTrue(cannotJerkOff.applied());

        assertTrue(wristRestraints.applied());
        assertTrue(handsTiedBehindBack.applied());

        chastityCage.remove();

        assertFalse(chastityCage.applied());
        assertFalse(somethingOnPenis.applied());
        assertTrue(cannotJerkOff.applied());

        assertTrue(wristRestraints.applied());
        assertTrue(handsTiedBehindBack.applied());

        wristRestraints.remove();

        assertFalse(wristRestraints.applied());
        assertFalse(handsTiedBehindBack.applied());
    }

    @Test
    public void testPegsOnNipplesAndNutsApplyOneAfterAnother() {
        State clothesPins = state(Toys.Clothespins);

        State somethingOnNipples = state(Body.SomethingOnNipples);
        State somethingOnBalls = state(Body.SomethingOnBalls);

        state(Body.SomethingOnNipples).apply(Toys.Clothespins);

        assertTrue(clothesPins.applied());
        assertTrue(somethingOnNipples.applied());
        assertFalse(somethingOnBalls.applied());

        state(Body.SomethingOnBalls).apply(Toys.Clothespins);

        assertTrue(clothesPins.applied());
        assertTrue(somethingOnNipples.applied());
        assertTrue(somethingOnBalls.applied());

        clothesPins.remove();

        assertFalse(clothesPins.applied());
        assertFalse(somethingOnNipples.applied());
        assertFalse(somethingOnBalls.applied());
    }

    @Test
    public void testPegsOnNipplesAndNutsRemoveOneAfterAnother() {
        State clothesPins = state(Toys.Clothespins);

        State somethingOnNipples = state(Body.SomethingOnNipples);
        State somethingOnBalls = state(Body.SomethingOnBalls);

        state(Clothespins).apply(Body.SomethingOnBalls,
                Body.SomethingOnNipples);

        assertTrue(clothesPins.applied());
        assertTrue(somethingOnNipples.applied());
        assertTrue(somethingOnBalls.applied());

        somethingOnBalls.remove(Toys.Clothespins);

        assertTrue(clothesPins.applied());
        assertTrue(somethingOnNipples.applied());
        assertFalse(somethingOnBalls.applied());

        somethingOnNipples.remove(Toys.Clothespins);

        assertFalse(clothesPins.applied());
        assertFalse(somethingOnNipples.applied());
        assertFalse(somethingOnBalls.applied());
    }

    @Test
    public void testLockUp() {
        State chastityCage = state(Toys.Chastity_Cage);
        State wristRestraints = state(Toys.Leather_Wrist_Cuffs);
        State key = state(Toys.Chastity_Device_Lock);

        assertFalse(chastityCage.applied());
        assertFalse(wristRestraints.applied());
        assertFalse(key.applied());

        State somethingOnPenis = state(Body.SomethingOnPenis);
        State cannotJerkOff = state(Body.CannotJerkOff);
        State handsTiedBehindBack = state(Body.WristsTiedBehindBack);

        chastityCage.apply(Body.SomethingOnPenis, Body.CannotJerkOff);
        key.apply(Toys.Chastity_Cage);
        wristRestraints.apply(Body.WristsTiedBehindBack, Body.CannotJerkOff);

        assertTrue(chastityCage.applied());
        assertTrue(key.applied());
        assertTrue(somethingOnPenis.applied());
        assertTrue(cannotJerkOff.applied());

        assertTrue(wristRestraints.applied());
        assertTrue(handsTiedBehindBack.applied());

        key.remove();

        assertFalse(key.applied());

        assertTrue(chastityCage.applied());
        assertTrue(somethingOnPenis.applied());
        assertTrue(cannotJerkOff.applied());

        assertTrue(wristRestraints.applied());
        assertTrue(handsTiedBehindBack.applied());

        chastityCage.remove();

        assertFalse(chastityCage.applied());
        assertFalse(somethingOnPenis.applied());
        assertTrue(cannotJerkOff.applied());

        assertTrue(wristRestraints.applied());
        assertTrue(handsTiedBehindBack.applied());

        wristRestraints.remove();

        assertFalse(wristRestraints.applied());
        assertFalse(handsTiedBehindBack.applied());
    }

    @Test
    public void testApplyHierarchically() {
        state(Toys.Chastity_Cage).apply(Body.CannotJerkOff);
        state(Toys.Chastity_Device_Lock).apply(Toys.Chastity_Cage);

        assertTrue(state(Body.CannotJerkOff).applied());
        assertTrue(state(Toys.Chastity_Cage).applied());
        assertTrue(state(Toys.Chastity_Device_Lock).applied());

        state(Toys.Chastity_Device_Lock).remove();
        assertTrue(state(Toys.Chastity_Cage).applied());
        assertTrue(state(Body.CannotJerkOff).applied());

        state(Toys.Chastity_Cage).remove();
        assertFalse(state(Body.CannotJerkOff).applied());
    }

    @Test
    public void testApplyFromMiddleOfTheHierarchy() {
        state(Toys.Chastity_Cage).apply(Body.CannotJerkOff);
        state(Toys.Chastity_Cage).apply(Toys.Chastity_Device_Lock);

        assertTrue(state(Body.CannotJerkOff).applied());
        assertTrue(state(Toys.Chastity_Cage).applied());
        assertTrue(state(Toys.Chastity_Device_Lock).applied());

        state(Toys.Chastity_Device_Lock).remove();
        assertTrue(state(Toys.Chastity_Cage).applied());
        assertTrue(state(Body.CannotJerkOff).applied());

        state(Toys.Chastity_Cage).remove();
        assertFalse(state(Body.CannotJerkOff).applied());
    }

    @Test
    public void testApplyItemToMultiplePlacesOneAfterAnother() {
        state(Body.SomethingOnNipples).apply(Toys.Clothespins);

        assertTrue(state(Toys.Clothespins).applied());
        assertTrue(state(Body.SomethingOnNipples).applied());
        assertFalse(state(Body.SomethingOnBalls).applied());

        state(Body.SomethingOnBalls).apply(Toys.Clothespins);

        assertTrue(state(Toys.Clothespins).applied());
        assertTrue(state(Body.SomethingOnNipples).applied());
        assertTrue(state(Body.SomethingOnBalls).applied());
    }

    @Test
    public void testApplyItemToMultiplePlaces() {
        state(Toys.Clothespins).apply(Body.SomethingOnNipples);
        state(Toys.Clothespins).apply(Body.SomethingOnBalls);

        assertTrue(state(Toys.Clothespins).applied());
        assertTrue(state(Body.SomethingOnBalls).applied());
        assertTrue(state(Body.SomethingOnNipples).applied());
    }

    @Test
    public void testApplyItemToMultiplePlacesRemoveAllAtOnce() {
        state(Toys.Clothespins).apply(Body.SomethingOnNipples);
        state(Toys.Clothespins).apply(Body.SomethingOnBalls);

        assertTrue(state(Toys.Clothespins).applied());
        assertTrue(state(Body.SomethingOnBalls).applied());
        assertTrue(state(Body.SomethingOnNipples).applied());

        state(Toys.Clothespins).remove();

        assertFalse(state(Toys.Clothespins).applied());
        assertFalse(state(Body.SomethingOnBalls).applied());
        assertFalse(state(Body.SomethingOnNipples).applied());
    }

    @Test
    public void testApplyItemToMultiplePlacesRemoveOneAfterAnother() {
        state(Toys.Clothespins).apply(Body.SomethingOnNipples);
        state(Toys.Clothespins).apply(Body.SomethingOnBalls);

        assertTrue(state(Toys.Clothespins).applied());
        assertTrue(state(Body.SomethingOnBalls).applied());
        assertTrue(state(Body.SomethingOnNipples).applied());

        state(Toys.Clothespins).remove(Body.SomethingOnBalls);

        assertTrue(state(Toys.Clothespins).applied());
        assertFalse(state(Body.SomethingOnBalls).applied());
        assertTrue(state(Body.SomethingOnNipples).applied());
    }

    @Test
    public void testApplyItemToMultiplePlacesRemoveOneAfterAnotherWithoutKnowingTheActualToy() {
        state(Toys.Clothespins).apply(Body.SomethingOnNipples);
        state(Toys.Clothespins).apply(Body.SomethingOnBalls);

        assertTrue(state(Toys.Clothespins).applied());
        assertTrue(state(Body.SomethingOnBalls).applied());
        assertTrue(state(Body.SomethingOnNipples).applied());

        state(Body.SomethingOnNipples).remove();

        assertTrue(state(Toys.Clothespins).applied());
        assertTrue(state(Body.SomethingOnBalls).applied());
        assertFalse(state(Body.SomethingOnNipples).applied());
    }

    @Test
    public void testThatStateDefaultValueEqualsRemovedALongTimeAgo() {
        assertEquals(0,
                state(Toys.Clothespins).duration().start(TimeUnit.SECONDS));
        assertEquals(State.REMOVED,
                state(Toys.Clothespins).duration().limit(TimeUnit.SECONDS));
    }

    @Test
    public void testApplyWithoutPeersWorks() {
        state(Body.SomethingOnBalls).apply();
        assertTrue(state(Body.SomethingOnBalls).applied());
    }

    @Test
    public void testApplyAndRemoveWithSelfReference() {
        assertFalse(state(Toys.Chastity_Cage).applied());
        assertFalse(state(Body.SomethingOnPenis).applied());

        state(Toys.Chastity_Cage).apply(Body.SomethingOnPenis);

        assertTrue(state(Toys.Chastity_Cage).applied());
        assertTrue(state(Body.SomethingOnPenis).applied());

        state(Body.SomethingOnPenis).apply(Body.SomethingOnPenis);
        assertTrue(state(Body.SomethingOnPenis).applied());

        state(Toys.Chastity_Cage).remove();
        assertTrue(state(Body.SomethingOnPenis).applied());

        state(Body.SomethingOnPenis).remove();
        assertFalse(state(Body.SomethingOnPenis).applied());
    }
}
