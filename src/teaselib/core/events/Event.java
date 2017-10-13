package teaselib.core.events;

public abstract class Event<S, E extends EventArgs> {
    public abstract void run(S sender, E eventArgs) throws Exception;
}
