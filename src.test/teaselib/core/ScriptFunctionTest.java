package teaselib.core;

import static org.junit.Assert.*;

import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.Test;

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

        public RunnableTestScript(Script script) {
            super(script);
        }

    }

    @Test
    public void testThatScriptFunctionHasCompletedWhenNextSayStatementIsExecuted() {
        TestScript mainScript = TestScript.getOne();
        CodeCoverage<TeaseScript> codeCoverage = new CodeCoverage<>(() -> new RunnableTestScript(mainScript) {
            @Override
            public void run() {
                // TODO Indeterministic behavior (stop or timeout) because of
                // race condition between code coverage input handler and the script function
                reply(() -> {
                    say("Inside script function.");
                }, "Stop");
                say("Resuming main script.");
            }
        });
        codeCoverage.runAll();
        assertTrue("Sccript function still running while resuming to main script thread",
                mainScript.scriptRenderer.renderMessage.toString().indexOf("Resuming main script.") >= 0);
    }

}
