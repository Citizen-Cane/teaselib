package teaselib.core.events;

public abstract class Delegate extends Event<Object, EventArgs> {

    public abstract void run() throws Exception;

    @Override
    public void run(Object sender, EventArgs args) throws Exception {
        run();
    }
}
