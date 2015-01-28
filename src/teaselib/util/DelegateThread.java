package teaselib.util;

import java.util.ArrayDeque;
import java.util.Deque;

import teaselib.ScriptInterruptedException;
import teaselib.TeaseLib;

public class DelegateThread extends Thread {

    private final Deque<Delegate> queue = new ArrayDeque<Delegate>();
    private boolean endThread = false;

    public DelegateThread() {
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
                TeaseLib.log(this, t);
            }
            if (endThread) {
                break;
            }
        }
    }

    /**
     * Execute the delegate synchronized. The current thread waits until the
     * delegates has completed execution.
     * 
     * @param delegate
     *            The delegate to execute in the delegate thread.
     * @throws Throwable
     *             If the delegate throws, the throwable is forwarded to the
     *             current thread.
     */
    public void run(Delegate delegate) throws Throwable {
        synchronized (delegate) {
            synchronized (queue) {
                queue.addLast(delegate);
                queue.notify();
            }
            try {
                delegate.wait();
            } catch (InterruptedException e) {
                throw new ScriptInterruptedException();
            }
        }
        Throwable t = delegate.getError();
        if (t != null) {
            throw t;
        }
    }

    void end() {
        endThread = true;
        try {
            join();
        } catch (InterruptedException e) {
            // Ignore
        }
    }
}
