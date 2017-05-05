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

public class StateMapsApplyTest extends StateMaps {
    public static final String TEST_DOMAIN = "test";

    enum Locks {
        Chastity_Device_Lock
    }

    public StateMapsApplyTest() {
        super(TestScript.getOne().teaseLib);
    }

    @Test
    public void testCannotJerkOff() {
        State chastityCage = state(TEST_DOMAIN, Toys.Chastity_Device);
        State wristRestraints = state(TEST_DOMAIN, Toys.Wrist_Restraints);

        assertFalse(chastityCage.applied());
        assertFalse(wristRestraints.applied());

        State somethingOnPenis = state(TEST_DOMAIN, Body.SomethingOnPenis);
        State cannotJerkOff = state(TEST_DOMAIN, Body.CannotJerkOff);
        State handsTiedBehindBack = state(TEST_DOMAIN, Body.WristsTiedBehindBack);

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
        State clothesPins = state(TEST_DOMAIN, Clothes_Pegs);

        State somethingOnNipples = state(TEST_DOMAIN, Body.SomethingOnNipples);
        State somethingOnBalls = state(TEST_DOMAIN, Body.SomethingOnBalls);

        state(TEST_DOMAIN, Body.SomethingOnNipples).apply(Clothes_Pegs);

        assertTrue(clothesPins.applied());
        assertTrue(somethingOnNipples.applied());
        assertFalse(somethingOnBalls.applied());

        state(TEST_DOMAIN, Body.SomethingOnBalls).apply(Clothes_Pegs);

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
        State clothesPins = state(TEST_DOMAIN, Clothes_Pegs);

        State somethingOnNipples = state(TEST_DOMAIN, Body.SomethingOnNipples);
        State somethingOnBalls = state(TEST_DOMAIN, Body.SomethingOnBalls);

        state(TEST_DOMAIN, Clothes_Pegs).apply(Body.SomethingOnBalls, Body.SomethingOnNipples);

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
        State chastityCage = state(TEST_DOMAIN, Toys.Chastity_Device);
        State wristRestraints = state(TEST_DOMAIN, Toys.Wrist_Restraints);
        State key = state(TEST_DOMAIN, Locks.Chastity_Device_Lock);

        assertFalse(chastityCage.applied());
        assertFalse(wristRestraints.applied());
        assertFalse(key.applied());

        State somethingOnPenis = state(TEST_DOMAIN, Body.SomethingOnPenis);
        State cannotJerkOff = state(TEST_DOMAIN, Body.CannotJerkOff);
        State handsTiedBehindBack = state(TEST_DOMAIN, Body.WristsTiedBehindBack);

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
        state(TEST_DOMAIN, Toys.Chastity_Device).apply(Body.CannotJerkOff);
        state(TEST_DOMAIN, Locks.Chastity_Device_Lock).apply(Toys.Chastity_Device);

        assertTrue(state(TEST_DOMAIN, Body.CannotJerkOff).applied());
        assertTrue(state(TEST_DOMAIN, Toys.Chastity_Device).applied());
        assertTrue(state(TEST_DOMAIN, Locks.Chastity_Device_Lock).applied());

        state(TEST_DOMAIN, Locks.Chastity_Device_Lock).remove();
        assertTrue(state(TEST_DOMAIN, Toys.Chastity_Device).applied());
        assertTrue(state(TEST_DOMAIN, Body.CannotJerkOff).applied());

        state(TEST_DOMAIN, Toys.Chastity_Device).remove();
        assertFalse(state(TEST_DOMAIN, Body.CannotJerkOff).applied());
    }

    @Test
    public void testApplyFromMiddleOfTheHierarchy() {
        state(TEST_DOMAIN, Toys.Chastity_Device).apply(Body.CannotJerkOff);
        state(TEST_DOMAIN, Toys.Chastity_Device).apply(Locks.Chastity_Device_Lock);

        assertTrue(state(TEST_DOMAIN, Body.CannotJerkOff).applied());
        assertTrue(state(TEST_DOMAIN, Toys.Chastity_Device).applied());
        assertTrue(state(TEST_DOMAIN, Locks.Chastity_Device_Lock).applied());

        state(TEST_DOMAIN, Locks.Chastity_Device_Lock).remove();
        assertTrue(state(TEST_DOMAIN, Toys.Chastity_Device).applied());
        assertTrue(state(TEST_DOMAIN, Body.CannotJerkOff).applied());

        state(TEST_DOMAIN, Toys.Chastity_Device).remove();
        assertFalse(state(TEST_DOMAIN, Body.CannotJerkOff).applied());
    }

    @Test
    public void testApplyItemToMultiplePlacesOneAfterAnother() {
        state(TEST_DOMAIN, Body.SomethingOnNipples).apply(Clothes_Pegs);

        assertTrue(state(TEST_DOMAIN, Clothes_Pegs).applied());
        assertTrue(state(TEST_DOMAIN, Body.SomethingOnNipples).applied());
        assertFalse(state(TEST_DOMAIN, Body.SomethingOnBalls).applied());

        state(TEST_DOMAIN, Body.SomethingOnBalls).apply(Clothes_Pegs);

        assertTrue(state(TEST_DOMAIN, Clothes_Pegs).applied());
        assertTrue(state(TEST_DOMAIN, Body.SomethingOnNipples).applied());
        assertTrue(state(TEST_DOMAIN, Body.SomethingOnBalls).applied());
    }

