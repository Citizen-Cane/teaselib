package teaselib.core;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

import teaselib.TeaseLib;

public class ResourceLoader {

    public final String basePath;
    public final String assetRoot;
    private final ClassLoader classLoader = getClass().getClassLoader();
    private final Method addURL;
    private final Set<URI> enumeratableClassPaths = new HashSet<URI>();

    /**
     * @param basePath
     *            The base path under which to find the resources. Either a zip,
     *            a jar, or a folder
     * @param assetRoot
     *            The root folder for all resources - either a directory in the
     *            base folder or a root folderin a zip or jar
     * @throws NoSuchMethodException
     * @throws InvocationTargetException
     * @throws IllegalAccessException
     * @throws MalformedURLException
     */
    public ResourceLoader(String basePath, String assetRoot) {
        this.basePath = basePath.endsWith("/") ? basePath : basePath + "/";
        if (assetRoot.contains("/") || assetRoot.contains("\\"))
            throw new IllegalArgumentException(
                    "No slashes '/' or backlashes '\\' for resource loader root please");
        this.assetRoot = assetRoot + "/";
        try {
            addURL = addURLMethod();
            // Add the base path to allow unpacking resource archives or to
            // deploy directories
            addAsset(new File(basePath).toURI());
        } catch (Exception e) {
            TeaseLib.instance().log.error(this, e);
            throw new IllegalArgumentException(basePath + assetRoot);
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

    public void addAssets(String... paths) {
        try {
            addAssets(toURIs(basePath, paths));
        } catch (Exception e) {
            throw new IllegalArgumentException("Cannot add assets:" + e);
        }
    }

    private static URI[] toURIs(String root, String[] paths) {
        URI[] uris = new URI[paths.length];
        for (int i = 0; i < paths.length; i++) {
            File path = new File(paths[i]);
            final File file;
            if (path.isAbsolute()) {
                file = path;
            } else {
                file = new File(root, paths[i]);
            }
            uris[i] = file.toURI();
        }
        return uris;
    }

    private void addAssets(URI[] assets) throws IllegalAccessException,
            InvocationTargetException, MalformedURLException {
        for (URI uri : assets) {
            addAsset(uri);
        }
    }

    private void addAsset(URI uri) throws IllegalAccessException,
            InvocationTargetException, MalformedURLException {
        File file = new File(uri);
        if (file.exists()) {
            addURL.invoke(classLoader, new Object[] { uri.toURL() });
            if (uri.getPath().endsWith(".zip") || file.isDirectory()) {
                enumeratableClassPaths.add(uri);
                TeaseLib.instance().log.info("Using resource location: "
                        + uri.getPath());
            }
        } else {
            // Just warn, since everybody should be able to unpack the archives
            // and explore, and remove them to ensure folder resources are used
            TeaseLib.instance().log.info("Archive not available: "
                    + uri.getPath());
        }
    }

    public InputStream getResource(String path) throws IOException {
        String resource = assetRoot + path;
        TeaseLib.instance().log.info("Resource: '" + resource + "'");
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

    /**
     * Return all resource in a path that match the extension
     * 
     * @param path
     *            The resource path to search for resources with the given
     *            extension. The path may denote be a directory or package, or a
     *            directory or package plus a partial base name.
     * 
     * @param extension
     *            The extension to look for, without the dot (for instance 'mp3'
     *            ,'jpg', 'txt')
     * @return A list of resource paths containing all matching resources.
     *         Please note that all resources matching the path are returned.
     *         This includes resources in sub-directories of the specified path
     *         as well.
     */
    public List<String> resources(String path, String extension) {
        // todo document regex patterns, they're just to hard to remember
        String filesWithExtension = path + ".+\\." + extension;
        String pathsThatEndWithExtension = "(.*)(" + filesWithExtension + ")$";
        return resources(pathsThatEndWithExtension);
    }

    /**
     * Retrieves all resource entries matching the path pattern.
     * 
     * @param pathPattern
     *            RegEx pattern for resource selection.
     * @return List of resource paths matching the pattern. All resources in all
     *         asset paths are enumerated, then matched against the pattern.
     */
    public List<String> resources(String pathPattern) {
        Pattern pattern = Pattern.compile(pathPattern);
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
                // folder (the asset root folder) as well
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

    public URL url(String path) {
        final String absolutePath = assetRoot + path;
        final URL resource = classLoader.getResource(absolutePath);
        if (resource == null) {
            TeaseLib.instance().log.info("Resource '" + absolutePath
                    + "' not found");
        }
        return resource;
    }

    public URI uri(String path) {
        URI uri = null;
        URL url = url(path);
        if (url != null) {
            try {
                uri = url.toURI();
            } catch (URISyntaxException e) {
                TeaseLib.instance().log.error(this, e);
            }
        }
        return uri;
    }

    /**
     * Get the absolute path of a resource. Good for creating a File or URL
     * object. The path denotes a directory or file in the file system, not in a
     * jar or zip.
     * 
     * @param resourcePath
     *            The path to the resource relative to the asset root directory.
     * @return The absolute path to the resource item.
     * @throws IOException
     */
    public File getAssetPath(String resourcePath) {
        return new File(basePath + assetRoot, resourcePath);
    }
}
