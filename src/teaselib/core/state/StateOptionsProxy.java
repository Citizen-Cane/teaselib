/**
 * 
 */
package teaselib.core.state;

import java.util.concurrent.TimeUnit;

import teaselib.Duration;
import teaselib.State;
import teaselib.State.Persistence;

public class StateOptionsProxy implements State.Options {
    final String namespace;
    final State.Options options;

    public StateOptionsProxy(String namespace, State.Options options) {
        this.namespace = namespace;
        this.options = options;
    }

    @Override
    public State remember() {
        return new StateProxy(namespace, options.remember());
    }

    @Override
    public Persistence over(long duration, TimeUnit unit) {
        return new StatePersistenceProxy(namespace, options.over(duration, unit));
    }

    @Override
    public Persistence over(Duration duration) {
        return new StatePersistenceProxy(namespace, options.over(duration));
    }

    @Override
    public String toString() {
        return options.toString();
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((namespace == null) ? 0 : namespace.hashCode());
        result = prime * result + ((options == null) ? 0 : options.hashCode());
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
        StateOptionsProxy other = (StateOptionsProxy) obj;
        if (namespace == null) {
            if (other.namespace != null)
                return false;
        } else if (!namespace.equals(other.namespace))
            return false;
        if (options == null) {
            if (other.options != null)
                return false;
        } else if (!options.equals(other.options))
            return false;
        return true;
    }

}