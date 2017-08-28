/**
 * 
 */
package teaselib.core.state;

import java.util.Set;

import teaselib.Duration;
import teaselib.State;
import teaselib.core.StateMaps;

/**
 * @author Citizen-Cane
 *
 */
public class StateProxy extends AbstractProxy<State> implements State, StateMaps.Attributes {
    public StateProxy(String namespace, State state) {
        super(namespace, state);
    }

    @Override
    public Options apply() {
        injectNamespace();
        return new StateOptionsProxy(namespace, state.apply());
    }

    @Override
    public <S> Options applyTo(S... items) {
        injectNamespace();
        return new StateOptionsProxy(namespace, state.applyTo(items));
    }

    private void injectNamespace() {
        ((StateMaps.Attributes) state).applyAttributes(namespace);

    }

    public Set<Object> peers() {
        return ((StateMaps.StateImpl) state).peers();
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
    public Persistence remove() {
        return new StatePersistenceProxy(namespace, state.remove());
    }

    @Override
    public <S extends Object> Persistence removeFrom(S... peer) {
        return new StatePersistenceProxy(namespace, state.removeFrom(peer));
    }

    @Override
    public void applyAttributes(Object... attributes) {
        ((StateMaps.Attributes) state).applyAttributes(attributes);
    }
}