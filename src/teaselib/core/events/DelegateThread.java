package teaselib.core.events;

import java.util.ArrayDeque;
import java.util.Deque;

import org.slf4j.LoggerFactory;

// TODO Forward all exceptions to caller
// TODO Resolve throwing Throwable, instead just throw exceptions
public class DelegateThread extends Thread {
    private static final org.slf4j.Logger logger = LoggerFactory.getLogger(DelegateThread.class);

    private final Deque<Delegate> queue = new ArrayDeque<>();
    private boolean endThread = false;

    public DelegateThread(String name) {
        setName(name);
        start();
    }

    @Override
    public void run() {
        // TOOD Don't lock the whole queue! (but doing so prevents dead locks)
        while (true) {
            try {
                Delegate delegate;
                synchronized (queue) {
                    while (queue.isEmpty()) {
                        try {
                            queue.wait();
                        } catch (InterruptedException ignored) {
                        }
                        if (endThread) {
                            break;
                        }
                    }
                    delegate = queue.removeFirst();
                }
                synchronized (delegate) {
                    try {
                        delegate.run();
                    } catch (Throwable t) {
                        delegate.setError(t);
                    }
                    delegate.notifyAll();
                }
            } catch (Throwable t) {
                logger.error(t.getMessage(), t);
            }
            if (endThread) {
                break;
            }
        }
    }

    /**
     * Execute the delegate synchronized. The current thread waits until the delegates has completed execution.
     * 
     * @param delegate
     *            The delegate to execute in the delegate thread.
     * @throws Throwable
     *             If the delegate throws, the throwable is forwarded to the current thread.
     */
    public void run(Delegate delegate) throws InterruptedException, Throwable {
        synchronized (delegate) {
            synchronized (queue) {
                queue.addLast(delegate);
                queue.notify();
            }
            delegate.wait();
        }
        Throwable t = delegate.getError();
        if (t != null) {
            throw t;
        }
    }
}
