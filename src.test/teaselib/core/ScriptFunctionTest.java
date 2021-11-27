package teaselib.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import java.io.IOException;
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
    public void testThatScriptFunctionWithoutDelayReturnsCorrectValue() throws IOException {
        try (TestScript script = new TestScript()) {
            script.debugger.freezeTime();
            String choice = "false";
            script.debugger.addResponse(choice, Debugger.Response.Ignore);

            assertEquals(TEST, script.reply(this::returnImmediately, choice));
        }
    }

    @Test
    public void testThatScriptFunctionWithDelayReturnsCorrectValue() throws IOException {
        try (TestScript script = new TestScript()) {
            script.debugger.freezeTime();
            String choice = "false";
            script.debugger.addResponse(choice, Debugger.Response.Ignore);

            assertEquals(TEST, script.reply(() -> returnAfterDelay(script), choice));
        }
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
        return new RunnableTestScript(TestScript.newTeaseLib()) {
            @Override
            public void run() {
                // Empty
            }
        };
    }

    @Test
    public void testSimpleScript() {
        try (CodeCoverage<Script> codeCoverage = new CodeCoverage<>()) {
            AtomicReference<String> result = new AtomicReference<>(null);
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
    }

    @Test
    public void testThatCallableScriptFunctionHasCompletedWhenNextSayStatementIsExecuted() {
        try (CodeCoverage<Script> codeCoverage = new CodeCoverage<>()) {
            AtomicReference<String> result = new AtomicReference<>(null);
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
    }

    @Test
    public void testThatRunnableScriptFunctionHasCompletedWhenNextSayStatementIsExecuted() {
        try (CodeCoverage<TeaseScript> codeCoverage = new CodeCoverage<>()) {
            AtomicReference<String> result = new AtomicReference<>(null);
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
    }

    @Test
    public void testEmptyScriptFunctionAlwaysTimesOut() {
        try (CodeCoverage<TeaseScript> codeCoverage = new CodeCoverage<>()) {
            AtomicReference<String> result = new AtomicReference<>(null);
            Supplier<RunnableTestScript> scriptSupplier = () -> new RunnableTestScript(getMainScript()) {
                @Override
                public void run() {
                    say("In main script.");
                    result.set(reply(() -> {
                        // Empty
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
    }

    @Test
    public void testDefaultScriptFunctionTimeout() {
        try (CodeCoverage<TeaseScript> codeCoverage = new CodeCoverage<>()) {
            AtomicReference<String> result = new AtomicReference<>(null);
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
    }

    @Test
    public void testDefaultScriptFunctionTimeoutConfirm() {
        try (CodeCoverage<TeaseScript> codeCoverage = new CodeCoverage<>()) {
            AtomicReference<String> result = new AtomicReference<>(null);
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
    }

    @Test
    public void testDefaultScriptFunctionTimeoutAutoConfirm() {
        try (CodeCoverage<TeaseScript> codeCoverage = new CodeCoverage<>()) {
            AtomicReference<String> result = new AtomicReference<>(null);
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
    }

    @Test
    public void testThatCodeCoverageCoversTimeout() {
        try (CodeCoverage<TeaseScript> codeCoverage = new CodeCoverage<>()) {
            AtomicReference<String> result1 = new AtomicReference<>(null);
            AtomicReference<String> result2 = new AtomicReference<>(null);
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

}
