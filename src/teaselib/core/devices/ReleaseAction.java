package teaselib.core.devices;

import teaselib.core.TeaseLib;
import teaselib.core.devices.ReleaseActionTest.ReleaseActionState;
import teaselib.core.util.Persist;
import teaselib.core.util.ReflectionUtils;

/**
 * @author Citizen-Cane
 *
 */
public abstract class ReleaseAction extends ActionState {
    public ReleaseAction(TeaseLib teaseLib, String domain, String item) {
        super(teaseLib, domain, ReflectionUtils.normalizeClassName(ReleaseActionState.class) + "." + item);
    }

    public ReleaseAction(Persist.Storage storage) {
        this(storage.getInstance(TeaseLib.class), storage.next(), storage.next());
    }

    @Override
    public Persistence removeFrom(Object... peers2) {
        performAction();
        return super.removeFrom(peers2);
    }
}
