package teaselib.core.devices;

import teaselib.core.TeaseLib;
import teaselib.core.util.ReflectionUtils;
import teaselib.core.util.Storage;

/**
 * @author Citizen-Cane
 *
 */
public abstract class ReleaseAction extends ActionState {
    boolean actionPerformed = false;

    protected ReleaseAction(TeaseLib teaseLib, String domain, String item, Class<? extends ReleaseAction> subClass) {
        super(teaseLib, domain, ReflectionUtils.normalizedClassName(subClass) + "." + item);
    }

    protected ReleaseAction(Storage storage, Class<? extends ReleaseAction> implementationClass) {
        this(storage.getInstance(TeaseLib.class), storage.next(), storage.next(), implementationClass);
    }

    @Override
    public Options applyTo(Object... peers) {
        try {
            return super.applyTo(peers);
        } finally {
            actionPerformed = false;
        }
    }

    @Override
    public void removeFrom(Object... peers) {
        performReleaseActionIfNecessary();
        super.removeFrom(peers);
        // TODO needed but Why?
        remove();
        // TODO performReleaseActionIfNecessary() called twice
    }

    @Override
    public void remove() {
        performReleaseActionIfNecessary();
        super.remove();
    }

    protected void performReleaseActionIfNecessary() {
        if (!actionPerformed) {
            actionPerformed = performAction();
        }
    }
}
