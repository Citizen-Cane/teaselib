package teaselib.core;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import teaselib.test.TestScript;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

public class TeaseLibTest {
    @Test
    public void testFreezeTime() throws IOException {
        try (TestScript script = new TestScript()) {
            TeaseLib teaseLib = script.teaseLib;
            teaseLib.freezeTime();

            long expected = teaseLib.getTime(TimeUnit.MILLISECONDS);
            teaseLib.freezeTime();
            long actual = teaseLib.getTime(TimeUnit.MILLISECONDS);
            Assertions.assertEquals(expected, actual);
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
            Assertions.assertEquals(expected, actual);
        }
    }
}
