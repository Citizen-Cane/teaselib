package teaselib;

import java.awt.Image;
import java.io.BufferedReader;
import java.io.File;
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

    final String basePath;
    final String assetRoot;
    private final ClassLoader classLoader = getClass().getClassLoader();
    private final Set<URI> enumeratableClassPaths = new HashSet<URI>();

    /**
     * @param basePath
     *            The base path under which to find the resource zips, jars and
     *            directories
     * @param assetRoot
     *            The root folder for all resources - either a directory in the
     *            base folder or the single root entry in a zip or jar
     * @throws NoSuchMethodException
     * @throws InvocationTargetException
     * @throws IllegalAccessException
     * @throws MalformedURLException
     */
    public ResourceLoader(String basePath, String assetRoot)
            throws NoSuchMethodException, InvocationTargetException,
            IllegalAccessException, MalformedURLException {
        this.basePath = basePath.endsWith("/") ? basePath : basePath + "/";
        if (assetRoot.contains("/") || assetRoot.contains("\\"))
            throw new IllegalArgumentException(
                    "No slashes '/' or backlashes '\\' for resource loader root please");
        this.assetRoot = assetRoot + "/";
        Method addURI = addURLMethod();
        // Add the base path to allow unpacking resource archives or to or to
        // deploy directories
        addAsset(addURI, new File(basePath).toURI());
    }

    public void addAssets(String[] paths) {
        try {
            addAssets(toURIs(basePath, paths));
        } catch (Throwable t) {
            throw new IllegalArgumentException("Cannot add assets:" + t);
        }
    }

    private static URI[] toURIs(String root, String[] paths) {
        URI[] uris = new URI[paths.length];
        for (int i = 0; i < paths.length; i++) {
            uris[i] = new File(root + paths[i]).toURI();
        }
        return uris;
    }

    private void addAssets(URI[] assets) throws IllegalAccessException,
            InvocationTargetException, MalformedURLException,
            NoSuchMethodException {
        addAssets(assets, addURLMethod());
    }

    private void addAssets(URI[] assets, Method addURI)
            throws IllegalAccessException, InvocationTargetException,
            MalformedURLException {
        for (URI uri : assets) {
            addAsset(addURI, uri);
        }
    }

    private static Method addURLMethod() throws NoSuchMethodException {
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
            MalformedURLException {
        File file = new File(uri);
        if (file.exists()) {
            method.invoke(classLoader, new Object[] { uri.toURL() });
            if (uri.getPath().endsWith(".zip") || file.isDirectory()) {
                enumeratableClassPaths.add(uri);
                TeaseLib.log("Using resource location: " + uri.toString());
            }
        } else {
            // Just warn, since everybody should be able to unpack the archives
            // and explore, and remove them to ensure folder resources are used
            TeaseLib.log("Archive not available: " + uri.toString());
        }
    }

    public InputStream getResource(String path) throws IOException {
        String resource = assetRoot + path;
        TeaseLib.log("Resource: '" + resource + "'");
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
        InputStream inputStream = null;
        try {
            inputStream = getResource(path);
            return ImageIO.read(inputStream);
        } finally {
            if (inputStream != null) {
                inputStream.close();
            }
        }
    }

    public Image image(URL path) throws IOException {
        InputStream inputStream = null;
        try {
            inputStream = getResource(path);
            return ImageIO.read(inputStream);
        } finally {
            if (inputStream != null) {
                inputStream.close();
            }
        }
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
        Pattern pattern = Pattern.compile("(.*)(" + pathPattern + ")$");
        List<String> resources = new LinkedList<String>();
        int start = assetRoot.length();
        for (URI uri : enumeratableClassPaths) {
            Collection<String> matches = ResourceList
                    .getResources(uri, pattern);
            for (String match : matches) {
                // assets are stored in the folder specified when instanciating
                // the resource loader
                // As a result, we don't have to mention it again in the script
                // When unpacking a zip, all files are stored into a single
                // folder (thew asset root folder) as well
                // Now when enumerating zip entries, we can just search for the
                // full path
                if (match.startsWith(assetRoot)) {
                    resources.add(match.substring(start));
                } else {
                    resources.add(match);
                }

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
