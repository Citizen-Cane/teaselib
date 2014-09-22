package teaselib.userinterface;

import java.util.HashMap;

import teaselib.TeaseLib;

public class MediaRendererQueue {

	protected final HashMap<Class<?>, MediaRenderer.Threaded> threadedMediaRenderers = new HashMap<>();

	public MediaRendererQueue()
	{
	}

	/**
	 * Start a media renderer, but wait for other renderers of the same kind to complete first
	 * @param mediaMenderer
	 * @param teaseScript
	 */
	public void start(MediaRenderer mediaMenderer, TeaseLib teaseLib)
	{
		if (mediaMenderer instanceof MediaRenderer.Threaded)
		{
			Class<?> key = mediaMenderer.getClass();
			if (threadedMediaRenderers.containsKey(key))
			{
				threadedMediaRenderers.get(key).completeAll();
			}
			threadedMediaRenderers.put(mediaMenderer.getClass(), (MediaRenderer.Threaded) mediaMenderer);
		}
		mediaMenderer.render(teaseLib);
	}

	public void completeAllMandatories()
	{
		for(MediaRenderer.Threaded renderer : threadedMediaRenderers.values())
		{
			renderer.completeMandatory();
		}
	}

	public void completeAll()
	{
		for(MediaRenderer.Threaded renderer : threadedMediaRenderers.values())
		{
			renderer.completeAll();
		}
	}
	public void endAll()
	{
		for(MediaRenderer.Threaded renderer : threadedMediaRenderers.values())
		{
			renderer.end();
		}
	}
}
