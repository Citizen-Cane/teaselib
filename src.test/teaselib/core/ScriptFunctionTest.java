package teaselib.core;

import static org.junit.Assert.assertEquals;

import java.util.concurrent.atomic.AtomicReference;

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

    static abstract class RunnableTestScript extends TeaseScript implements RunnableScript {

        public RunnableTestScript(TeaseLib teaseLib) {
            super(teaseLib, new ResourceLoader(RunnableTestScript.class), TestScript.newActor(), TestScript.NAMESPACE);
        }

        public RunnableTestScript(Script script) {
            super(script);
        }

    }

    private static RunnableTestScript getMainScript() {
        RunnableTestScript mainScript = new RunnableTestScript(TestScript.teaseLib()) {
            @Override
            public void run() {
                // Empty
            }
        };
        return mainScript;
    }

    @Test
    public void testSimpleScript() {
        AtomicReference<String> result = new AtomicReference<>(null);
        RunnableTestScript mainScript = getMainScript();

        CodeCoverage<Script> codeCoverage = new CodeCoverage<>();
        codeCoverage.runAll(() -> new RunnableTestScript(mainScript) {
            @Override
            public void run() {
                say("In main script.");
                result.set(reply("Finished"));
                say("Still in main script.");
            }
        });
        assertEquals("Finished", result.get());
        // assertTrue("Wrong text in message renderer",
        // mainScript.scriptRenderer.renderMessage.toString().indexOf("Still in main script.") >= 0);
    }

    @Test
    public void testThatCallableScriptFunctionHasCompletedWhenNextSayStatementIsExecuted() {
        AtomicReference<String> result = new AtomicReference<>(null);
        RunnableTestScript mainScript = getMainScript();

        CodeCoverage<Script> codeCoverage = new CodeCoverage<>();
        codeCoverage.runAll(() -> new RunnableTestScript(mainScript) {
            @Override
            public void run() {
                say("In main script.");
                result.set(reply(() -> {
                    say("Inside script function.");
                    return ScriptFunction.Timeout;
                }, "Finished"));
                say("Resuming main script.");
            }
        });
        assertEquals("Finished", result.get());
        // assertTrue("Script function still running while resuming to main script thread",
        // mainScript.scriptRenderer.renderMessage.toString().indexOf("Resuming main script.") >= 0);
    }

    @Test
    public void testThatRunnableScriptFunctionHasCompletedWhenNextSayStatementIsExecuted() {
        AtomicReference<String> result = new AtomicReference<>(null);
        RunnableTestScript mainScript = getMainScript();

        CodeCoverage<TeaseScript> codeCoverage = new CodeCoverage<>();
        codeCoverage.runAll(() -> new RunnableTestScript(mainScript) {
            @Override
            public void run() {
                say("In main script.");
                result.set(reply(() -> {
                    say("Inside script function.");
                }, "Finished"));
                say("Resuming main script.");
            }
        });
        assertEquals("Finished", result.get());
        // assertTrue("Script function still running while resuming to main script thread",
        // mainScript.scriptRenderer.renderMessage.toString().indexOf("Resuming main script.") >= 0);
    }

    @Test
    public void testEmptyScriptFunctionAlwaysTimesOut() {
        AtomicReference<String> result = new AtomicReference<>(null);
        RunnableTestScript mainScript = getMainScript();

        CodeCoverage<TeaseScript> codeCoverage = new CodeCoverage<>();
        codeCoverage.runAll(() -> new RunnableTestScript(mainScript) {
            @Override
            public void run() {
                say("In main script.");
                result.set(reply(() -> {
                }, "Finished"));
                say("Resuming main script.");
            }
        });
        assertEquals(ScriptFunction.Timeout, result.get());
        // assertTrue("Script function still running while resuming to main script thread",
        // mainScript.scriptRenderer.renderMessage.toString().indexOf("Resuming main script.") >= 0);
    }

    @Test
    public void testDefaultScriptFunctionTimeout() {
        AtomicReference<String> result = new AtomicReference<>(null);
        RunnableTestScript mainScript = getMainScript();

        CodeCoverage<TeaseScript> codeCoverage = new CodeCoverage<>();
        codeCoverage.runAll(() -> new RunnableTestScript(mainScript) {
            @Override
            public void run() {
                say("In main script.");
                result.set(reply(timeout(5), "Finished"));
                say("Resuming main script.");
            }
        });
        assertEquals("Finished", result.get());
        // assertTrue("Script function still running while resuming to main script thread",
        // mainScript.scriptRenderer.renderMessage.toString().indexOf("Resuming main script.") >= 0);
    }

    @Test
    public void testDefaultScriptFunctionTimeoutConfirm() {
        AtomicReference<String> result = new AtomicReference<>(null);
        RunnableTestScript mainScript = getMainScript();

        // TODO Reused prompt
        CodeCoverage<TeaseScript> codeCoverage = new CodeCoverage<>();
        codeCoverage.run(() -> new RunnableTestScript(mainScript) {
            @Override
            public void run() {
                say("In main script.");
                result.set(reply(timeoutWithConfirmation(5), "Finished"));
                say("Resuming main script.");
            }
        });

        assertEquals("Finished", result.get());
        // assertTrue("Script function still running while resuming to main script thread",
        // mainScript.scriptRenderer.renderMessage.toString().indexOf("Resuming main script.") >= 0);
    }

    @Test
    public void testDefaultScriptFunctionTimeoutAutoConfirm() {
        AtomicReference<String> result = new AtomicReference<>(null);
        RunnableTestScript mainScript = getMainScript();

        CodeCoverage<TeaseScript> codeCoverage = new CodeCoverage<>();
        codeCoverage.runAll(() -> new RunnableTestScript(mainScript) {
            @Override
            public void run() {
                say("In main script.");
                result.set(reply(timeoutWithAutoConfirmation(5), "Finished"));
                say("Resuming main script.");
            }
        });
        assertEquals("Finished", result.get());
        // assertTrue("Script function still running while resuming to main script thread",
        // mainScript.scriptRenderer.renderMessage.toString().indexOf("Resuming main script.") >= 0);
    }

}
