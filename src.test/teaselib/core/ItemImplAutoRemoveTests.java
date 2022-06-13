package teaselib.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import org.junit.Test;

import teaselib.Duration;
import teaselib.State;
import teaselib.State.Persistence.Until;
import teaselib.test.TestScript;
import teaselib.util.Item;

public class ItemImplAutoRemoveTests {

    enum TestItem {
        TEST_ITEM
    }

    enum TestStates {
        BODY_PART
    }

    @Test
    public void testUntilExpiredThrowsWithDefaultDuration() throws IOException {
        try (TestScript script = new TestScript()) {
            Debugger debugger = script.debugger;
            debugger.freezeTime();

            Item item = script.item(TestItem.TEST_ITEM);
            Item.Options options = item.apply();
            assertThrows(IllegalArgumentException.class, () -> options.remember(Until.Expired));
        }
    }

    @Test
    public void testUntilRemovedDefaultDurationLimitIsInfinite() throws IOException {
        try (TestScript script = new TestScript()) {
            Debugger debugger = script.debugger;
            debugger.freezeTime();

            Item item = script.item(TestItem.TEST_ITEM);
            Item.Options options = item.apply();
            options.remember(Until.Removed);
            assertEquals(Duration.INFINITE, item.duration().limit(TimeUnit.SECONDS));
        }
    }

    @Test
    public void testAutoRemovalAfterDuration() throws IOException {
        try (TestScript script = new TestScript()) {
            Debugger debugger = script.debugger;
            debugger.freezeTime();

            {
                Item item = script.item(TestItem.TEST_ITEM);
                State state = script.state(TestItem.TEST_ITEM);
                State part = script.state(TestStates.BODY_PART);

                assertFalse(item.applied());
                assertTrue(item.expired());
                assertFalse(state.applied());
                assertTrue(state.expired());
                assertFalse(part.applied());
                assertTrue(part.expired());

                item.to(TestStates.BODY_PART).apply().over(2, TimeUnit.HOURS).remember(Until.Expired);

                assertTrue(item.applied());
                assertFalse(item.expired());
                assertTrue(state.applied());
                assertFalse(state.expired());
                assertTrue(part.applied());
                assertFalse(part.expired());
            }

            {
                debugger.clearStateMaps();

                Item item = script.item(TestItem.TEST_ITEM);
                State state = script.state(TestItem.TEST_ITEM);
                State part = script.state(TestStates.BODY_PART);
                assertTrue(item.applied());
                assertFalse(item.expired());
                assertTrue(state.applied());
                assertFalse(state.expired());
                assertTrue(part.applied());
                assertFalse(part.expired());

                debugger.advanceTime(1, TimeUnit.HOURS);
                script.triggerAutoRemove();
                assertTrue(item.applied());
                assertFalse(item.expired());
                assertTrue(state.applied());
                assertFalse(state.expired());
                assertTrue(part.applied());
                assertFalse(part.expired());
            }

            {
                debugger.clearStateMaps();

                Item item = script.item(TestItem.TEST_ITEM);
                State state = script.state(TestItem.TEST_ITEM);
                State part = script.state(TestStates.BODY_PART);
                assertTrue(item.applied());
                assertFalse(item.expired());
                assertTrue(state.applied());
                assertFalse(state.expired());
                assertTrue(part.applied());
                assertFalse(part.expired());

                debugger.advanceTime(2, TimeUnit.HOURS);
                script.triggerAutoRemove();
                assertFalse(item.applied());
                assertTrue(item.expired());
                assertFalse(state.applied());
                assertTrue(state.expired());
                assertFalse(part.applied());
                assertTrue(part.expired());
            }
        }
    }

    @Test
    public void testAutoRemovalWithoutDuration() throws IOException {
        try (TestScript script = new TestScript()) {
            Debugger debugger = script.debugger;
            debugger.freezeTime();

            {
                Item item = script.item(TestItem.TEST_ITEM);
                State state = script.state(TestItem.TEST_ITEM);
                State part = script.state(TestStates.BODY_PART);

                assertFalse(item.applied());
                assertTrue(item.expired());
                assertFalse(state.applied());
                assertTrue(state.expired());
                assertFalse(part.applied());
                assertTrue(part.expired());

                item.to(TestStates.BODY_PART).apply().remember(Until.Removed);
                assertTrue(item.applied());
                assertFalse(item.expired());
                assertTrue(part.applied());
                assertFalse(part.expired());
            }

            {
                debugger.clearStateMaps();

                Item item = script.item(TestItem.TEST_ITEM);
                State state = script.state(TestItem.TEST_ITEM);
                State part = script.state(TestStates.BODY_PART);
                assertTrue(item.applied());
                assertFalse(item.expired());
                assertTrue(state.applied());
                assertFalse(state.expired());
                assertTrue(part.applied());
                assertFalse(part.expired());

                debugger.advanceTime(2, TimeUnit.HOURS);
                assertTrue(item.applied());
                assertFalse(item.expired());
                assertTrue(state.applied());
                assertFalse(state.expired());
                assertTrue(part.applied());
                assertFalse(part.expired());
            }

            {
                debugger.clearStateMaps();

                Item item = script.item(TestItem.TEST_ITEM);
                State state = script.state(TestItem.TEST_ITEM);
                State part = script.state(TestStates.BODY_PART);
                assertTrue(item.applied());
                assertFalse(item.expired());
                assertTrue(state.applied());
                assertFalse(state.expired());
                assertTrue(part.applied());
                assertFalse(part.expired());

                script.triggerAutoRemove();
                assertTrue(item.applied());
                assertFalse(item.expired());
                assertTrue(state.applied());
                assertFalse(state.expired());
                assertTrue(part.applied());
                assertFalse(part.expired());

                debugger.advanceTime(64, TimeUnit.HOURS);
                script.triggerAutoRemove();
                assertTrue(item.applied());
                assertFalse(item.expired());
                assertTrue(state.applied());
                assertFalse(state.expired());
                assertTrue(part.applied());
                assertFalse(part.expired());

            }
        }
    }

}
