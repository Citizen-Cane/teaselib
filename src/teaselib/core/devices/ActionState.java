package teaselib.core.devices;

import java.util.Arrays;
import java.util.List;

import teaselib.core.StateImpl;
import teaselib.core.TeaseLib;
import teaselib.core.util.Persist;
import teaselib.core.util.QualifiedItem;
import teaselib.core.util.Storage;

/**
 * @author Citizen-Cane
 *
 */
public abstract class ActionState extends StateImpl implements Persist.Persistable {
    public static final class NotAvailable extends ActionState {
        public NotAvailable(TeaseLib teaseLib, String domain, Object item) {
            super(teaseLib, domain, item);
        }

        public NotAvailable(Storage storage) {
            this(storage.getInstance(TeaseLib.class), storage.next(), storage.next());
        }

        public static String stateName() {
            return ActionState.persistedInstance(ActionState.NotAvailable.class, TeaseLib.DefaultDomain,
                    "ReleaseAction.NotAvailable");

        }

        @Override
        protected boolean performAction() {
            return true;
        }
    }

    public static String persistedInstance(Class<? extends ActionState> clazz, String domain, String item) {
        return Persist.persistedInstance(clazz, Arrays.asList(domain, item));
    }

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