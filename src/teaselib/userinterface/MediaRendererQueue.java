package teaselib.userinterface;

import java.util.Collection;
import java.util.HashMap;

import teaselib.ScriptInterruptedException;
import teaselib.TeaseLib;

public class MediaRendererQueue {

	protected final HashMap<Class<?>, MediaRenderer.Threaded> threadedMediaRenderers = new HashMap<>();

	public MediaRendererQueue()
	{
	}

	public void start(Collection<MediaRenderer> renderers, TeaseLib teaseLib)
	{
		for(MediaRenderer r : renderers)
		{
			start(r, teaseLib);
		}
	}
	
	/**
	 * Start a media renderer, but wait for other renderers of the same kind to complete first
	 * @param mediaMenderer
	 * @param teaseScript
	 */
	public void start(MediaRenderer mediaMenderer, TeaseLib teaseLib)
	{
		if (Thread.currentThread().isInterrupted())
		{
			throw new ScriptInterruptedException();
		}
		if (mediaMenderer instanceof MediaRenderer.Threaded)
		{
			// Before a media renderer can render, all predecessors must complete their work
			Class<?> key = mediaMenderer.getClass();
			synchronized(this)
			{
				if (threadedMediaRenderers.containsKey(key))
				{
					threadedMediaRenderers.get(key).completeAll();
				}
				threadedMediaRenderers.put(mediaMenderer.getClass(), (MediaRenderer.Threaded) mediaMenderer);
			}
		}
		mediaMenderer.render(teaseLib);
	}

	public synchronized void completeAllMandatories()
	{
		for(MediaRenderer.Threaded renderer : threadedMediaRenderers.values())
		{
			renderer.completeMandatory();
		}
	}

	public synchronized void completeAll()
	{
		for(MediaRenderer.Threaded renderer : threadedMediaRenderers.values())
		{
			renderer.completeAll();
		}
	}
	public synchronized void endAll()
	{
		for(MediaRenderer.Threaded renderer : threadedMediaRenderers.values())
		{
			renderer.end();
		}
	}
}
