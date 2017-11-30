package teaselib.core.events;

@FunctionalInterface
public interface Event<S, E extends EventArgs> {
    public abstract void run(S sender, E eventArgs) throws Exception;
}
