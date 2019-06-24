package teaselib.core.events;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EventSource<T extends EventArgs> {
    private static final Logger logger = LoggerFactory.getLogger(EventSource.class);

    private final String name;
    private final List<Event<T>> delegates = new ArrayList<>();
    final Event<T> initial;
    final Event<T> completing;

    public EventSource(String name) {
        this.name = name;
        this.initial = null;
        this.completing = null;
    }

    public EventSource(String name, Event<T> initial, Event<T> completing) {
        this.name = name;
        this.initial = initial;
        this.completing = completing;
    }

    public synchronized void add(Event<T> delegate) {
        delegates.add(delegate);
    }

    public synchronized void remove(Event<T> delegate) {
        boolean removed = delegates.remove(delegate);
        if (!removed) {
            throw new IllegalArgumentException("Removing event " + delegate.toString() + " failed.");
        }
    }

    public synchronized boolean contains(Event<T> delegate) {
        return delegates.contains(delegate);
    }

    /**
     * Returns the number of user events
     * 
     * @return
     */
    public synchronized int size() {
        return delegates.size();
    }

    public synchronized void run(T eventArgs) {
        logger.info("{} , {} listeners {}", name, delegates.size(), eventArgs);
        if (initial != null) {
            runDelegate(eventArgs, initial);
        }
        for (Event<T> delegate : new ArrayList<>(delegates)) {
            runDelegate(eventArgs, delegate);
            if (eventArgs.consumed) {
                logger.info("Event {} consumed", eventArgs);
                break;
            }
        }
        if (completing != null) {
            runDelegate(eventArgs, completing);
        }
    }

    private void runDelegate(T eventArgs, Event<T> delegate) {
        try {
            delegate.run(eventArgs);
        } catch (Throwable t) {
            logger.error(t.getMessage(), t);
        }
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + " " + name + "(" + hashCode() + ")";
    }
}
