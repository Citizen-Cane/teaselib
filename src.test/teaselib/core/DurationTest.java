package teaselib.core;

import static java.util.concurrent.TimeUnit.MINUTES;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.concurrent.TimeUnit;

import org.junit.Before;
import org.junit.Test;

import teaselib.Duration;
import teaselib.State;
import teaselib.Toys;
import teaselib.test.TestScript;
import teaselib.util.Item;

public class DurationTest {

    private TestScript script;

    @Before
    public void setup() {
        script = TestScript.getOne();
        script.teaseLib.freezeTime();
    }

    @Test
    public void testStart() throws Exception {
        assertEquals(TimeUnit.MILLISECONDS.toHours(System.currentTimeMillis()),
                script.duration(24, TimeUnit.HOURS).start(TimeUnit.HOURS));

        assertEquals(TimeUnit.MILLISECONDS.toMinutes(System.currentTimeMillis()),
                script.duration(60, TimeUnit.MINUTES).start(TimeUnit.MINUTES));

        assertEquals(TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis()),
                script.duration(60, TimeUnit.SECONDS).start(TimeUnit.SECONDS));
    }

    @Test
    public void testLimit() throws Exception {
        assertEquals(24, script.duration(24, TimeUnit.HOURS).limit(TimeUnit.HOURS));

        assertEquals(60, script.duration(60, TimeUnit.MINUTES).limit(TimeUnit.MINUTES));

        assertEquals(60, script.duration(60, TimeUnit.SECONDS).limit(TimeUnit.SECONDS));
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
        assertEquals(24, script.duration(24, TimeUnit.HOURS).remaining(TimeUnit.HOURS));

        assertEquals(24, script.duration(24, TimeUnit.MINUTES).remaining(TimeUnit.MINUTES));

        assertEquals(24, script.duration(24, TimeUnit.SECONDS).remaining(TimeUnit.SECONDS));
    }

    @Test
    public void verifyThatRemainingBecomesNegative() throws Exception {
        Duration duration = script.duration(24, TimeUnit.HOURS);
        assertEquals(24, duration.remaining(TimeUnit.HOURS));

        script.teaseLib.advanceTime(24, TimeUnit.HOURS);
        assertEquals(0, duration.remaining(TimeUnit.HOURS));

        script.teaseLib.advanceTime(1, TimeUnit.HOURS);
        assertEquals(-1, duration.remaining(TimeUnit.HOURS));
    }

    @Test
    public void testElapsingDuration() {
        TimeUnit unit = SECONDS;

        script.debugger.freezeTime();
        Duration duration = script.duration(60, unit);

        assertEquals(now(unit), duration.start(unit));
        assertEquals(now(unit), duration.end(unit));
        assertEquals(0, duration.since(unit));

        script.debugger.advanceTime(60, unit);
        assertEquals(now(unit) - 60, duration.start(unit));
        assertEquals(now(unit), duration.end(unit));
        assertEquals(0, duration.since(unit));
    }

    @Test
    public void testStateDuration() {
        TimeUnit unit = SECONDS;

        script.debugger.freezeTime();
        State state = script.state("test");
        state.apply();

        Duration elapsing = state.duration();
        script.debugger.advanceTime(60, unit);

        assertEquals(now(unit) - 60, elapsing.start(unit));
        assertEquals(now(unit), elapsing.end(unit));
        assertEquals(0, elapsing.since(unit));

        state.remove();

        Duration frozen = state.duration();
        assertEquals(now(unit) - 60, frozen.start(unit));
        assertEquals(60, frozen.elapsed(unit));
        assertEquals(now(unit), frozen.end(unit));
        assertEquals(0, frozen.since(unit));

        script.debugger.advanceTime(30, unit);

        assertEquals(now(unit) - 90, frozen.start(unit));
        assertEquals(60, frozen.elapsed(unit));
        assertEquals(now(unit) - 30, frozen.end(unit));
        assertEquals(30, frozen.since(unit));

        assertTrue(state.removed());
        assertEquals(30, state.removed(unit));
    }

    @Test
    public void testItemDuration() {
        TimeUnit unit = MINUTES;

        script.debugger.freezeTime();
        Item item = script.item(Toys.Nipple_Clamps);
        item.apply();

        Duration elapsing = item.duration();
        assertEquals(now(unit), elapsing.start(unit));
        script.debugger.advanceTime(25, unit);

        assertEquals(now(unit) - 25, elapsing.start(unit));
        assertEquals(now(unit), elapsing.end(unit));
        assertEquals(0, elapsing.since(unit));

        item.remove();

        Duration frozen = item.duration();
        assertEquals(now(unit) - 25, frozen.start(unit));
        assertEquals(25, frozen.elapsed(unit));
        assertEquals(now(unit), frozen.end(unit));
        assertEquals(0, frozen.since(unit));

        script.debugger.advanceTime(10, unit);

        assertEquals(now(unit) - 35, frozen.start(unit));
        assertEquals(25, frozen.elapsed(unit));
        assertEquals(now(unit) - 10, frozen.end(unit));
        assertEquals(10, frozen.since(unit));

        assertTrue(item.removed());
        assertEquals(10, item.removed(unit));
    }

    @Test
    public void testStateNotRemovedSince() {
        TimeUnit unit = SECONDS;

        script.debugger.freezeTime();
        State state = script.state("test");
        state.apply();

        Duration elapsing = state.duration();
        script.debugger.advanceTime(60, unit);

        assertEquals(now(unit) - 60, elapsing.start(unit));
        assertEquals(now(unit), elapsing.end(unit));
        assertEquals(0, elapsing.since(unit));

        assertFalse(state.removed());
        assertEquals(0, state.removed(SECONDS));
    }

    @Test
    public void testIndefiniteDuration() {
        assertEquals(Duration.INFINITE, script.duration(Duration.INFINITE, SECONDS).limit(SECONDS));
    }

    private long now(TimeUnit unit) {
        return script.teaseLib.getTime(unit);
    }

    @Test
    public void testExpired() {
        assertFalse(script.duration(24, TimeUnit.HOURS).expired());
        assertTrue(script.duration(0, TimeUnit.HOURS).expired());

        assertFalse(script.duration(60, TimeUnit.MINUTES).expired());
        assertTrue(script.duration(0, TimeUnit.MINUTES).expired());

        assertFalse(script.duration(60, TimeUnit.SECONDS).expired());
        assertTrue(script.duration(0, TimeUnit.SECONDS).expired());
    }
}
