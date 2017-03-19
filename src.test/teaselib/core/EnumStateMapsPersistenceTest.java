package teaselib.core;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.concurrent.TimeUnit;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import teaselib.Body;
import teaselib.Duration;
import teaselib.Toys;
import teaselib.hosts.DummyPersistence;
import teaselib.test.TestScript;

public class EnumStateMapsPersistenceTest extends EnumStateMaps {
    final DummyPersistence persistence;

    static TestScript script;

    @BeforeClass
    public static void initPersistence() {
        script = TestScript.getOne();
    }

    @Before
    public void initStorage() {
        persistence.storage.clear();

    }

    public EnumStateMapsPersistenceTest() {
        super(script.teaseLib);
        persistence = script.persistence;
    }

    @Test
    public void testPersistenceOnLock() {
        assertApplyChastityCage();

        state(Toys.Chastity_Device_Lock).apply(Toys.Chastity_Cage);

        assertTrue(state(Toys.Chastity_Cage).applied());
        assertTrue(state(Toys.Chastity_Cage).expired());
        assertTrue(state(Toys.Chastity_Device_Lock).applied());
        assertTrue(state(Toys.Chastity_Device_Lock).expired());

        state(Toys.Chastity_Device_Lock).apply(Toys.Chastity_Cage)
                .upTo(24, TimeUnit.HOURS).remember();

        assertTrue(state(Body.SomethingOnPenis).expired());
        assertTrue(state(Body.CannotJerkOff).expired());

        clear();

        assertTrue(state(Body.SomethingOnPenis).expired());
        assertTrue(state(Body.CannotJerkOff).expired());

        assertRemoveKey();
    }

    @Test
    public void testLockDurationOnInfiniteToy() {
        assertApplyChastityCage();

        state(Toys.Chastity_Cage)
                .apply(Body.SomethingOnPenis, Body.CannotJerkOff)
                .upTo(Duration.INFINITE, TimeUnit.SECONDS);
        state(Toys.Chastity_Device_Lock).apply(Toys.Chastity_Cage);

        assertTrue(state(Toys.Chastity_Cage).applied());
        assertFalse(state(Toys.Chastity_Cage).expired());
        assertTrue(state(Toys.Chastity_Device_Lock).applied());
        assertTrue(state(Toys.Chastity_Device_Lock).expired());

        state(Toys.Chastity_Device_Lock).apply(Toys.Chastity_Cage)
                .upTo(24, TimeUnit.HOURS).remember();

        assertFalse(state(Body.SomethingOnPenis).expired());
        assertFalse(state(Body.CannotJerkOff).expired());

        clear();

        assertFalse(state(Body.SomethingOnPenis).expired());
        assertFalse(state(Body.CannotJerkOff).expired());

        assertRemoveKey();
    }

    private void assertApplyChastityCage() {
        assertFalse(state(Toys.Chastity_Cage).applied());
        assertFalse(state(Toys.Chastity_Device_Lock).applied());

        state(Toys.Chastity_Cage).apply(Body.SomethingOnPenis,
                Body.CannotJerkOff);

        assertTrue(state(Body.SomethingOnPenis).applied());
        assertTrue(state(Body.CannotJerkOff).applied());
    }

    private void assertRemoveKey() {
        assertTrue(state(Toys.Chastity_Device_Lock).applied());
        assertTrue(state(Toys.Chastity_Cage).applied());

        assertFalse(state(Toys.Chastity_Device_Lock).expired());
        assertFalse(state(Toys.Chastity_Cage).expired());

        state(Toys.Chastity_Device_Lock).remove();

        assertFalse(state(Toys.Chastity_Device_Lock).applied());
        assertTrue(state(Toys.Chastity_Device_Lock).expired());
        assertTrue(state(Toys.Chastity_Cage).applied());
        assertFalse(state(Toys.Chastity_Cage).expired());

        state(Toys.Chastity_Cage).remove();
        assertFalse(state(Toys.Chastity_Cage).applied());
        assertTrue(state(Toys.Chastity_Cage).expired());

        assertFalse(state(Body.SomethingOnPenis).applied());
        assertFalse(state(Body.SomethingOnPenis).applied());
    }
}
