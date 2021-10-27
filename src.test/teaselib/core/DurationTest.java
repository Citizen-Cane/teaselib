package teaselib.core;

import static java.util.concurrent.TimeUnit.*;
import static org.junit.Assert.*;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import org.junit.After;
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
    public void setup() throws IOException {
        script = new TestScript();
        script.teaseLib.freezeTime();
    }

    @After
    public void cleanup() {
        script.close();
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
    public void testItemDurationForDifferentItemsOfSameKind() {
        TimeUnit unit = TimeUnit.HOURS;
        FrozenDuration neverApplied = new FrozenDuration(script.teaseLib, 0, 0, TimeUnit.HOURS);

        script.debugger.freezeTime();
        Item item1 = script.items(Toys.Chastity_Device).matching(Toys.Chastity_Devices.Cage).get();
        Item item2 = script.items(Toys.Chastity_Device).matching(Toys.Chastity_Devices.Gates_of_Hell).get();

        assertEquals("Removed since for item1", neverApplied.since(unit), item1.removed(unit));
        assertEquals("Removed since for item2", neverApplied.since(unit), item2.removed(unit));

        item1.apply();
        script.debugger.advanceTime(4, unit);
        assertEquals("Removed since for item1", 0, item1.removed(unit));

        item1.remove();
        assertEquals("Removed since for item1", 0, item1.removed(unit));

        script.debugger.advanceTime(1, unit);

        item2.apply();
        script.debugger.advanceTime(1, unit);
        assertEquals("Removed since for item2", 0, item2.removed(unit));

        item2.remove();
        assertEquals("Removed since for item1", 2, item1.removed(unit));
        assertEquals("Removed since for item2", 0, item2.removed(unit));

        script.debugger.advanceTime(2, unit);
        assertEquals("Removed since for item1", 4, item1.removed(unit));
        assertEquals("Removed since for item2", 2, item2.removed(unit));
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
    public void testStateNeverApplied() {
        script.debugger.freezeTime();
        State state = script.state("test");
        assertTrue(state.removed());
        assertTrue(state.removed(TimeUnit.DAYS) > 0);
    }

    @Test
    public void testItemNeverApplied() {
        script.debugger.freezeTime();
        Item item = script.item("test");
        assertTrue(item.removed());
        assertTrue(item.removed(TimeUnit.DAYS) > 0);
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
