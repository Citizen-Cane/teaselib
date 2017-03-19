package teaselib.core;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.TimeUnit;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import teaselib.Body;
import teaselib.Duration;
import teaselib.Toys;
import teaselib.hosts.DummyPersistence;
import teaselib.test.TestScript;

@RunWith(Parameterized.class)
public class EnumStateMapsPersistenceTest extends EnumStateMaps {
    final DummyPersistence persistence;

    static TestScript script;

    TestParameter rememberState;

    @BeforeClass
    public static void initPersistence() {
        script = TestScript.getOne();
    }
    
    enum TestParameter {
        DontTestPersistence,
        TestPersistence
    }

    @Parameters(name = "{0}")
    public static Collection<Object[]> parameters() {
        return Arrays.asList(
                new Object[][] { { TestParameter.DontTestPersistence },
                        { TestParameter.TestPersistence } });
    }

    public EnumStateMapsPersistenceTest(TestParameter remember) {
        super(script.teaseLib);
        persistence = script.persistence;
        rememberState = remember;
    }

    @Before
    public void initStorage() {
        persistence.storage.clear();

    }

    void rememberOrNot(State.Persistence state) {
        if (rememberState == TestParameter.TestPersistence) {
            state.remember();
        }
    }

    void clearStatesMapsOrNot() {
        if (rememberState == TestParameter.TestPersistence) {
            clear();
        }
    }

    @Test
    public void testPersistenceOnLock() {
        assertApplyChastityCage();

        state(Toys.Chastity_Device_Lock).apply(Toys.Chastity_Cage);

        assertTrue(state(Toys.Chastity_Cage).applied());
        assertTrue(state(Toys.Chastity_Cage).expired());
        assertTrue(state(Toys.Chastity_Device_Lock).applied());
        assertTrue(state(Toys.Chastity_Device_Lock).expired());

        rememberOrNot(state(Toys.Chastity_Device_Lock).apply(Toys.Chastity_Cage)
                .upTo(24, TimeUnit.HOURS));

        assertTrue(state(Body.SomethingOnPenis).expired());
        assertTrue(state(Body.CannotJerkOff).expired());

        clearStatesMapsOrNot();

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
