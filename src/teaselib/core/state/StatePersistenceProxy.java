/**
 * 
 */
package teaselib.core.state;

import teaselib.State;
import teaselib.State.Persistence;

public class StatePersistenceProxy implements State.Persistence {
    final String namespace;
    final State.Persistence persistence;

    public StatePersistenceProxy(String namespace, Persistence persistence) {
        this.namespace = namespace;
        this.persistence = persistence;
    }

    @Override
    public State remember() {
        return new StateProxy(namespace, persistence.remember());
    }

    @Override
    public String toString() {
        return persistence.toString();
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((namespace == null) ? 0 : namespace.hashCode());
        result = prime * result + ((persistence == null) ? 0 : persistence.hashCode());
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
        StatePersistenceProxy other = (StatePersistenceProxy) obj;
        if (namespace == null) {
            if (other.namespace != null)
                return false;
        } else if (!namespace.equals(other.namespace))
            return false;
        if (persistence == null) {
            if (other.persistence != null)
                return false;
        } else if (!persistence.equals(other.persistence))
            return false;
        return true;
    }
}