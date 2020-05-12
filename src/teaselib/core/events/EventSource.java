package teaselib.core.events;

import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import teaselib.core.util.ExceptionUtil;

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

    public synchronized Event<T> add(Event<T> delegate) {
        delegates.add(delegate);
        return delegate;
    }

    public synchronized void remove(Event<T> delegate) {
        boolean removed = delegates.remove(delegate);
        if (!removed) {
            throw new NoSuchElementException("Event " + delegate + " in event source '" + name + "'.");
        }
    }

    public synchronized boolean contains(Event<T> delegate) {
        return delegates.contains(delegate);
    }

    public synchronized int size() {
        return delegates.size();
    }

    public synchronized void fire(T eventArgs) {
        List<Throwable> throwables = new ArrayList<>(delegates.size());

        logger.info("{} , {} listeners {}", name, delegates.size(), eventArgs);
        if (initial != null) {
            runAndCatchThrowable(eventArgs, initial, throwables);
        }

        for (Event<T> delegate : new ArrayList<>(delegates)) {
            runAndCatchThrowable(eventArgs, delegate, throwables);
            if (eventArgs.consumed) {
                logger.debug("Event {} consumed", eventArgs);
                break;
            }
        }

        if (completing != null) {
            runAndCatchThrowable(eventArgs, completing, throwables);
        }

        if (!throwables.isEmpty()) {
            Throwable first = throwables.remove(0);
            throwables.stream().forEach(first::addSuppressed);
            throw ExceptionUtil.asRuntimeException(first);
        }
    }

    private void runAndCatchThrowable(T eventArgs, Event<T> delegate, List<Throwable> throwables) {
        try {
            delegate.run(eventArgs);
        } catch (Throwable t) {
            logger.error(t.getMessage(), t);
            throwables.add(t);
        }
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + " " + name + "(" + hashCode() + ")";
    }

}
