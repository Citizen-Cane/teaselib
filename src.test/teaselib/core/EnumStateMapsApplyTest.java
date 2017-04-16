package teaselib.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static teaselib.HouseHold.Clothes_Pegs;

import java.util.concurrent.TimeUnit;

import org.junit.Test;

import teaselib.Body;
import teaselib.State;
import teaselib.Toys;
import teaselib.test.TestScript;

public class EnumStateMapsApplyTest extends EnumStateMaps {

    enum Locks {
        Chastity_Device_Lock
    }

    public EnumStateMapsApplyTest() {
        super(TestScript.getOne().teaseLib);
    }

    @Test
    public void testCannotJerkOff() {
        State chastityCage = state(Toys.Chastity_Device);
        State wristRestraints = state(Toys.Wrist_Restraints);

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
        State clothesPins = state(Clothes_Pegs);

        State somethingOnNipples = state(Body.SomethingOnNipples);
        State somethingOnBalls = state(Body.SomethingOnBalls);

        state(Body.SomethingOnNipples).apply(Clothes_Pegs);

        assertTrue(clothesPins.applied());
        assertTrue(somethingOnNipples.applied());
        assertFalse(somethingOnBalls.applied());

        state(Body.SomethingOnBalls).apply(Clothes_Pegs);

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
        State clothesPins = state(Clothes_Pegs);

        State somethingOnNipples = state(Body.SomethingOnNipples);
        State somethingOnBalls = state(Body.SomethingOnBalls);

        state(Clothes_Pegs).apply(Body.SomethingOnBalls, Body.SomethingOnNipples);

        assertTrue(clothesPins.applied());
        assertTrue(somethingOnNipples.applied());
        assertTrue(somethingOnBalls.applied());

        somethingOnBalls.remove(Clothes_Pegs);

        assertTrue(clothesPins.applied());
        assertTrue(somethingOnNipples.applied());
        assertFalse(somethingOnBalls.applied());

        somethingOnNipples.remove(Clothes_Pegs);

        assertFalse(clothesPins.applied());
        assertFalse(somethingOnNipples.applied());
        assertFalse(somethingOnBalls.applied());
    }

    @Test
    public void testLockUp() {
        State chastityCage = state(Toys.Chastity_Device);
        State wristRestraints = state(Toys.Wrist_Restraints);
        State key = state(Locks.Chastity_Device_Lock);

        assertFalse(chastityCage.applied());
        assertFalse(wristRestraints.applied());
        assertFalse(key.applied());

        State somethingOnPenis = state(Body.SomethingOnPenis);
        State cannotJerkOff = state(Body.CannotJerkOff);
        State handsTiedBehindBack = state(Body.WristsTiedBehindBack);

        chastityCage.apply(Body.SomethingOnPenis, Body.CannotJerkOff);
        key.apply(Toys.Chastity_Device);
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
        state(Toys.Chastity_Device).apply(Body.CannotJerkOff);
        state(Locks.Chastity_Device_Lock).apply(Toys.Chastity_Device);

        assertTrue(state(Body.CannotJerkOff).applied());
        assertTrue(state(Toys.Chastity_Device).applied());
        assertTrue(state(Locks.Chastity_Device_Lock).applied());

        state(Locks.Chastity_Device_Lock).remove();
        assertTrue(state(Toys.Chastity_Device).applied());
        assertTrue(state(Body.CannotJerkOff).applied());

        state(Toys.Chastity_Device).remove();
        assertFalse(state(Body.CannotJerkOff).applied());
    }

    @Test
    public void testApplyFromMiddleOfTheHierarchy() {
        state(Toys.Chastity_Device).apply(Body.CannotJerkOff);
        state(Toys.Chastity_Device).apply(Locks.Chastity_Device_Lock);

        assertTrue(state(Body.CannotJerkOff).applied());
        assertTrue(state(Toys.Chastity_Device).applied());
        assertTrue(state(Locks.Chastity_Device_Lock).applied());

        state(Locks.Chastity_Device_Lock).remove();
        assertTrue(state(Toys.Chastity_Device).applied());
        assertTrue(state(Body.CannotJerkOff).applied());

        state(Toys.Chastity_Device).remove();
        assertFalse(state(Body.CannotJerkOff).applied());
    }

