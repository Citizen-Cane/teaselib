package teaselib.core.devices;

import java.util.Arrays;
import java.util.List;

import teaselib.core.StateImpl;
import teaselib.core.TeaseLib;
import teaselib.core.util.Persist;
import teaselib.core.util.QualifiedItem;

/**
 * @author Citizen-Cane
 *
 */
public abstract class ActionState extends StateImpl implements Persist.Persistable {
    public ActionState(TeaseLib teaseLib, String domain, Object item) {
        super(teaseLib, domain, item);
    }

    @Override
    public List<String> persisted() {
        if (getClass().isAnonymousClass()) {
            throw new UnsupportedOperationException("Can't persists anonymous class " + this);
        } else {
            return Arrays.asList(Persist.persist(domain), Persist.persist(QualifiedItem.of(item).name()));
        }
    }

    protected abstract boolean performAction();
}