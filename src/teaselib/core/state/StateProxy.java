/**
 * 
 */
package teaselib.core.state;

import java.util.Set;

import teaselib.Duration;
import teaselib.State;
import teaselib.core.StateMaps;

public class StateProxy implements State, StateMaps.Attributes {
    final String namespace;
    public final State state;

    public StateProxy(String namespace, State state) {
        this.namespace = namespace;
        this.state = state;
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

    @Override
    public String toString() {
        return state.toString();
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((namespace == null) ? 0 : namespace.hashCode());
        result = prime * result + ((state == null) ? 0 : state.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        StateProxy other = (StateProxy) obj;
        if (namespace == null) {
            if (other.namespace != null)
                return false;
        } else if (!namespace.equals(other.namespace))
            return false;
        if (state == null) {
            if (other.state != null)
                return false;
        } else if (!state.equals(other.state))
            return false;
        return true;
    }

}