    @Test
    public void testApplyItemToMultiplePlacesOneAfterAnother() {
        state(Body.SomethingOnNipples).apply(Clothes_Pegs);

        assertTrue(state(Clothes_Pegs).applied());
        assertTrue(state(Body.SomethingOnNipples).applied());
        assertFalse(state(Body.SomethingOnBalls).applied());

        state(Body.SomethingOnBalls).apply(Clothes_Pegs);

        assertTrue(state(Clothes_Pegs).applied());
        assertTrue(state(Body.SomethingOnNipples).applied());
        assertTrue(state(Body.SomethingOnBalls).applied());
    }

    @Test
    public void testApplyItemToMultiplePlaces() {
        state(Clothes_Pegs).apply(Body.SomethingOnNipples);
        state(Clothes_Pegs).apply(Body.SomethingOnBalls);

        assertTrue(state(Clothes_Pegs).applied());
        assertTrue(state(Body.SomethingOnBalls).applied());
        assertTrue(state(Body.SomethingOnNipples).applied());
    }

    @Test
    public void testApplyItemToMultiplePlacesRemoveAllAtOnce() {
        state(Clothes_Pegs).apply(Body.SomethingOnNipples);
        state(Clothes_Pegs).apply(Body.SomethingOnBalls);

        assertTrue(state(Clothes_Pegs).applied());
        assertTrue(state(Body.SomethingOnBalls).applied());
        assertTrue(state(Body.SomethingOnNipples).applied());

        state(Clothes_Pegs).remove();

        assertFalse(state(Clothes_Pegs).applied());
        assertFalse(state(Body.SomethingOnBalls).applied());
        assertFalse(state(Body.SomethingOnNipples).applied());
    }

    @Test
    public void testApplyItemToMultiplePlacesRemoveOneAfterAnother() {
        state(Clothes_Pegs).apply(Body.SomethingOnNipples);
        state(Clothes_Pegs).apply(Body.SomethingOnBalls);

        assertTrue(state(Clothes_Pegs).applied());
        assertTrue(state(Body.SomethingOnBalls).applied());
        assertTrue(state(Body.SomethingOnNipples).applied());

        state(Clothes_Pegs).remove(Body.SomethingOnBalls);

        assertTrue(state(Clothes_Pegs).applied());
        assertFalse(state(Body.SomethingOnBalls).applied());
        assertTrue(state(Body.SomethingOnNipples).applied());
    }

    @Test
    public void testApplyItemToMultiplePlacesRemoveOneAfterAnotherWithoutKnowingTheActualToy() {
        state(Clothes_Pegs).apply(Body.SomethingOnNipples);
        state(Clothes_Pegs).apply(Body.SomethingOnBalls);

        assertTrue(state(Clothes_Pegs).applied());
        assertTrue(state(Body.SomethingOnBalls).applied());
        assertTrue(state(Body.SomethingOnNipples).applied());

        state(Body.SomethingOnNipples).remove();

        assertTrue(state(Clothes_Pegs).applied());
        assertTrue(state(Body.SomethingOnBalls).applied());
        assertFalse(state(Body.SomethingOnNipples).applied());
    }

    @Test
    public void testThatStateDefaultValueEqualsRemovedALongTimeAgo() {
        assertEquals(0, state(Clothes_Pegs).duration().start(TimeUnit.SECONDS));
        assertEquals(State.REMOVED, state(Clothes_Pegs).duration().limit(TimeUnit.SECONDS));
    }

    @Test
    public void testApplyWithoutPeersWorks() {
        state(Body.SomethingOnBalls).apply();
        assertTrue(state(Body.SomethingOnBalls).applied());
    }

    @Test
    public void testApplyAndRemoveWithSelfReference() {
        assertFalse(state(Toys.Chastity_Device).applied());
        assertFalse(state(Body.SomethingOnPenis).applied());

        state(Toys.Chastity_Device).apply(Body.SomethingOnPenis);

        assertTrue(state(Toys.Chastity_Device).applied());
        assertTrue(state(Body.SomethingOnPenis).applied());

        state(Body.SomethingOnPenis).apply(Body.SomethingOnPenis);
        assertTrue(state(Body.SomethingOnPenis).applied());

        state(Toys.Chastity_Device).remove();
        assertTrue(state(Body.SomethingOnPenis).applied());

        state(Body.SomethingOnPenis).remove();
        assertFalse(state(Body.SomethingOnPenis).applied());
    }
}
