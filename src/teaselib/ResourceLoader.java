package teaselib;

import java.awt.Image;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Method;
import java.net.URI;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

import javax.imageio.ImageIO;

public class ResourceLoader {

	String assetsBasePath; 
	
	private final ClassLoader classLoader = getClass().getClassLoader();
	private final Set<URI> enumeratableClassPaths = new HashSet<>();
	
	public ResourceLoader(URI[] assets, String assetsBasePath) throws IOException
	{
		for(URI uri : assets)
		{
			addClassPath(uri);
		}
		this.assetsBasePath = assetsBasePath;
	}
	
	private void addClassPath(URI uri) throws IOException
	{
		Class<URLClassLoader> classLoaderClass = URLClassLoader.class;
		@SuppressWarnings("rawtypes")
		final Class[] parameters = new Class[]{URL.class};
		try {
		     Method method = classLoaderClass.getDeclaredMethod("addURL", parameters);
		     method.setAccessible(true);
		     method.invoke(classLoader, new Object[]{uri.toURL()});
		  } catch (Throwable t) {
		     throw new IOException("Could not add URL to system classloader", t);
		  }
		if (uri.getPath().endsWith(".zip"))
		{
			enumeratableClassPaths.add(uri);
		}
	}

	public InputStream getResource(String path) throws IOException
	{
		InputStream inputStream = classLoader.getResourceAsStream(assetsBasePath + path);
		if (inputStream == null)
		{
			throw new IOException(path);
		}
		return inputStream;
	}

	public InputStream getResource(URL url) throws IOException
	{
		String path = url.toString();
		InputStream inputStream = classLoader.getResourceAsStream(path);
		if (inputStream == null)
		{
			throw new IOException("No input stream: " + path);
		}
		return inputStream;
	}

	public BufferedReader script(String name) throws IOException
	{
		String path = name + ".sbd";
		InputStream inputStream = getResource(path);
		return new BufferedReader(new InputStreamReader(inputStream));
	}

	public Image image(String path) throws IOException
	{
		InputStream inputStream = getResource(path); 
		return ImageIO.read(inputStream);
	}

	public Image image(URL path) throws IOException
	{
		InputStream inputStream = getResource(path); 
		return ImageIO.read(inputStream);
	}

	/**
	 * Retrieves all zip entries matching the path pattern 
	 * @param pathPattern RegEx pattern for URL selection
	 * @return List of URLs matching the pattern 
	 * @throws IOException
	 */
	public List<String> resources(String pathPattern)
	{
		Pattern pattern = Pattern.compile(assetsBasePath + pathPattern);
		List<String> resources = new LinkedList<>();
		int start = assetsBasePath.length(); 
		for(URI uri : enumeratableClassPaths)
		{
			Collection<String> matches = ResourceList.getResources(uri, pattern);
			for(String match : matches)
			{
				resources.add(match.substring(start));
			}
		}
		return resources;
	}

	public URL path(String path)
	{
		return classLoader.getResource(assetsBasePath + path);
	}

	/**
	 * Copy the whole directory from the resources zip into a directory and return the path to it.
	 * Useful if a resource has to ber opened on the desktop and more than a single file is needed 
	 * @param path Path to resources directory
	 * @return Path to actual directory
	 * @throws IOException
	 */
	public File getDirectory(String path) throws IOException
	{
		throw new UnsupportedOperationException();
	}

}
