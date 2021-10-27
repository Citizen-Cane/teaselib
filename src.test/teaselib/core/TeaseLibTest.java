package teaselib.core;

import static org.junit.Assert.*;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import org.junit.Test;

import teaselib.test.TestScript;

public class TeaseLibTest {
    @Test
    public void testFreezeTime() throws IOException {
        try (TestScript script = new TestScript()) {
            TeaseLib teaseLib = script.teaseLib;
            teaseLib.freezeTime();

            long expected = teaseLib.getTime(TimeUnit.MILLISECONDS);
            teaseLib.freezeTime();
            long actual = teaseLib.getTime(TimeUnit.MILLISECONDS);
            assertEquals(expected, actual);
        }
    }

    @Test
    public void testAdvanceTime() throws IOException {
        try (TestScript script = new TestScript()) {
            TeaseLib teaseLib = script.teaseLib;
            teaseLib.freezeTime();

            teaseLib.advanceTime(1, TimeUnit.SECONDS);

            long expected = teaseLib.getTime(TimeUnit.SECONDS);
            teaseLib.freezeTime();
            long actual = teaseLib.getTime(TimeUnit.SECONDS);
            assertEquals(expected, actual);
        }
    }
}
