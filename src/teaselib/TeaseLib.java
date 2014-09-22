package teaselib;

import java.io.IOException;
import java.net.URI;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * @author someone
 *
 * Teaselib can either be used via the shared global instance, or by creating a private instance.
 * 
 * A private TeaseLib instance might be useful when using more than one TTS voice in a scenario with multiple actors
 * 
 * All static methods refer to the shared instance
 */
public class TeaseLib {

	private static TeaseLib instance;
	
	public final Host host;
	public final ResourceLoader resources;
	public final Persistence persistence;
	
	/**
	 * @param host
	 * @param persistence
	 * @param resourceLoader
	 */
	public TeaseLib(Host host, Persistence persistence, ResourceLoader resourceLoader)
	{
		if (host == null || resourceLoader == null)
		{
			throw new IllegalArgumentException();
		}
		this.host = host;
		this.resources = resourceLoader;
		this.persistence = persistence;
		TeaseLib.instance = this;
	}

	public TeaseLib(Host host, Persistence persistence, URI[] assets, String assetsBasePath) throws IOException
	{
		this(host, persistence, new ResourceLoader(assets, assetsBasePath));
	}

	public static TeaseLib instance()
	{
		if (instance == null)
		{
			throw new IllegalStateException("Please create a TeaseLib instance first");
		}
		return instance;
	}

	public static Host host()
	{
		return instance().host;
	}

	public static ResourceLoader resources()
	{
		return instance().resources;
	}

	public static Persistence persistence()
	{
		return instance().persistence;
	}
	
	public static synchronized void log(String text)
	{
		Date now = new Date(System.currentTimeMillis());
		SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm:ss");
		String line = timeFormat.format(now) + ": " + text;
		host().log(line);
	}

	/**
	 * Log details interesting for teaselib devs
	 * @param line Log output
	 */
	public static void logDetail(String line)
	{
		log(line);
	}

	public static void logDetail(Object instance, Throwable e)
	{
		log(instance, e);
	}

	public static void log(Object instance, Throwable e)
	{
		String message = e.getMessage();
		log(e.getClass().getSimpleName() + ": " + (message != null ? message : ""));
		log(instance.getClass().getName() +":" + instance.toString());
		logStackTrace(e);
		Throwable cause = e.getCause();
		if (cause != null)
		{
			log("Caused by");
			logStackTrace(cause);
		}
	}

	private static void logStackTrace(Throwable e) {
		for(StackTraceElement ste : e.getStackTrace())
		{
			log("\t" + ste.toString());
		}
	}
}
