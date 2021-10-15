/**
 * 
 */
package teaselib.core.state;

import java.util.Set;
import java.util.concurrent.TimeUnit;

import teaselib.Duration;
import teaselib.State;
import teaselib.core.ScriptEvents;
import teaselib.core.StateImpl;
import teaselib.core.util.QualifiedString;

/**
 * @author Citizen-Cane
 *
 */
public class StateProxy extends AbstractProxy<State> implements State, State.Attributes {
    final ScriptEvents events;

    public StateProxy(String namespace, State state, ScriptEvents events) {
        super(namespace, state);
        this.events = events;
    }

    @Override
    public Options apply() {
        injectNamespace();
        StateOptionsProxy options = new StateOptionsProxy(namespace, state.apply(), events);
        events.stateApplied.fire(new ScriptEvents.StateChangedEventArgs(state));
        return options;
    }

    @Override
    public Options applyTo(Object... items) {
        injectNamespace();
        StateOptionsProxy options = new StateOptionsProxy(namespace, state.applyTo(items), events);
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
    public boolean is(Object... objects) {
        return state.is(objects);
    }

    @Override
    public boolean applied() {
        return state.applied();
    }

    @Override
    public boolean expired() {
        return state.expired();
    }

    @Override
    public Duration duration() {
        return state.duration();
    }

    @Override
    public void remove() {
        events.stateRemoved.fire(new ScriptEvents.StateChangedEventArgs(state));
        state.remove();
    }

    @Override
    public void removeFrom(Object... peers) {
        events.stateRemoved.fire(new ScriptEvents.StateChangedEventArgs(state));
        state.removeFrom(peers);
    }

    @Override
    public boolean removed() {
        return state.removed();
    }

    @Override
    public long removed(TimeUnit unit) {
        return state.removed(unit);
    }

    @Override
    public void applyAttributes(Object... attributes) {
        ((State.Attributes) state).applyAttributes(attributes);
    }
}