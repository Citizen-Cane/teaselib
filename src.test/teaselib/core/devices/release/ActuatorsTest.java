package teaselib.core.devices.release;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.junit.Test;

import teaselib.core.devices.release.KeyReleaseBaseTest.ActuatorMock;

public class ActuatorsTest {
    private static final List<Actuator> actuatorMocks = Arrays.asList(new ActuatorMock(2, TimeUnit.HOURS),
            new ActuatorMock(1, TimeUnit.HOURS));

    @Test
    public void testMatchingActuatorSorting() {
        List<Actuator> matching = Actuators.matching(actuatorMocks, 1, TimeUnit.HOURS);
        assertEquals(1, matching.get(0).available(TimeUnit.HOURS));
        assertEquals(2, matching.get(1).available(TimeUnit.HOURS));
    }

    @Test
    public void testMatchingActuatorSorting2() {
        List<Actuator> matching = Actuators.matching(actuatorMocks, 2, TimeUnit.HOURS);
        assertEquals(2, matching.get(0).available(TimeUnit.HOURS));
        assertEquals(1, matching.get(1).available(TimeUnit.HOURS));
    }

    @Test
    public void testMatchingActuatorSortingMin() {
        List<Actuator> matching = Actuators.matching(actuatorMocks, Long.MIN_VALUE, TimeUnit.HOURS);
        assertEquals(1, matching.get(0).available(TimeUnit.HOURS));
        assertEquals(2, matching.get(1).available(TimeUnit.HOURS));
    }

    @Test
    public void testMatchingActuatorSortingMax() {
        List<Actuator> matching = Actuators.matching(actuatorMocks, Long.MAX_VALUE, TimeUnit.HOURS);
        assertEquals(2, matching.get(0).available(TimeUnit.HOURS));
        assertEquals(1, matching.get(1).available(TimeUnit.HOURS));
    }

}
