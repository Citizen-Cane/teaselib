package teaselib.core.state;

import teaselib.State;
import teaselib.core.StateImpl;
import teaselib.util.Item;
import teaselib.util.ItemImpl;

public class AbstractProxy<T> {

    protected final String namespace;
    public final T state;

    public AbstractProxy(String namespace, T state) {
        this.namespace = namespace;
        this.state = state;
    }

    @Override
    public String toString() {
        return state.toString();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof AbstractProxy<?>) {
            return state.equals(((AbstractProxy<?>) obj).state);
        } else {
            return state.equals(obj);
        }
    }

    @Override
    public int hashCode() {
        return state.hashCode();
    }

    public static Object[] removeProxies(Object... peers) {
        var proxiesRemoved = new Object[peers.length];
        for (int i = 0; i < peers.length; i++) {
            proxiesRemoved[i] = removeProxy(peers[i]);
        }
        return proxiesRemoved;
    }

    public static Object removeProxy(Object item) {
        if (item instanceof Item) {
            return itemImpl((Item) item);
        } else if (item instanceof State) {
            return stateImpl((State) item);
        } else {
            return item;
        }
    }

    public static ItemImpl itemImpl(Item item) {
        if (item instanceof ItemProxy)
            return itemImpl(((ItemProxy) item).item);
        else if (item instanceof ItemImpl)
            return (ItemImpl) item;
        else
            throw new IllegalArgumentException(
                    "Cannot cast item '" + item.displayName() + "' to " + ItemImpl.class.getSimpleName() + ".");
    }

    public static StateImpl stateImpl(State state) {
        if (state instanceof StateProxy)
            return stateImpl(((StateProxy) state).state);
        else if (state instanceof StateImpl)
            return (StateImpl) state;
        else
            throw new IllegalArgumentException(
                    "Cannot cast state '" + state + "' to " + StateImpl.class.getSimpleName() + ".");
    }
}