    @Test
    public void testApplyItemToMultiplePlaces() {
        state(TEST_DOMAIN, Clothes_Pegs).apply(Body.SomethingOnNipples);
        state(TEST_DOMAIN, Clothes_Pegs).apply(Body.SomethingOnBalls);

        assertTrue(state(TEST_DOMAIN, Clothes_Pegs).applied());
        assertTrue(state(TEST_DOMAIN, Body.SomethingOnBalls).applied());
        assertTrue(state(TEST_DOMAIN, Body.SomethingOnNipples).applied());
    }

    @Test
    public void testApplyItemToMultiplePlacesRemoveAllAtOnce() {
        state(TEST_DOMAIN, Clothes_Pegs).apply(Body.SomethingOnNipples);
        state(TEST_DOMAIN, Clothes_Pegs).apply(Body.SomethingOnBalls);

        assertTrue(state(TEST_DOMAIN, Clothes_Pegs).applied());
        assertTrue(state(TEST_DOMAIN, Body.SomethingOnBalls).applied());
        assertTrue(state(TEST_DOMAIN, Body.SomethingOnNipples).applied());

        state(TEST_DOMAIN, Clothes_Pegs).remove();

        assertFalse(state(TEST_DOMAIN, Clothes_Pegs).applied());
        assertFalse(state(TEST_DOMAIN, Body.SomethingOnBalls).applied());
        assertFalse(state(TEST_DOMAIN, Body.SomethingOnNipples).applied());
    }

    @Test
    public void testApplyItemToMultiplePlacesRemoveOneAfterAnother() {
        state(TEST_DOMAIN, Clothes_Pegs).apply(Body.SomethingOnNipples);
        state(TEST_DOMAIN, Clothes_Pegs).apply(Body.SomethingOnBalls);

        assertTrue(state(TEST_DOMAIN, Clothes_Pegs).applied());
        assertTrue(state(TEST_DOMAIN, Body.SomethingOnBalls).applied());
        assertTrue(state(TEST_DOMAIN, Body.SomethingOnNipples).applied());

        state(TEST_DOMAIN, Clothes_Pegs).remove(Body.SomethingOnBalls);

        assertTrue(state(TEST_DOMAIN, Clothes_Pegs).applied());
        assertFalse(state(TEST_DOMAIN, Body.SomethingOnBalls).applied());
        assertTrue(state(TEST_DOMAIN, Body.SomethingOnNipples).applied());
    }

    @Test
    public void testApplyItemToMultiplePlacesRemoveOneAfterAnotherWithoutKnowingTheActualToy() {
        state(TEST_DOMAIN, Clothes_Pegs).apply(Body.SomethingOnNipples);
        state(TEST_DOMAIN, Clothes_Pegs).apply(Body.SomethingOnBalls);

        assertTrue(state(TEST_DOMAIN, Clothes_Pegs).applied());
        assertTrue(state(TEST_DOMAIN, Body.SomethingOnBalls).applied());
        assertTrue(state(TEST_DOMAIN, Body.SomethingOnNipples).applied());

        state(TEST_DOMAIN, Body.SomethingOnNipples).remove();

        assertTrue(state(TEST_DOMAIN, Clothes_Pegs).applied());
        assertTrue(state(TEST_DOMAIN, Body.SomethingOnBalls).applied());
        assertFalse(state(TEST_DOMAIN, Body.SomethingOnNipples).applied());
    }

    @Test
    public void testThatStateDefaultValueEqualsRemovedALongTimeAgo() {
        assertEquals(0, state(TEST_DOMAIN, Clothes_Pegs).duration().start(TimeUnit.SECONDS));
        assertEquals(State.REMOVED,
                state(TEST_DOMAIN, Clothes_Pegs).duration().limit(TimeUnit.SECONDS));
    }

    @Test
    public void testApplyWithoutPeersWorks() {
        state(TEST_DOMAIN, Body.SomethingOnBalls).apply();
        assertTrue(state(TEST_DOMAIN, Body.SomethingOnBalls).applied());
    }

    @Test
    public void testApplyAndRemoveWithSelfReference() {
        assertFalse(state(TEST_DOMAIN, Toys.Chastity_Device).applied());
        assertFalse(state(TEST_DOMAIN, Body.SomethingOnPenis).applied());

        state(TEST_DOMAIN, Toys.Chastity_Device).apply(Body.SomethingOnPenis);

        assertTrue(state(TEST_DOMAIN, Toys.Chastity_Device).applied());
        assertTrue(state(TEST_DOMAIN, Body.SomethingOnPenis).applied());

        state(TEST_DOMAIN, Body.SomethingOnPenis).apply(Body.SomethingOnPenis);
        assertTrue(state(TEST_DOMAIN, Body.SomethingOnPenis).applied());

        state(TEST_DOMAIN, Toys.Chastity_Device).remove();
        assertTrue(state(TEST_DOMAIN, Body.SomethingOnPenis).applied());

        state(TEST_DOMAIN, Body.SomethingOnPenis).remove();
        assertFalse(state(TEST_DOMAIN, Body.SomethingOnPenis).applied());
    }
}
