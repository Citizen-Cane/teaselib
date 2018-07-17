package teaselib.core.devices;

import teaselib.core.TeaseLib;
import teaselib.core.util.Persist;
import teaselib.core.util.ReflectionUtils;

/**
 * @author Citizen-Cane
 *
 */
public abstract class ReleaseAction extends ActionState {
    public ReleaseAction(TeaseLib teaseLib, String domain, String item,
            Class<? extends ReleaseAction> implementationClass) {
        // TODO Class is wrong, must be derived class
        super(teaseLib, domain, ReflectionUtils.normalizeClassName(implementationClass) + "." + item);
    }

    public ReleaseAction(Persist.Storage storage, Class<? extends ReleaseAction> implementationClass) {
        this(storage.getInstance(TeaseLib.class), storage.next(), storage.next(), implementationClass);
    }

    @Override
    public Persistence removeFrom(Object... peers2) {
        performAction();
        return super.removeFrom(peers2);
    }
}
