package teaselib.util;

import java.util.ArrayDeque;
import java.util.Deque;

import teaselib.TeaseLib;

public class DelegateThread extends Thread {

	private final Deque<Delegate> queue = new ArrayDeque<>();

	public DelegateThread() {
		start();
	}

	@Override
	public void run() {
		while (true) {
			try {
				synchronized (this) {
					wait();
					while (!queue.isEmpty()) {
						Delegate delegate = queue.removeFirst();
						try {
							delegate.run();
						} catch (Throwable t) {
							TeaseLib.log(delegate, t);
							delegate.setError(t);
						}
						synchronized (delegate)
						{
							delegate.notifyAll();
						}
					}
				}
			} catch (InterruptedException e) {
				// Expected
			} catch (Throwable t) {
				TeaseLib.log(this, t);
			}
		}
	}

// TODO Must lock delegate immediately, and release when executed to make this work
// TODO Need to query execution state of delegate -> provide wait(delegate) method
//	public void runAsync(Delegate delegate) {
//		synchronized (delegate) {
//			synchronized (this) {
//				queue.add(delegate);
//				notify();
//			}
//		}
//	}

	/**
	 * Execute the delegate synchronized. The current thread waits until the delegates has completed execution. 
	 * @param delegate The delegate to execute in the delegate thread.
	 * @throws Throwable If the delegate throws, the throwable is forwarded to the current thread.
	 */
	public void run(Delegate delegate) throws Throwable {
		synchronized (delegate) {
			synchronized (this) {
				queue.add(delegate);
				notify();
			}
			try {
				delegate.wait();
			} catch (InterruptedException e) {
				// Ignore
			}
		}
		Throwable t = delegate.getError();
		if (t != null)
		{
			throw t;
		}
	}

	void end() {
		interrupt();
		try {
			join();
		} catch (InterruptedException e) {
			// Ignore
		}
	}
}
