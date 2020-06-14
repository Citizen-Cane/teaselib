package teaselib.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

import org.junit.Test;

import teaselib.ScriptFunction;
import teaselib.TeaseScript;
import teaselib.functional.RunnableScript;
import teaselib.test.TestScript;

/**
 * @author Citizen-Cane
 *
 */
public class ScriptFunctionTest {
    private static final String TEST = "test";

    @Test
    public void testThatScriptFunctionWithoutDelayReturnsCorrectValue() {
        TestScript script = TestScript.getOne();
        script.debugger.freezeTime();
        String choice = "false";
        script.debugger.addResponse(choice, Debugger.Response.Ignore);

        assertEquals(TEST, script.reply(this::returnImmediately, choice));
    }

    @Test
    public void testThatScriptFunctionWithDelayReturnsCorrectValue() {
        TestScript script = TestScript.getOne();
        script.debugger.freezeTime();
        String choice = "false";
        script.debugger.addResponse(choice, Debugger.Response.Ignore);

        assertEquals(TEST, script.reply(() -> returnAfterDelay(script), choice));
    }

    private String returnImmediately() {
        return TEST;
    }

    private static String returnAfterDelay(TeaseScript script) {
        script.say("something");
        return TEST;
    }

    abstract static class RunnableTestScript extends TeaseScript implements RunnableScript {

        public RunnableTestScript(TeaseLib teaseLib) {
            super(teaseLib, new ResourceLoader(RunnableTestScript.class), TestScript.newActor(), TestScript.NAMESPACE);
        }

        public RunnableTestScript(Script script) {
            super(script);
        }

    }

    private static RunnableTestScript getMainScript() {
        return new RunnableTestScript(TestScript.teaseLib()) {
            @Override
            public void run() {
                // Empty
            }
        };
    }

    @Test
    public void testSimpleScript() {
        AtomicReference<String> result = new AtomicReference<>(null);

        CodeCoverage<Script> codeCoverage = new CodeCoverage<>();
        Supplier<RunnableTestScript> scriptSupplier = () -> new RunnableTestScript(getMainScript()) {
            @Override
            public void run() {
                say("In main script.");
                result.set(reply("Finished"));
                say("Still in main script.");
            }
        };

        codeCoverage.run(scriptSupplier);
        assertEquals("Finished", result.get());
        assertFalse(codeCoverage.hasNext());
    }

    @Test
    public void testThatCallableScriptFunctionHasCompletedWhenNextSayStatementIsExecuted() {
        AtomicReference<String> result = new AtomicReference<>(null);

        CodeCoverage<Script> codeCoverage = new CodeCoverage<>();
        Supplier<RunnableTestScript> scriptSupplier = () -> new RunnableTestScript(getMainScript()) {
            @Override
            public void run() {
                say("In main script.");
                result.set(reply(() -> {
                    say("Inside script function.");
                    return ScriptFunction.TimeoutString;
                }, "Finished"));
                say("Resuming main script.");
            }
        };

        codeCoverage.run(scriptSupplier);
        assertEquals(ScriptFunction.TimeoutString, result.get());

        codeCoverage.run(scriptSupplier);
        assertEquals("Finished", result.get());

        assertFalse(codeCoverage.hasNext());
    }

    @Test
    public void testThatRunnableScriptFunctionHasCompletedWhenNextSayStatementIsExecuted() {
        AtomicReference<String> result = new AtomicReference<>(null);

        CodeCoverage<TeaseScript> codeCoverage = new CodeCoverage<>();
        Supplier<RunnableTestScript> scriptSupplier = () -> new RunnableTestScript(getMainScript()) {
            @Override
            public void run() {
                say("In main script.");
                result.set(reply(() -> {
                    say("Inside script function.");
                }, "Finished"));
                say("Resuming main script.");
            }
        };

        codeCoverage.run(scriptSupplier);
        assertEquals(ScriptFunction.TimeoutString, result.get());

        codeCoverage.run(scriptSupplier);
        assertEquals("Finished", result.get());

        assertFalse(codeCoverage.hasNext());
    }

