package teaselib.core;

import teaselib.core.configuration.DebugSetup;
import teaselib.test.TestScript;

/**
 * @author Citizen-Cane
 *
 */
public class ShowChoicesAbstractTest {
    protected static final DebugSetup DEBUG_SETUP = new DebugSetup();// .withOutput().withInput();
    protected static final int ITERATIONS = 1;

    protected TestScript script;
    protected Debugger debugger;

    public ShowChoicesAbstractTest() {
        super();
    }

    protected void init() {
        script = TestScript.getOne(DEBUG_SETUP);
        debugger = script.debugger;
        debugger.freezeTime();
    }
}