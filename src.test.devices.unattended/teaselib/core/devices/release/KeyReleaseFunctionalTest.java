/**
 * 
 */
package teaselib.core.devices.release;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import teaselib.core.configuration.DebugSetup;
import teaselib.core.devices.Devices;

/**
 * @author Citizen-Cane
 *
 */
public class KeyReleaseFunctionalTest extends KeyReleaseBaseTest {
    final Devices devices = new Devices(DebugSetup.getConfigurationWithRemoteDeviceAccess());
    final KeyRelease keyRelease = devices.getDefaultDevice(KeyRelease.class);

    @Before
    public void before() {
        assertConnected(keyRelease, WAIT_FOR_CONNECTION_SECONDS);
        releaseAllRunningActuators(keyRelease);
    }

    @After
    public void releaseAllAfterwards() {
        releaseAllRunningActuators(keyRelease);
    }

    @Test
    public void testManualRelease() {
        for (Actuator actuator : assertConnected(keyRelease)) {
            arm(actuator);
            start(actuator);
            waitForAutoRelease(actuator);
            // Release the key in the last minute
            actuator.release();
            assertStopped(actuator);
        }

        assertEndState(keyRelease);
    }

    @Test
    public void testAutomaticRelease() {
        for (Actuator actuator : assertConnected(keyRelease)) {
            arm(actuator);
            start(actuator);
            waitForAutoRelease(actuator);
            assertStopped(actuator);
        }
        assertEndState(keyRelease);
    }

    @Test(expected = IllegalStateException.class)
    public void testWrongCall() {
        for (Actuator actuator : assertConnected(keyRelease)) {
            arm(actuator);
            start(actuator);
            hold(actuator);
            waitForAutoRelease(actuator);
            assertStopped(actuator);
        }
        assertEndState(keyRelease);
    }

    @Test
    public void testWrongCallReleasesAll() {
        for (Actuator actuator : assertConnected(keyRelease)) {
            arm(actuator);
            hold(actuator);
            start(actuator);
        }

        Actuator wrongCalled = keyRelease.actuators().get(0);
        try {
            hold(wrongCalled);
            fail("Expected WrongCallException");
        } catch (IllegalStateException e) {
            for (Actuator actuator : assertConnected(keyRelease)) {
                assertStopped(actuator);
            }
        } catch (Exception e) {
            fail("Expected WrongCallException");
        }

        assertEndState(keyRelease);
    }

    @Test
    @Ignore
    public void testStatus() {
        // TODO Test once status command is implemented
        for (Actuator actuator : assertConnected(keyRelease)) {
            arm(actuator);
            start(actuator);
            waitForAutoRelease(actuator);
            assertStopped(actuator);
        }

        assertEndState(keyRelease);
    }

    @Test
    public void testDeepSleepRelease() {
        for (Actuator actuator : assertConnected(keyRelease)) {
            arm(actuator);
            start(actuator);
            keyRelease.sleep(Integer.MAX_VALUE);
            // Sleeping longer than the next release duration with only one
            // release pending causes the device to enter deep sleep and
            // release on reset
            sleep(HOLD_DURATION_MINUTES + 1, TimeUnit.MINUTES);
            // The key should have been released automatically by now
            assertStopped(actuator);
        }

        assertEndState(keyRelease);
    }

    @Test
    public void testDeepSleepPacket() {
        Actuators actuators = assertConnected(keyRelease);

        Actuator actuator = actuators.get(0);
        actuator.sleep(HOLD_DURATION_MINUTES, TimeUnit.MINUTES);

        // Trigger deep sleep by request a duration longer than the release duration
        // of the last running actuator. When the device wakes up from deep sleep,
        // it resets and subsequently releases the key.
        sleep(HOLD_DURATION_MINUTES + 1, TimeUnit.MINUTES);
        assertEndState(keyRelease);
    }

    @Test
    public void testHardwiredDuration() {
        List<Long> durations = Arrays.asList(60L, 120L);
        assertEquals(0, Actuators.getActuatorIndex(-59, durations));
        assertEquals(0, Actuators.getActuatorIndex(-60, durations));
        assertEquals(0, Actuators.getActuatorIndex(-61, durations));
        assertEquals(0, Actuators.getActuatorIndex(-90, durations));
        assertEquals(0, Actuators.getActuatorIndex(-119, durations));
        assertEquals(0, Actuators.getActuatorIndex(-120, durations));
        assertEquals(0, Actuators.getActuatorIndex(-121, durations));
        assertEquals(0, Actuators.getActuatorIndex(Long.MIN_VALUE, durations));

        assertEquals(0, Actuators.getActuatorIndex(-1, durations));
        assertEquals(0, Actuators.getActuatorIndex(0, durations));
        assertEquals(0, Actuators.getActuatorIndex(30, durations));
        assertEquals(0, Actuators.getActuatorIndex(59, durations));
        assertEquals(0, Actuators.getActuatorIndex(60, durations));

        assertEquals(1, Actuators.getActuatorIndex(61, durations));
        assertEquals(1, Actuators.getActuatorIndex(90, durations));
        assertEquals(1, Actuators.getActuatorIndex(120, durations));
        assertEquals(1, Actuators.getActuatorIndex(121, durations));
        assertEquals(1, Actuators.getActuatorIndex(Long.MAX_VALUE, durations));
    }
}
