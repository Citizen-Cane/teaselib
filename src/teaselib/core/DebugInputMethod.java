package teaselib.core;

import teaselib.core.ui.InputMethod;

/**
 * @author citizen-cane
 *
 */
public interface DebugInputMethod extends InputMethod {

    void attach(TeaseLib teaseLib);

    void detach(TeaseLib teaseLib);

}