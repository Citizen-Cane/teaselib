package teaselib.core;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static teaselib.Toys.Clothespins;

import java.util.concurrent.TimeUnit;

import org.junit.Test;

import teaselib.Body;
import teaselib.Toys;
import teaselib.test.TestScript;

public class EnumStateMapsTest extends EnumStateMaps {

    public EnumStateMapsTest() {
        super(TestScript.getOne().teaseLib);
    }

    @Test
    public void testCannotJerkOff() throws Exception {
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
    public void testPegsOnNipplesAndNutsApplyOneAfterAnother()
            throws Exception {
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
    public void testPegsOnNipplesAndNutsRemoveOneAfterAnother()
            throws Exception {
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
    public void testLockUp() throws Exception {
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
    public void testLockDuration() throws Exception {
        State chastityCage = state(Toys.Chastity_Cage);
        State key = state(Toys.Chastity_Device_Lock);

        assertFalse(chastityCage.applied());
        assertFalse(key.applied());

        chastityCage.apply(Body.SomethingOnPenis, Body.CannotJerkOff);
        assertTrue(state(Body.SomethingOnPenis).applied());
        assertTrue(state(Body.CannotJerkOff).applied());

        key.apply(Toys.Chastity_Cage);

        assertTrue(chastityCage.applied());
        assertTrue(chastityCage.expired());
        assertTrue(key.applied());
        assertTrue(key.expired());

        key.apply(Toys.Chastity_Cage).upTo(24, TimeUnit.HOURS);
        assertTrue(key.applied());
        assertTrue(chastityCage.applied());

        assertFalse(key.expired());
        assertFalse(chastityCage.expired());

        key.remove();
        assertFalse(key.applied());
        assertTrue(key.expired());
        assertTrue(chastityCage.applied());
        assertFalse(chastityCage.expired());

        chastityCage.remove();
        assertFalse(chastityCage.applied());
        assertTrue(chastityCage.expired());

        assertFalse(state(Body.SomethingOnPenis).applied());
        assertFalse(state(Body.SomethingOnPenis).applied());
    }

    @Test
    public void testApplyRemovesPreviousApplyCorrectSequence() {
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
    public void testApplyRemovesPreviousApplyWrongSequence() {
        state(Toys.Chastity_Cage).apply(Body.CannotJerkOff);
        state(Toys.Chastity_Cage).apply(Toys.Chastity_Device_Lock);

        assertFalse(state(Body.CannotJerkOff).applied());
    }

    @Test
    public void testApplyItemToMultiplePlacesCorrectly() throws Exception {
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
    public void testApplyItemToMultiplePlacesTheWrongWay() throws Exception {
        state(Toys.Clothespins).apply(Body.SomethingOnNipples);
        state(Toys.Clothespins).apply(Body.SomethingOnBalls);

        assertTrue(state(Toys.Clothespins).applied());
        assertTrue(state(Body.SomethingOnBalls).applied());

        assertFalse(state(Body.SomethingOnNipples).applied());
    }
}
