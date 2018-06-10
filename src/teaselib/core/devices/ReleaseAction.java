package teaselib.core.devices;

import java.util.Arrays;
import java.util.List;

import teaselib.core.StateImpl;
import teaselib.core.TeaseLib;
import teaselib.core.devices.ReleaseActionTest.ReleaseActionState;
import teaselib.core.util.Persist;
import teaselib.core.util.QualifiedItem;
import teaselib.core.util.ReflectionUtils;

/**
 * @author Citizen-Cane
 *
 */
public abstract class ReleaseAction extends StateImpl implements Persist.Persistable {
    public ReleaseAction(TeaseLib teaseLib, String domain, String item) {
        super(teaseLib, domain, ReflectionUtils.normalizeClassName(ReleaseActionState.class) + "." + item);
    }

    @Override
    public List<String> persisted() {
        if (getClass().isAnonymousClass()) {
            throw new UnsupportedOperationException("Can't persists anonymous class " + this);
        } else {
            return Arrays.asList(Persist.persist(domain), Persist.persist(QualifiedItem.of(item).name()));
        }
    }

    public ReleaseAction(Persist.Storage storage) {
        this(storage.getInstance(TeaseLib.class), storage.next(), storage.next());
    }

    @Override
    public Persistence removeFrom(Object... peers2) {
        performAction();
        return super.removeFrom(peers2);
    }

    protected abstract void performAction();
}
