package teaselib.core.devices;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import teaselib.Duration;
import teaselib.State;
import teaselib.core.StateImpl;
import teaselib.core.StateMaps;
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

        private static final Set<Object> intrinsicAttributes = Collections
                .unmodifiableSet(new HashSet<>(Arrays.asList(State.Available)));

        @Override
        public boolean is(Object... attributes) {
            return StateMaps.hasAllAttributes(intrinsicAttributes, attributes) && super.is(attributes);
        }

        @Override
        public Options apply() {
            // Ignore
            return this;
        }

        @Override
        public Options applyTo(Object... attributes) {
            // Ignore
            return this;
        }

        @Override
        public void applyAttributes(Object... attributes) {
            // Ignore
        }

        @Override
        public State over(long limit, TimeUnit unit) {
            // Ignore
            return this;
        }

        @Override
        public State over(Duration duration) {
            // Ignore
            return this;
        }

        @Override
        protected boolean performAction() {
            // Ignore
            return true;
        }
    }

    public static boolean isActionState(Object object) {
        return object instanceof ActionState;
    }

    public static boolean isntActionState(Object object) {
        return !(object instanceof ActionState);
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