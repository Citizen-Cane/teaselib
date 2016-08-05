package teaselib.core.events;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EventSource<S, T extends EventArgs> {
    private static final Logger logger = LoggerFactory
            .getLogger(EventSource.class);

    private final String name;
    private final List<Event<S, T>> delegates = new ArrayList<Event<S, T>>();
    final Event<S, T> initial;
    final Event<S, T> completing;

    public EventSource(String name) {
        this.name = name;
        this.initial = null;
        this.completing = null;
    }

    public EventSource(String name, Event<S, T> initial,
            Event<S, T> completing) {
        this.name = name;
        this.initial = initial;
        this.completing = completing;
    }

    public synchronized void add(Event<S, T> delegate) {
        delegates.add(delegate);
    }

    public synchronized void remove(Event<S, T> delegate) {
        boolean removed = delegates.remove(delegate);
        if (!removed) {
            throw new IllegalArgumentException(
                    "Removing event " + delegate.toString() + "failed.");
        }
    }

    public synchronized boolean contains(Event<S, T> delegate) {
        return delegates.contains(delegate);
    }

    public synchronized void run(S sender, T eventArgs) {
        logger.info(getClass().getSimpleName() + " " + name + ", "
                + delegates.size() + " listeners " + eventArgs.toString());
        if (initial != null) {
            runDelegate(sender, eventArgs, initial);
        }
        for (Event<S, T> delegate : delegates) {
            runDelegate(sender, eventArgs, delegate);
            if (eventArgs.consumed) {
                logger.info("Event " + eventArgs.toString() + " consumed");
                break;
            }
        }
        if (completing != null) {
            runDelegate(sender, eventArgs, completing);
        }
    }

    private void runDelegate(S sender, T eventArgs, Event<S, T> delegate) {
        try {
            delegate.run(sender, eventArgs);
        } catch (Throwable t) {
            logger.error(t.getMessage(), t);
        }
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + " " + name + "(" + hashCode() + ")";
    }
}
