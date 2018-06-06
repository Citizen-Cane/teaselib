package teaselib.core.state;

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

}