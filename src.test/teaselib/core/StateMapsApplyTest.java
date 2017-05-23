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

        State somethingOnPenis = state(TEST_DOMAIN, Body.OnPenis);
        State cannotJerkOff = state(TEST_DOMAIN, Body.CantJerkOff);
        State handsTiedBehindBack = state(TEST_DOMAIN, Body.WristsTiedBehindBack);

        chastityCage.apply(Body.OnPenis, Body.CantJerkOff);
        wristRestraints.apply(Body.WristsTiedBehindBack, Body.CantJerkOff);

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

        State somethingOnNipples = state(TEST_DOMAIN, Body.OnNipples);
        State somethingOnBalls = state(TEST_DOMAIN, Body.OnBalls);

        state(TEST_DOMAIN, Body.OnNipples).apply(Clothes_Pegs);

        assertTrue(clothesPins.applied());
        assertTrue(somethingOnNipples.applied());
        assertFalse(somethingOnBalls.applied());

        state(TEST_DOMAIN, Body.OnBalls).apply(Clothes_Pegs);

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

        State somethingOnNipples = state(TEST_DOMAIN, Body.OnNipples);
        State somethingOnBalls = state(TEST_DOMAIN, Body.OnBalls);

        state(TEST_DOMAIN, Clothes_Pegs).apply(Body.OnBalls, Body.OnNipples);

        assertTrue(clothesPins.applied());
        assertTrue(somethingOnNipples.applied());
        assertTrue(somethingOnBalls.applied());

        somethingOnBalls.remove();

        assertTrue(clothesPins.applied());
        assertTrue(somethingOnNipples.applied());
        assertFalse(somethingOnBalls.applied());

        somethingOnNipples.remove();

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

        State somethingOnPenis = state(TEST_DOMAIN, Body.OnPenis);
        State cannotJerkOff = state(TEST_DOMAIN, Body.CantJerkOff);
        State handsTiedBehindBack = state(TEST_DOMAIN, Body.WristsTiedBehindBack);

        chastityCage.apply(Body.OnPenis, Body.CantJerkOff);
        key.apply(Toys.Chastity_Device);
        wristRestraints.apply(Body.WristsTiedBehindBack, Body.CantJerkOff);

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
        state(TEST_DOMAIN, Toys.Chastity_Device).apply(Body.CantJerkOff);
        state(TEST_DOMAIN, Locks.Chastity_Device_Lock).apply(Toys.Chastity_Device);

        assertTrue(state(TEST_DOMAIN, Body.CantJerkOff).applied());
        assertTrue(state(TEST_DOMAIN, Toys.Chastity_Device).applied());
        assertTrue(state(TEST_DOMAIN, Locks.Chastity_Device_Lock).applied());

        state(TEST_DOMAIN, Locks.Chastity_Device_Lock).remove();
        assertTrue(state(TEST_DOMAIN, Toys.Chastity_Device).applied());
        assertTrue(state(TEST_DOMAIN, Body.CantJerkOff).applied());

        state(TEST_DOMAIN, Toys.Chastity_Device).remove();
        assertFalse(state(TEST_DOMAIN, Body.CantJerkOff).applied());
    }

    @Test
    public void testApplyFromMiddleOfTheHierarchy() {
        state(TEST_DOMAIN, Toys.Chastity_Device).apply(Body.CantJerkOff);
        state(TEST_DOMAIN, Toys.Chastity_Device).apply(Locks.Chastity_Device_Lock);

        assertTrue(state(TEST_DOMAIN, Body.CantJerkOff).applied());
        assertTrue(state(TEST_DOMAIN, Toys.Chastity_Device).applied());
        assertTrue(state(TEST_DOMAIN, Locks.Chastity_Device_Lock).applied());

        state(TEST_DOMAIN, Locks.Chastity_Device_Lock).remove();
        assertTrue(state(TEST_DOMAIN, Toys.Chastity_Device).applied());
        assertTrue(state(TEST_DOMAIN, Body.CantJerkOff).applied());

        state(TEST_DOMAIN, Toys.Chastity_Device).remove();
        assertFalse(state(TEST_DOMAIN, Body.CantJerkOff).applied());
    }

    @Test
    public void testApplyItemToMultiplePlacesOneAfterAnother() {
        state(TEST_DOMAIN, Body.OnNipples).apply(Clothes_Pegs);

        assertTrue(state(TEST_DOMAIN, Clothes_Pegs).applied());
        assertTrue(state(TEST_DOMAIN, Body.OnNipples).applied());
        assertFalse(state(TEST_DOMAIN, Body.OnBalls).applied());

        state(TEST_DOMAIN, Body.OnBalls).apply(Clothes_Pegs);

        assertTrue(state(TEST_DOMAIN, Clothes_Pegs).applied());
        assertTrue(state(TEST_DOMAIN, Body.OnNipples).applied());
        assertTrue(state(TEST_DOMAIN, Body.OnBalls).applied());
    }

    @Test
    public void testApplyItemToMultiplePlaces() {
        state(TEST_DOMAIN, Clothes_Pegs).apply(Body.OnNipples);
        state(TEST_DOMAIN, Clothes_Pegs).apply(Body.OnBalls);

        assertTrue(state(TEST_DOMAIN, Clothes_Pegs).applied());
        assertTrue(state(TEST_DOMAIN, Body.OnBalls).applied());
        assertTrue(state(TEST_DOMAIN, Body.OnNipples).applied());
    }

    @Test
    public void testApplyItemToMultiplePlacesRemoveAllAtOnce() {
        state(TEST_DOMAIN, Clothes_Pegs).apply(Body.OnNipples);
        state(TEST_DOMAIN, Clothes_Pegs).apply(Body.OnBalls);

        assertTrue(state(TEST_DOMAIN, Clothes_Pegs).applied());
        assertTrue(state(TEST_DOMAIN, Body.OnBalls).applied());
        assertTrue(state(TEST_DOMAIN, Body.OnNipples).applied());

        state(TEST_DOMAIN, Clothes_Pegs).remove();

        assertFalse(state(TEST_DOMAIN, Clothes_Pegs).applied());
        assertFalse(state(TEST_DOMAIN, Body.OnBalls).applied());
        assertFalse(state(TEST_DOMAIN, Body.OnNipples).applied());
    }

    @Test
    public void testApplyItemToMultiplePlacesRemoveOneAfterAnother() {
        state(TEST_DOMAIN, Clothes_Pegs).apply(Body.OnNipples);
        state(TEST_DOMAIN, Clothes_Pegs).apply(Body.OnBalls);

        assertTrue(state(TEST_DOMAIN, Clothes_Pegs).applied());
        assertTrue(state(TEST_DOMAIN, Body.OnBalls).applied());
        assertTrue(state(TEST_DOMAIN, Body.OnNipples).applied());

        // state(TEST_DOMAIN, Clothes_Pegs).remove(Body.SomethingOnBalls);
        state(TEST_DOMAIN, Body.OnBalls).remove();

        assertTrue(state(TEST_DOMAIN, Clothes_Pegs).applied());
        assertFalse(state(TEST_DOMAIN, Body.OnBalls).applied());
        assertTrue(state(TEST_DOMAIN, Body.OnNipples).applied());
    }

    @Test
    public void testApplyItemToMultiplePlacesRemoveOneAfterAnotherWithoutKnowingTheActualToy() {
        state(TEST_DOMAIN, Clothes_Pegs).apply(Body.OnNipples);
        state(TEST_DOMAIN, Clothes_Pegs).apply(Body.OnBalls);

        assertTrue(state(TEST_DOMAIN, Clothes_Pegs).applied());
        assertTrue(state(TEST_DOMAIN, Body.OnBalls).applied());
        assertTrue(state(TEST_DOMAIN, Body.OnNipples).applied());

        state(TEST_DOMAIN, Body.OnNipples).remove();

        assertTrue(state(TEST_DOMAIN, Clothes_Pegs).applied());
        assertTrue(state(TEST_DOMAIN, Body.OnBalls).applied());
        assertFalse(state(TEST_DOMAIN, Body.OnNipples).applied());
    }

    @Test
    public void testThatStateDefaultValueEqualsRemovedALongTimeAgo() {
        assertEquals(0, state(TEST_DOMAIN, Clothes_Pegs).duration().start(TimeUnit.SECONDS));
        assertEquals(State.REMOVED, state(TEST_DOMAIN, Clothes_Pegs).duration().limit(TimeUnit.SECONDS));
    }

    @Test
    public void testApplyWithoutPeersWorks() {
        state(TEST_DOMAIN, Body.OnBalls).apply();
        assertTrue(state(TEST_DOMAIN, Body.OnBalls).applied());
    }

    @Test
    public void testApplyAndRemoveWithSelfReference() {
        assertFalse(state(TEST_DOMAIN, Toys.Chastity_Device).applied());
        assertFalse(state(TEST_DOMAIN, Body.OnPenis).applied());

        state(TEST_DOMAIN, Toys.Chastity_Device).apply(Body.OnPenis);

        assertTrue(state(TEST_DOMAIN, Toys.Chastity_Device).applied());
        assertTrue(state(TEST_DOMAIN, Body.OnPenis).applied());

        state(TEST_DOMAIN, Body.OnPenis).apply(Body.OnPenis);
        assertTrue(state(TEST_DOMAIN, Body.OnPenis).applied());

        state(TEST_DOMAIN, Toys.Chastity_Device).remove();
        assertTrue(state(TEST_DOMAIN, Body.OnPenis).applied());

        state(TEST_DOMAIN, Body.OnPenis).remove();
        assertFalse(state(TEST_DOMAIN, Body.OnPenis).applied());
    }
}
