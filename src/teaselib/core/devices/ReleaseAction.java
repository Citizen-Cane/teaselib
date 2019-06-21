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
        // Fail early
        performReleaseActionIfNecessary();
        // Book-keeping
        super.removeFrom(peers);
        // Remove must be called explicitely in order
        // to change the action state to "not applied"
        remove();
        // This is because the action is performed when a single item is removed,
        // the action state has to be set to "not applied"
        // to indicate the action has been performed
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
