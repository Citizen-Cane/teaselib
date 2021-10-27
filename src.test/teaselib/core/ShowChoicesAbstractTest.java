package teaselib.core;

import java.io.IOException;

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

    protected void init() throws IOException {
        script = new TestScript();
        debugger = script.debugger;
        debugger.freezeTime();
    }

    public void cleanup() {
        script.close();
    }

}