    @Test
    public void testEmptyScriptFunctionAlwaysTimesOut() {
        AtomicReference<String> result = new AtomicReference<>(null);

        CodeCoverage<TeaseScript> codeCoverage = new CodeCoverage<>();
        Supplier<RunnableTestScript> scriptSupplier = () -> new RunnableTestScript(getMainScript()) {
            @Override
            public void run() {
                say("In main script.");
                result.set(reply(() -> {
                }, "Finished"));
                say("Resuming main script.");
            }
        };

        codeCoverage.run(scriptSupplier);
        assertEquals(ScriptFunction.TimeoutString, result.get());

        codeCoverage.run(scriptSupplier);
        assertEquals("Finished", result.get());

        assertFalse(codeCoverage.hasNext());
    }

    @Test
    public void testDefaultScriptFunctionTimeout() {
        AtomicReference<String> result = new AtomicReference<>(null);

        CodeCoverage<TeaseScript> codeCoverage = new CodeCoverage<>();
        Supplier<RunnableTestScript> scriptSupplier = () -> new RunnableTestScript(getMainScript()) {
            @Override
            public void run() {
                say("In main script.");
                result.set(reply(timeout(5), "Finished"));
                say("Resuming main script.");
            }
        };

        codeCoverage.run(scriptSupplier);
        assertEquals(ScriptFunction.TimeoutString, result.get());

        codeCoverage.run(scriptSupplier);
        assertEquals("Finished", result.get());

        assertFalse(codeCoverage.hasNext());
    }

    @Test
    public void testDefaultScriptFunctionTimeoutConfirm() {
        AtomicReference<String> result = new AtomicReference<>(null);

        CodeCoverage<TeaseScript> codeCoverage = new CodeCoverage<>();
        Supplier<RunnableTestScript> scriptSupplier = () -> new RunnableTestScript(getMainScript()) {
            @Override
            public void run() {
                say("In main script.");
                result.set(reply(timeoutWithConfirmation(5), "Finished"));
                say("Resuming main script.");
            }
        };

        codeCoverage.run(scriptSupplier);
        assertEquals(ScriptFunction.TimeoutString, result.get());

        codeCoverage.run(scriptSupplier);
        assertEquals("Finished", result.get());

        assertFalse(codeCoverage.hasNext());
    }

    @Test
    public void testDefaultScriptFunctionTimeoutAutoConfirm() {
        AtomicReference<String> result = new AtomicReference<>(null);

        CodeCoverage<TeaseScript> codeCoverage = new CodeCoverage<>();
        Supplier<RunnableTestScript> scriptSupplier = () -> new RunnableTestScript(getMainScript()) {
            @Override
            public void run() {
                say("In main script.");
                result.set(reply(timeoutWithAutoConfirmation(5), "Finished"));
                say("Resuming main script.");
            }
        };

        codeCoverage.run(scriptSupplier);
        assertEquals(ScriptFunction.TimeoutString, result.get());

        codeCoverage.run(scriptSupplier);
        assertEquals("Finished", result.get());

        assertFalse(codeCoverage.hasNext());
    }

    @Test
    public void testThatCodeCoverageCoversTimeout() {
        AtomicReference<String> result1 = new AtomicReference<>(null);
        AtomicReference<String> result2 = new AtomicReference<>(null);

        CodeCoverage<TeaseScript> codeCoverage = new CodeCoverage<>();
        Supplier<RunnableTestScript> scriptSupplier = () -> new RunnableTestScript(getMainScript()) {
            @Override
            public void run() {
                result1.set(null);
                say("In main script 1.");
                result1.set(reply(() -> {
                    say("Inside script function 1.");
                }, "Finished 1"));
                say("Resuming main script 1.");

                result2.set(null);
                say("In main script 2.");
                result2.set(reply(() -> {
                    say("Inside script function 2.");
                }, "Finished 2"));
                say("Resuming main script 2.");
            }
        };

        codeCoverage.run(scriptSupplier);
        assertEquals(ScriptFunction.TimeoutString, result1.get());
        assertEquals(ScriptFunction.TimeoutString, result2.get());

        codeCoverage.run(scriptSupplier);
        assertEquals("Finished 1", result1.get());
        assertEquals(ScriptFunction.TimeoutString, result2.get());

        codeCoverage.run(scriptSupplier);
        assertEquals(ScriptFunction.TimeoutString, result1.get());
        assertEquals("Finished 2", result2.get());

        codeCoverage.run(scriptSupplier);
        assertEquals("Finished 1", result1.get());
        assertEquals("Finished 2", result2.get());

        assertFalse(codeCoverage.hasNext());
    }

}
