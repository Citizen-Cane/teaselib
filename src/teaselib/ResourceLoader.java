package teaselib;

import java.awt.Image;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
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

	private final String basePath;
	private final String assetRoot;

	private final ClassLoader classLoader = getClass().getClassLoader();
	private final Set<URI> enumeratableClassPaths = new HashSet<>();

	public ResourceLoader(String basePath, URI[] assets, String assetRoot)
			throws NoSuchMethodException, InvocationTargetException,
			IllegalAccessException, MalformedURLException,
			FileNotFoundException {
		Class<URLClassLoader> classLoaderClass = URLClassLoader.class;
		@SuppressWarnings("rawtypes")
		final Class[] parameters = new Class[] { URL.class };
		Method addURI = classLoaderClass
				.getDeclaredMethod("addURL", parameters);
		addURI.setAccessible(true);
		addAsset(addURI, new File(basePath + assetRoot).toURI());
		for (URI uri : assets) {
			addAsset(addURI, uri);
		}
		this.basePath = basePath;
		this.assetRoot = assetRoot;
	}

	private void addAsset(Method method, URI uri)
			throws IllegalAccessException, InvocationTargetException,
			MalformedURLException, FileNotFoundException {
		if (new File(uri).exists()) {
			method.invoke(classLoader, new Object[] { uri.toURL() });
			if (uri.getPath().endsWith(".zip")) {
				enumeratableClassPaths.add(uri);
			}
		} else {
			// Just warn, since everybody should be able to unpack the archives and explore
			// throw new FileNotFoundException(uri.toString());
			TeaseLib.log("Archive not available: " + uri.toString());
		}
	}

	public InputStream getResource(String path) throws IOException {
		InputStream inputStream = classLoader.getResourceAsStream(assetRoot
				+ path);
		if (inputStream == null) {
			throw new IOException(path);
		}
		return inputStream;
	}

	public InputStream getResource(URL url) throws IOException {
		String path = url.toString();
		InputStream inputStream = classLoader.getResourceAsStream(path);
		if (inputStream == null) {
			throw new IOException("No input stream: " + path);
		}
		return inputStream;
	}

	public BufferedReader script(String name) throws IOException {
		String path = name + ".sbd";
		InputStream inputStream = getResource(path);
		return new BufferedReader(new InputStreamReader(inputStream));
	}

	public Image image(String path) throws IOException {
		InputStream inputStream = getResource(path);
		return ImageIO.read(inputStream);
	}

	public Image image(URL path) throws IOException {
		InputStream inputStream = getResource(path);
		return ImageIO.read(inputStream);
	}

	/**
	 * Retrieves all zip entries matching the path pattern
	 * 
	 * @param pathPattern
	 *            RegEx pattern for URL selection
	 * @return List of URLs matching the pattern
	 * @throws IOException
	 */
	public List<String> resources(String pathPattern) {
		Pattern pattern = Pattern.compile(assetRoot + pathPattern);
		List<String> resources = new LinkedList<>();
		int start = assetRoot.length();
		for (URI uri : enumeratableClassPaths) {
			Collection<String> matches = ResourceList
					.getResources(uri, pattern);
			for (String match : matches) {
				resources.add(match.substring(start));
			}
		}
		return resources;
	}

	public URL path(String path) {
		return classLoader.getResource(assetRoot + path);
	}

	/**
	 * Copy the whole directory from the resources zip into a directory and
	 * return the path to it. Useful if a resource has to ber opened on the
	 * desktop and more than a single file is needed
	 * 
	 * @param path
	 *            Path to resources directory
	 * @return Path to actual directory
	 * @throws IOException
	 */

	/**
	 * Get the absolute path of a resource. Good for creating a File or URL
	 * object. The path denotes a directory or file in the file system, not in a
	 * jar or zip.
	 * 
	 * @param resourcePath
	 *            relative path to resource
	 * @return The absolute path of the resource item
	 * @throws IOException
	 */
	public File getAssetsPath(String resourcePath) throws IOException {
		return new File(basePath + assetRoot, resourcePath);
	}
}
