package teaselib.core.devices;

import teaselib.State;
import teaselib.core.TeaseLib;
import teaselib.core.util.Persist;
import teaselib.core.util.ReflectionUtils;

/**
 * @author Citizen-Cane
 *
 */
public abstract class ReleaseAction extends ActionState {
    boolean actionPerformed = false;

    public ReleaseAction(TeaseLib teaseLib, String domain, String item, Class<? extends ReleaseAction> subClass) {
        super(teaseLib, domain, ReflectionUtils.normalizeClassName(subClass) + "." + item);
    }

    public ReleaseAction(Persist.Storage storage, Class<? extends ReleaseAction> implementationClass) {
        this(storage.getInstance(TeaseLib.class), storage.next(), storage.next(), implementationClass);
    }

    @Override
    public Options applyTo(Object... attributes) {
        try {
            return super.applyTo(attributes);
        } finally {
            actionPerformed = false;
        }
    }

    @Override
    public State removeFrom(Object... peers2) {
        performReleaseActionIfNecessary();
        return super.removeFrom(peers2);
    }

    @Override
    public State remove() {
        performReleaseActionIfNecessary();
        return super.remove();
    }

    protected void performReleaseActionIfNecessary() {
        if (!actionPerformed) {
            actionPerformed = performAction();
        }
    }
}
