package teaselib.core;

import teaselib.State;
import teaselib.core.util.QualifiedString;

/**
 * @author Citizen-Cane
 *
 */
public class TestableStateMaps extends StateMaps {

    public TestableStateMaps(TeaseLib teaseLib) {
        super(teaseLib);
    }

    protected State state(String domain, Enum<?> item) {
        return state(domain, QualifiedString.of(item));
    }

    protected State state(String domain, String item) {
        return state(domain, QualifiedString.of(item));
    }

}
