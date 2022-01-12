package teaselib.core;

import java.io.IOException;

import org.junit.After;
import org.junit.Before;

import teaselib.Actor;
import teaselib.TeaseScript;
import teaselib.test.TestScript;

/**
 * @author Citizen-Cane
 *
 */
public class ShowChoicesAbstractTest {
    protected static final int ITERATIONS = 1;

    protected TestScript script;
    protected Debugger debugger;

    public ShowChoicesAbstractTest() {
        super();
    }

    @Before
    public void initTestScript() throws IOException {
        script = new TestScript();
        debugger = script.debugger;
        debugger.freezeTime();
    }

    @After
    public void cleanup() {
        script.close();
    }

    static class TestException extends RuntimeException {
        private static final long serialVersionUID = 1L;

        public TestException() {
            this("test");
        }

        public TestException(String message) {
            super(message);
        }
    }

    abstract static class RunnableTestScript extends TeaseScript implements Runnable {
        RunnableTestScript(Script script) {
            super(script);
        }

        public RunnableTestScript(TeaseLib teaseLib, ResourceLoader resourceLoader, Actor actor, String namespace) {
            super(teaseLib, resourceLoader, actor, namespace);
        }
    }

    static void throwScriptInterruptedException() {
        throw new ScriptInterruptedException();
    }

    static void interruptScript() {
        Thread.currentThread().interrupt();
    }

}
