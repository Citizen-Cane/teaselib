package teaselib.core.devices;

import teaselib.core.TeaseLib;
import teaselib.core.util.ReflectionUtils;
import teaselib.core.util.Storage;

/**
 * @author Citizen-Cane
 *
 */
public abstract class ApplyAction extends ActionState {
    boolean actionPerformed = false;

    protected ApplyAction(TeaseLib teaseLib, String domain, String item, Class<? extends ApplyAction> subClass) {
        super(teaseLib, domain, ReflectionUtils.normalizedClassName(subClass) + "." + item);
    }

    protected ApplyAction(Storage storage, Class<? extends ApplyAction> implementationClass) {
        this(storage.getInstance(TeaseLib.class), storage.next(), storage.next(), implementationClass);
    }

    @Override
    public Options apply() {
        performApplyActionIfNecessary();
        return super.apply();
    }

    @Override
    public Options applyTo(Object... peers) {
        performApplyActionIfNecessary();
        return super.applyTo(peers);
    }

    @Override
    public void removeFrom(Object... peers) {
        try {
            super.removeFrom(peers);
        } finally {
            actionPerformed = false;
        }
    }

    protected void performApplyActionIfNecessary() {
        if (!actionPerformed) {
            actionPerformed = performAction();
        }
    }

}
