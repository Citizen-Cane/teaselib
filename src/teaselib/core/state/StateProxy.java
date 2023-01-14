/**
 * 
 */
package teaselib.core.state;

import java.util.Set;
import java.util.concurrent.TimeUnit;

import teaselib.Duration;
import teaselib.State;
import teaselib.core.ItemLogger;
import teaselib.core.ScriptEvents;
import teaselib.core.StateImpl;
import teaselib.core.util.QualifiedString;

/**
 * @author Citizen-Cane
 *
 */
public class StateProxy extends AbstractProxy<State> implements State, State.Attributes {

    private final ScriptEvents events;
    private ItemLogger itemLogger;

    public StateProxy(String namespace, State state, ScriptEvents events) {
        super(namespace, state);
        this.events = events;
        this.itemLogger = ((StateImpl) state).cache.teaseLib.itemLogger;
    }

    @Override
    public Options apply() {
        var stateImpl = (StateImpl) state;

        injectNamespace();
        var options = new StateOptionsProxy(stateImpl, namespace, state.apply(), itemLogger);
        itemLogger.log(stateImpl, "apply");

        events.stateApplied.fire(new ScriptEvents.StateChangedEventArgs(state));
        return options;
    }

    @Override
    public Options applyTo(Object... peers) {
        injectNamespace();
        var options = new StateOptionsProxy((StateImpl) state, namespace, state.applyTo(peers), itemLogger);
        itemLogger.log((StateImpl) state, "applyTo", peers);

        events.stateApplied.fire(new ScriptEvents.StateChangedEventArgs(state));
        return options;
    }

    private void injectNamespace() {
        ((State.Attributes) state).applyAttributes(namespace);

    }

    public Set<QualifiedString> peers() {
        return ((StateImpl) state).peers();
    }

    @Override
    public boolean is(Object... attributes) {
        boolean is = state.is(attributes);
        itemLogger.log((StateImpl) state, "is", attributes, is);
        return is;
    }

    @Override
    public boolean applied() {
        boolean applied = state.applied();
        itemLogger.log((StateImpl) state, "applied", applied);
        return applied;
    }

    @Override
    public boolean expired() {
        boolean expired = state.expired();
        itemLogger.log((StateImpl) state, "expired", expired);
        return expired;
    }

    @Override
    public Duration duration() {
        return state.duration();
    }

    @Override
    public void remove() {
        events.stateRemoved.fire(new ScriptEvents.StateChangedEventArgs(state));
        state.remove();
        itemLogger.log((StateImpl) state, "remove");
    }

    @Override
    public void removeFrom(Object... peers) {
        events.stateRemoved.fire(new ScriptEvents.StateChangedEventArgs(state));
        state.removeFrom(peers);
        itemLogger.log((StateImpl) state, "removeFRom", peers);

    }

    @Override
    public boolean removed() {
        boolean removed = state.removed();
        itemLogger.log((StateImpl) state, "removed", removed);
        return removed;
    }

    @Override
    public long removed(TimeUnit unit) {
        long removed = state.removed(unit);
        itemLogger.log((StateImpl) state, "removed", removed, unit);
        return removed;
    }

    @Override
    public void applyAttributes(Object... attributes) {
        ((State.Attributes) state).applyAttributes(attributes);
    }
}