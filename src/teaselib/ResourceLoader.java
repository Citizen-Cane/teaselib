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
    private final Set<URI> enumeratableClassPaths = new HashSet<URI>();

    public ResourceLoader(String basePath, String assetRoot)
            throws NoSuchMethodException, InvocationTargetException,
            IllegalAccessException, MalformedURLException,
            FileNotFoundException {
        this.basePath = basePath.endsWith("/") ? basePath : basePath + "/";
        this.assetRoot = assetRoot.endsWith("/") ? assetRoot : assetRoot + "/";
        Method addURI = addURLMethod();
        addAsset(addURI, getAssetPath("").toURI());
    }

    public void addAssets(String[] paths) {
        try {
            addAssets(toURIs(basePath, paths));
        } catch (Throwable t) {
            throw new IllegalArgumentException("Cannot add assets:" + t);
        }
    }

    private static URI[] toURIs(String root, String[] paths) throws IOException {
        URI[] uris = new URI[paths.length];
        for (int i = 0; i < paths.length; i++) {
            uris[i] = new File(root + paths[i]).toURI();
        }
        return uris;
    }

    private void addAssets(URI[] assets) throws IllegalAccessException,
            InvocationTargetException, MalformedURLException,
            FileNotFoundException, NoSuchMethodException {
        addAssets(assets, addURLMethod());
    }

    private void addAssets(URI[] assets, Method addURI)
            throws IllegalAccessException, InvocationTargetException,
            MalformedURLException, FileNotFoundException {
        for (URI uri : assets) {
            addAsset(addURI, uri);
        }
    }

    private Method addURLMethod() throws NoSuchMethodException {
        Class<URLClassLoader> classLoaderClass = URLClassLoader.class;
        @SuppressWarnings("rawtypes")
        final Class[] parameters = new Class[] { URL.class };
        Method addURI = classLoaderClass
                .getDeclaredMethod("addURL", parameters);
        addURI.setAccessible(true);
        return addURI;
    }

    private void addAsset(Method method, URI uri)
            throws IllegalAccessException, InvocationTargetException,
            MalformedURLException, FileNotFoundException {
        if (new File(uri).exists()) {
            method.invoke(classLoader, new Object[] { uri.toURL() });
            if (uri.getPath().endsWith(".zip")) {
                enumeratableClassPaths.add(uri);
                TeaseLib.log("Using: " + uri.toString());
            }
        } else {
            // Just warn, since everybody should be able to unpack the archives
            // and explore, and remove them to ensure folder resources are used
            TeaseLib.log("Archive not available: " + uri.toString());
        }
    }

    public InputStream getResource(String path) throws IOException {
        String resource = assetRoot + path;
        InputStream inputStream = classLoader.getResourceAsStream(resource);
        if (inputStream == null) {
            throw new IOException(resource);
        }
        return inputStream;
    }

    public InputStream getResource(URL url) throws IOException {
        String resource = url.toString();
        InputStream inputStream = classLoader.getResourceAsStream(resource);
        if (inputStream == null) {
            throw new IOException("No input stream: " + resource);
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
        List<String> resources = new LinkedList<String>();
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
    public File getAssetPath(String resourcePath) {
        return new File(basePath + assetRoot, resourcePath);
    }
}
