package teaselib.core;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import teaselib.Actor;
import teaselib.TeaseScript;
import teaselib.test.TestScript;

import java.io.IOException;
import java.io.Serial;

/**
 * @author Citizen-Cane
 */
class ShowChoicesAbstractTest {
    protected static final int ITERATIONS = 1;

    protected TestScript script;
    protected Debugger debugger;

    public ShowChoicesAbstractTest() {
        super();
    }

    @BeforeEach
    public void initTestScript() throws IOException {
        script = new TestScript();
        debugger = script.debugger;
        debugger.freezeTime();
    }

    @AfterEach
    public void cleanup() {
        script.close();
    }

    static class TestException extends RuntimeException {
        @Serial
        private static final long serialVersionUID = 1L;

        public TestException() {
            this("test");
        }

        public TestException(String message) {
            super(message);
        }
    }

    abstract static class RunnableTestScript extends TeaseScript implements Runnable {
        RunnableTestScript(TeaseScript script) {
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
