package teaselib.core.events;

@FunctionalInterface
public interface Event<E extends EventArgs> {
    public abstract void run(E eventArgs) throws Exception;
}
