package teaselib.core;

import static org.junit.Assert.*;

import java.util.concurrent.TimeUnit;

import org.junit.Test;

import teaselib.test.TestScript;

public class TeaseLibTest {
    @Test
    public void testFreezeTime() {
        TestScript script = TestScript.getOne();
        TeaseLib teaseLib = script.teaseLib;
        teaseLib.freezeTime();

        long expected = teaseLib.getTime(TimeUnit.MILLISECONDS);
        teaseLib.freezeTime();
        long actual = teaseLib.getTime(TimeUnit.MILLISECONDS);
        assertEquals(expected, actual);
    }

    @Test
    public void testAdvanceTime() {
        TestScript script = TestScript.getOne();
        TeaseLib teaseLib = script.teaseLib;
        teaseLib.freezeTime();

        teaseLib.advanceTime(1, TimeUnit.SECONDS);

        long expected = teaseLib.getTime(TimeUnit.SECONDS);
        teaseLib.freezeTime();
        long actual = teaseLib.getTime(TimeUnit.SECONDS);
        assertEquals(expected, actual);
    }
}
