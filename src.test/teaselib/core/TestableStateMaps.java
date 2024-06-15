package teaselib.core;

import teaselib.State;
import teaselib.core.util.QualifiedString;
import teaselib.test.TestScript;

/**
 * @author Citizen-Cane
 *
 */
public class TestableStateMaps extends StateMaps {

    protected final TestScript script;
    public TestableStateMaps(TestScript script) {
        super(script.teaseLib);
        this.script=script;
    }

    protected State state(String domain, Enum<?> item) {
        return state(domain, QualifiedString.of(item));
    }

    protected State state(String domain, String item) {
        return state(domain, QualifiedString.of(item));
    }

}
