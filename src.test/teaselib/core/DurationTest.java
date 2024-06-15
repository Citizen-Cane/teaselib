package teaselib.core;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import teaselib.Duration;
import teaselib.State;
import teaselib.Toys;
import teaselib.test.TestScript;
import teaselib.util.Item;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import static java.util.concurrent.TimeUnit.MINUTES;
import static java.util.concurrent.TimeUnit.SECONDS;

class DurationTest {

    private TestScript script;

    @BeforeEach
    public void setup() throws IOException {
        script = new TestScript();
        script.teaseLib.freezeTime();
    }

    @AfterEach
    public void cleanup() {
        script.close();
    }

    @Test
    public void testStart() throws Exception {
        Assertions.assertEquals(TimeUnit.MILLISECONDS.toHours(System.currentTimeMillis()), script.duration(24, TimeUnit.HOURS).start(TimeUnit.HOURS));

        Assertions.assertEquals(TimeUnit.MILLISECONDS.toMinutes(System.currentTimeMillis()), script.duration(60, MINUTES).start(MINUTES));

        Assertions.assertEquals(TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis()), script.duration(60, SECONDS).start(SECONDS));
    }

    @Test
    public void testLimit() throws Exception {
        Assertions.assertEquals(24, script.duration(24, TimeUnit.HOURS).limit(TimeUnit.HOURS));
        Assertions.assertEquals(60, script.duration(60, MINUTES).limit(MINUTES));
        Assertions.assertEquals(60, script.duration(60, SECONDS).limit(SECONDS));
    }

    @Test
    public void testSinceElapsingDuration() throws Exception {
        Duration duration = script.duration(30, TimeUnit.MINUTES);
        Assertions.assertEquals(0, duration.since(MINUTES));
        Assertions.assertEquals(0, duration.since(TimeUnit.HOURS));
        script.debugger.advanceTime(30, MINUTES);
        Assertions.assertEquals(0, duration.since(MINUTES));
        Assertions.assertEquals(0, duration.since(TimeUnit.HOURS));
        script.debugger.advanceTime(30, MINUTES);
        Assertions.assertEquals(0, duration.since(MINUTES));
        Assertions.assertEquals(0, duration.since(TimeUnit.HOURS));
        script.debugger.advanceTime(30, MINUTES);
        Assertions.assertEquals(0, duration.since(MINUTES));
        Assertions.assertEquals(0, duration.since(TimeUnit.HOURS));
    }

    @Test
    public void testSinceFrozenDuration() throws Exception {
        Duration duration = new FrozenDuration(script.teaseLib, script.duration(30, TimeUnit.MINUTES));
        Assertions.assertEquals(0, duration.since(TimeUnit.HOURS));
        script.debugger.advanceTime(30, MINUTES);
        Assertions.assertEquals(30, duration.since(MINUTES));
        Assertions.assertEquals(0, duration.since(TimeUnit.HOURS));
        script.debugger.advanceTime(30, MINUTES);
        Assertions.assertEquals(60, duration.since(MINUTES));
        Assertions.assertEquals(1, duration.since(TimeUnit.HOURS));
    }

    @Test
    public void testElapsed() throws Exception {
        Duration duration = script.duration(24, TimeUnit.HOURS);
        script.teaseLib.advanceTime(1, TimeUnit.HOURS);
        Assertions.assertEquals(1, duration.elapsed(TimeUnit.HOURS));

        Duration duration2 = script.duration(60, TimeUnit.MINUTES);
        script.teaseLib.advanceTime(1, TimeUnit.MINUTES);
        Assertions.assertEquals(1, duration2.elapsed(MINUTES));

        Duration duration3 = script.duration(60, TimeUnit.SECONDS);
        script.teaseLib.advanceTime(1, TimeUnit.SECONDS);
        Assertions.assertEquals(1, duration3.elapsed(SECONDS));
    }

    @Test
    public void testRemaining() throws Exception {
        Assertions.assertEquals(24, script.duration(24, TimeUnit.HOURS).remaining(TimeUnit.HOURS));

        Assertions.assertEquals(24, script.duration(24, MINUTES).remaining(MINUTES));

        Assertions.assertEquals(24, script.duration(24, SECONDS).remaining(SECONDS));
    }

    @Test
    public void verifyThatRemainingBecomesNegative() throws Exception {
        Duration duration = script.duration(24, TimeUnit.HOURS);
        Assertions.assertEquals(24, duration.remaining(TimeUnit.HOURS));

        script.teaseLib.advanceTime(24, TimeUnit.HOURS);
        Assertions.assertEquals(0, duration.remaining(TimeUnit.HOURS));

        script.teaseLib.advanceTime(1, TimeUnit.HOURS);
        Assertions.assertEquals(-1, duration.remaining(TimeUnit.HOURS));
    }

    @Test
    public void testElapsingDuration() {
        TimeUnit unit = SECONDS;

        Duration duration = script.duration(60, unit);

        Assertions.assertEquals(now(unit), duration.start(unit));
        Assertions.assertEquals(now(unit), duration.end(unit));
        Assertions.assertEquals(0, duration.since(unit));

        script.debugger.advanceTime(60, unit);
        Assertions.assertEquals(now(unit) - 60, duration.start(unit));
        Assertions.assertEquals(now(unit), duration.end(unit));
        Assertions.assertEquals(0, duration.since(unit));
    }

    @Test
    public void testStateDuration() {
        TimeUnit unit = SECONDS;

        State state = script.state("test");
        Assertions.assertEquals(Duration.INFINITE, state.removed(unit));
        state.apply();

        Duration elapsing = state.duration();
        script.debugger.advanceTime(60, unit);

        Assertions.assertEquals(now(unit) - 60, elapsing.start(unit));
        Assertions.assertEquals(now(unit), elapsing.end(unit));
        Assertions.assertEquals(0, elapsing.since(unit));

        state.remove();

        Duration frozen = state.duration();
        Assertions.assertEquals(now(unit) - 60, frozen.start(unit));
        Assertions.assertEquals(60, frozen.elapsed(unit));
        Assertions.assertEquals(now(unit), frozen.end(unit));
        Assertions.assertEquals(0, frozen.since(unit));

        script.debugger.advanceTime(30, unit);

        Assertions.assertEquals(now(unit) - 90, frozen.start(unit));
        Assertions.assertEquals(60, frozen.elapsed(unit));
        Assertions.assertEquals(now(unit) - 30, frozen.end(unit));
        Assertions.assertEquals(30, frozen.since(unit));

        Assertions.assertTrue(state.removed());
        Assertions.assertEquals(30, state.removed(unit));
    }

    @Test
    public void testItemDuration() {
        TimeUnit unit = MINUTES;

        Item item = script.item(Toys.Nipple_Clamps);
        item.apply();

        Duration elapsing = item.duration();
        Assertions.assertEquals(now(unit), elapsing.start(unit));
        script.debugger.advanceTime(25, unit);

        Assertions.assertEquals(now(unit) - 25, elapsing.start(unit));
        Assertions.assertEquals(now(unit), elapsing.end(unit));
        Assertions.assertEquals(0, elapsing.since(unit));

        item.remove();

        Duration frozen = item.duration();
        Assertions.assertEquals(now(unit) - 25, frozen.start(unit));
        Assertions.assertEquals(25, frozen.elapsed(unit));
        Assertions.assertEquals(now(unit), frozen.end(unit));
        Assertions.assertEquals(0, frozen.since(unit));

        script.debugger.advanceTime(10, unit);

        Assertions.assertEquals(now(unit) - 35, frozen.start(unit));
        Assertions.assertEquals(25, frozen.elapsed(unit));
        Assertions.assertEquals(now(unit) - 10, frozen.end(unit));
        Assertions.assertEquals(10, frozen.since(unit));

        Assertions.assertTrue(item.removed());
        Assertions.assertEquals(10, item.removed(unit));
    }

    @Test
    public void testItemDurationForDifferentItemsOfSameKind() {
        TimeUnit unit = TimeUnit.HOURS;
        FrozenDuration neverApplied = new FrozenDuration(script.teaseLib, 0, 0, 0, TimeUnit.HOURS);

        Item item1 = script.items(Toys.Chastity_Device).matching(Toys.Chastity_Devices.Cage).item();
        Item item2 = script.items(Toys.Chastity_Device).matching(Toys.Chastity_Devices.Gates_of_Hell).item();

        Assertions.assertEquals(neverApplied.since(unit), item1.removed(unit), "Removed since for item1");
        Assertions.assertEquals(neverApplied.since(unit), item2.removed(unit), "Removed since for item2");

        item1.apply();
        script.debugger.advanceTime(4, unit);
        Assertions.assertEquals(0, item1.removed(unit), "Removed since for item1");

        item1.remove();
        Assertions.assertEquals(0, item1.removed(unit), "Removed since for item1");

        script.debugger.advanceTime(1, unit);

        Assertions.assertEquals(1, item1.removed(unit), "Removed since for item1");
        item2.apply();
        Assertions.assertEquals(1, item1.removed(unit), "Removed since for item1");

        script.debugger.advanceTime(1, unit);
        Assertions.assertEquals(0, item2.removed(unit), "Removed since for item2");

        item2.remove();
        Assertions.assertEquals(2, item1.removed(unit), "Removed since for item1");
        Assertions.assertEquals(0, item2.removed(unit), "Removed since for item2");

        script.debugger.advanceTime(2, unit);
        Assertions.assertEquals(4, item1.removed(unit), "Removed since for item1");
        Assertions.assertEquals(2, item2.removed(unit), "Removed since for item2");
    }

    @Test
    public void testStateNotRemovedSince() {
        TimeUnit unit = SECONDS;

        State state = script.state("test");
        state.apply();

        Duration elapsing = state.duration();
        script.debugger.advanceTime(60, unit);

        Assertions.assertEquals(now(unit) - 60, elapsing.start(unit));
        Assertions.assertEquals(now(unit), elapsing.end(unit));
        Assertions.assertEquals(0, elapsing.since(unit));

        Assertions.assertFalse(state.removed());
        Assertions.assertEquals(0, state.removed(SECONDS));
    }

    @Test
    public void testStateNeverApplied() {
        State state = script.state("test");
        Assertions.assertTrue(state.removed());
        Assertions.assertEquals(Duration.INFINITE, state.removed(SECONDS));
        Assertions.assertEquals(Duration.INFINITE, state.removed(MINUTES));
        Assertions.assertEquals(Duration.INFINITE, state.removed(TimeUnit.DAYS));
    }

    @Test
    public void testItemNeverApplied() {
        Item item = script.item("test");
        Assertions.assertFalse(item.is("test"));
        Assertions.assertFalse(item.is("other"));
        Assertions.assertEquals(0, item.duration().elapsed(SECONDS));
        Assertions.assertTrue(item.removed());
        Assertions.assertEquals(Duration.INFINITE, item.removed(SECONDS));
        Assertions.assertEquals(Duration.INFINITE, item.removed(MINUTES));
        Assertions.assertEquals(Duration.INFINITE, item.removed(TimeUnit.DAYS));
    }

    @Test
    public void testIndefiniteDuration() {
        Assertions.assertEquals(Duration.INFINITE, script.duration(Duration.INFINITE, SECONDS).limit(SECONDS));
    }

    private long now(TimeUnit unit) {
        return script.teaseLib.getTime(unit);
    }

    @Test
    public void testExpired() {
        Assertions.assertFalse(script.duration(24, TimeUnit.HOURS).expired());
        Assertions.assertTrue(script.duration(0, TimeUnit.HOURS).expired());

        Assertions.assertFalse(script.duration(60, MINUTES).expired());
        Assertions.assertTrue(script.duration(0, MINUTES).expired());

        Assertions.assertFalse(script.duration(60, SECONDS).expired());
        Assertions.assertTrue(script.duration(0, SECONDS).expired());
    }
}
