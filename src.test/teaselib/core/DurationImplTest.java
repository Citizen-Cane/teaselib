package teaselib.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.concurrent.TimeUnit;

import org.junit.Before;
import org.junit.Test;

import teaselib.Duration;
import teaselib.State;
import teaselib.TeaseScript;
import teaselib.test.TestScript;

public class DurationImplTest {

    private TeaseScript script;

    @Before
    public void setup() {
        script = TestScript.getOne();
        script.teaseLib.freezeTime();
    }

    @Test
    public void testStart() throws Exception {
        assertEquals(TimeUnit.MILLISECONDS.toHours(System.currentTimeMillis()),
                script.duration(24, TimeUnit.HOURS).start(TimeUnit.HOURS));

        assertEquals(
                TimeUnit.MILLISECONDS.toMinutes(System.currentTimeMillis()),
                script.duration(60, TimeUnit.MINUTES).start(TimeUnit.MINUTES));

        assertEquals(
                TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis()),
                script.duration(60, TimeUnit.SECONDS).start(TimeUnit.SECONDS));
    }

    @Test
    public void testLimit() throws Exception {
        assertEquals(24,
                script.duration(24, TimeUnit.HOURS).limit(TimeUnit.HOURS));

        assertEquals(60,
                script.duration(60, TimeUnit.MINUTES).limit(TimeUnit.MINUTES));

        assertEquals(60,
                script.duration(60, TimeUnit.SECONDS).limit(TimeUnit.SECONDS));
    }

    @Test
    public void testElapsed() throws Exception {
        Duration duration = script.duration(24, TimeUnit.HOURS);
        script.teaseLib.advanceTime(1, TimeUnit.HOURS);
        assertEquals(1, duration.elapsed(TimeUnit.HOURS));

        Duration duration2 = script.duration(60, TimeUnit.MINUTES);
        script.teaseLib.advanceTime(1, TimeUnit.MINUTES);
        assertEquals(1, duration2.elapsed(TimeUnit.MINUTES));

        Duration duration3 = script.duration(60, TimeUnit.SECONDS);
        script.teaseLib.advanceTime(1, TimeUnit.SECONDS);
        assertEquals(1, duration3.elapsed(TimeUnit.SECONDS));
    }

    @Test
    public void testRemaining() throws Exception {
        assertEquals(24,
                script.duration(24, TimeUnit.HOURS).remaining(TimeUnit.HOURS));

        assertEquals(24, script.duration(24, TimeUnit.MINUTES)
                .remaining(TimeUnit.MINUTES));

        assertEquals(24, script.duration(24, TimeUnit.SECONDS)
                .remaining(TimeUnit.SECONDS));
    }

    @Test
    public void testEnd() {
        script.teaseLib.freezeTime();
        assertEquals(script.teaseLib.getTime(TimeUnit.SECONDS) + 60,
                script.duration(60, TimeUnit.SECONDS).end(TimeUnit.SECONDS));

        assertEquals(State.INDEFINITELY,
                script.duration(State.INDEFINITELY, TimeUnit.SECONDS)
                        .end(TimeUnit.SECONDS));
    }

    @Test
    public void testExpired() throws Exception {
        assertFalse(script.duration(24, TimeUnit.HOURS).expired());
        assertTrue(script.duration(0, TimeUnit.HOURS).expired());

        assertFalse(script.duration(60, TimeUnit.MINUTES).expired());
        assertTrue(script.duration(0, TimeUnit.MINUTES).expired());

        assertFalse(script.duration(60, TimeUnit.SECONDS).expired());
        assertTrue(script.duration(0, TimeUnit.SECONDS).expired());
    }
}
