package teaselib.core;

import static org.junit.Assert.assertEquals;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import teaselib.ScriptFunction;
import teaselib.core.Debugger.Response;
import teaselib.core.debug.CheckPoint;
import teaselib.core.debug.CheckPointListener;
import teaselib.core.debug.TimeAdvanceListener;
import teaselib.test.TestScript;

public class CheckPointTest {
    static final Logger logger = LoggerFactory.getLogger(CheckPointTest.class);

    abstract static class CheckPointTester implements CheckPointListener {
        AtomicInteger actual = new AtomicInteger(0);
        AtomicReference<Exception> exception = new AtomicReference<>();

        void unhandled(CheckPoint checkPoint) {
            if (exception.get() == null) {
                RuntimeException e = new IllegalStateException("Checkpoint not handled: " + checkPoint);
                exception.set(e);
                throw e;
            } else {
                throw new IllegalStateException("Additional errors after " + exception.get().getMessage(),
                        exception.get());
            }
        }

        void throwCatchedException() throws Exception {
            if (exception.get() != null) {
                throw exception.get();
            }
        }
    }

    @Test
    public void testCheckPointNewMeessage() throws Exception {
        TestScript script = TestScript.getOne();
        CheckPointTester checkPoints = genericSequential();

        script.teaseLib.addCheckPointListener(checkPoints);
        script.say("test");
        script.teaseLib.removeCheckPointListener(checkPoints);

        checkPoints.throwCatchedException();
        assertEquals(1, checkPoints.actual.get());
    }

    private static CheckPointTester genericSequential() {
        return new CheckPointTester() {
            @Override
            public void checkPointReached(CheckPoint checkPoint) {
                if (checkPoint == CheckPoint.Script.NewMessage) {
                    assertEquals(1, actual.incrementAndGet());
                } else {
                    unhandled(checkPoint);
                }
            }
        };
    }

    @Test
    public void testCheckPointScriptFunction() throws Exception {
        TestScript script = TestScript.getOne();
        CheckPointTester checkPoints = genericScriptFunction(false);

        script.debugger.addResponse("Finished", Response.Ignore);
        script.teaseLib.addCheckPointListener(checkPoints);
        assertEquals(ScriptFunction.TimeoutString, script.reply(() -> script.say("test"), "Finished"));
        script.teaseLib.removeCheckPointListener(checkPoints);

        checkPoints.throwCatchedException();
        assertEquals(3, checkPoints.actual.get());
    }

    @Test
    public void testCheckPointScriptFunctionWithoutTimeAdvanceInThreads() throws Exception {
        TestScript script = TestScript.getOne();
        script.debugger.freezeTime();
        CheckPointTester checkPoints = genericScriptFunction(false);
        TimeAdvanceListener tal = e -> assertEquals(3, checkPoints.actual.incrementAndGet());

        script.debugger.addResponse("Finished", Response.Ignore);
        script.teaseLib.addCheckPointListener(checkPoints);
        script.teaseLib.addTimeAdvancedListener(tal);
        assertEquals(ScriptFunction.TimeoutString, script.reply(() -> script.say("test"), "Finished"));
        script.teaseLib.removeCheckPointListener(checkPoints);
        script.teaseLib.removeTimeAdvancedListener(tal);

        checkPoints.throwCatchedException();
        assertEquals(3, checkPoints.actual.get());
    }

    @Test
    public void testCheckPointScriptFunctionAndTimeListener() throws Exception {
        TestScript script = TestScript.getOne();
        script.debugger.freezeTime();
        script.debugger.advanceTimeAllThreads();

        CheckPointTester checkPoints = genericScriptFunction(true);
        TimeAdvanceListener tal = e -> assertEquals(3, checkPoints.actual.incrementAndGet());

        script.debugger.addResponse("Finished", Response.Ignore);
        script.teaseLib.addCheckPointListener(checkPoints);
        script.teaseLib.addTimeAdvancedListener(tal);
        assertEquals(ScriptFunction.TimeoutString, script.reply(() -> script.say("test"), "Finished"));
        script.teaseLib.removeCheckPointListener(checkPoints);
        script.teaseLib.removeTimeAdvancedListener(tal);

        checkPoints.throwCatchedException();
        assertEquals(4, checkPoints.actual.get());
    }

    @Test
    // TODO make test succeed after changing PromptQueue to make prompt actice, the nstart script function
    // -> to avoid deadlock when canceling script function after showing a prompt failed
    // showing prompt first also reduces complexity, and makes error handling easier
    public void testCheckPointScriptFunctionAndTimeListenerWithResponse() throws Exception {
        TestScript script = TestScript.getOne();
        CheckPointTester checkPoints = genericScriptFunction(true);
        TimeAdvanceListener tal = e -> assertEquals(3, checkPoints.actual.incrementAndGet());

        script.debugger.addResponse("Finished", Response.Choose);
        script.teaseLib.addCheckPointListener(checkPoints);
        script.teaseLib.addTimeAdvancedListener(tal);
        assertEquals("Finished", script.reply(() -> script.say("test"), "Finished"));
        script.teaseLib.removeCheckPointListener(checkPoints);
        script.teaseLib.removeTimeAdvancedListener(tal);

        checkPoints.throwCatchedException();
        assertEquals(2, checkPoints.actual.get());
    }

    private static CheckPointTester genericScriptFunction(boolean withTimeAdvanceListener) {
        return new CheckPointTester() {
            @Override
            public void checkPointReached(CheckPoint checkPoint) {
                logger.info("Reached checkpoint {}", checkPoint);
                if (checkPoint == CheckPoint.ScriptFunction.Started) {
                    assertEquals(1, actual.incrementAndGet());
                } else if (checkPoint == CheckPoint.Script.NewMessage) {
                    assertEquals(2, actual.incrementAndGet());
                } else if (checkPoint == CheckPoint.ScriptFunction.Finished) {
                    assertEquals(withTimeAdvanceListener ? 4 : 3, actual.incrementAndGet());
                } else {
                    unhandled(checkPoint);
                }
            }
        };
    }

}
