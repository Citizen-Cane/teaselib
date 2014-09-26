package teaselib.userinterface;

import teaselib.ScriptInterruptedException;
import teaselib.TeaseLib;

public abstract class MediaRendererThread implements Runnable, MediaRenderer,
		MediaRenderer.Threaded {

	protected Thread renderThread = null;
	protected boolean endThread = false;
	protected boolean finishedMandatoryParts = false;
	protected TeaseLib teaseLib = null;

	protected final Object completedMandatoryParts = new Object();
	protected final Object completedAll = new Object();
	
	private long start = 0;

	/**
	 * The render method executed by the render thread
	 */
	protected abstract void render() throws InterruptedException;

	@Override
	public final void run()
	{
		try {
			render();
		} catch (InterruptedException e) {
			TeaseLib.logDetail(this, e);
		}
		catch(Throwable t)
		{
			TeaseLib.log(this, t);
		}
		endThread = true;
		synchronized(completedMandatoryParts)
		{
			completedMandatoryParts.notifyAll();
		}
		synchronized(completedAll)
		{
			completedAll.notifyAll();
		}
	}

	@Override
	public void render(TeaseLib teaseLib) {
		this.teaseLib = teaseLib;
		endThread = false;
		finishedMandatoryParts = false;
		renderThread = new Thread(this);
		start = System.currentTimeMillis();
		renderThread.start();
	}

	public void completeMandatory() {
		if (!endThread)
		{
			synchronized (completedMandatoryParts) {
				try {
					completedMandatoryParts.wait();
				} catch (InterruptedException e) {
					// Expected
				} catch (IllegalMonitorStateException e) {
					TeaseLib.logDetail(this, e);
				}
			}
		}		
		TeaseLib.logDetail(getClass().getSimpleName()
				+ " completed mandatory part after "
				+ String.format("%.2f seconds", getElapsedSeconds()));
	}

	public void completeAll() {
		if (renderThread == null) return;
		try
		{
			while (renderThread.isAlive()) {
				try {
					renderThread.join();
					TeaseLib.logDetail(getClass().getSimpleName()
							+ " completed all after "
							+ String.format("%.2f", getElapsedSeconds()));
				} catch (InterruptedException e) {
					end();
					throw new ScriptInterruptedException();
				}
			}
		}
		finally
		{
			renderThread = null;
		}
	}

	private double getElapsedSeconds() {
		return (System.currentTimeMillis() - start) / 1000;
	}

	@Override
	public void end() {
		if (renderThread == null) return;
		endThread = true;
		renderThread.interrupt();
		TeaseLib.logDetail(getClass().getSimpleName() + " interrupted after "
				+ String.format("%.2f", getElapsedSeconds()));
		// Almost like complete, but avoid recursion
		try
		{
			while (renderThread.isAlive()) {
				try {
					renderThread.join();
				} catch (InterruptedException e) {
					throw new ScriptInterruptedException();
				}
			}
		}
		finally
		{
			renderThread = null;
			TeaseLib.logDetail(getClass().getSimpleName()
					+ " ended after "
					+ String.format("%.2f", getElapsedSeconds()));
		}
	}
}