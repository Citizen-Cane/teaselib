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

import teaselib.Config;
import teaselib.TeaseLib;

public class ResourceLoader {

    private final ClassLoader classLoader = getClass().getClassLoader();
    private final Method addURL;
    private final Set<URI> enumeratableClassPaths = new HashSet<URI>();

    private final File basePath;

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
    public ResourceLoader(Class<?> mainScript) {
        String systemProperty = System.getProperty(
                Config.Namespace + "." + Config.Assets.toString(), "");
        if (systemProperty.isEmpty()) {
            basePath = getClassPath(mainScript);
        } else {
            basePath = new File(systemProperty);
        }
        try {
            addURL = addURLMethod();
        } catch (NoSuchMethodException e) {
            throw new IllegalArgumentException(e.getMessage(), e);
        }
        addAssets(basePath.getAbsolutePath());
    }

    public static File getClassPath(Class<?> mainScript) {
        String classFile = "/" + mainScript.getName().replace(".", "/")
                + ".class";
        URL url = mainScript.getResource(classFile);
        String protocol = url.getProtocol().toLowerCase();
        if (protocol.equals("file")) {
            String path = undecoratedPath(url);
            int classOffset = classFile.length();
            return new File(path.substring(0, path.length() - classOffset));
        } else if (protocol.equals("jar")) {
            String path = undecoratedPath(url);
            int startOffset = new String("File:/").length();
            int jarOffset = path.indexOf(".jar!");
            return new File(path.substring(startOffset, jarOffset))
                    .getParentFile();
        } else {
            throw new IllegalArgumentException(
                    "Unsupported protocol: " + url.toString());
        }
    }

    private static String undecoratedPath(URL url) {
        return url.getPath().replace("%20", " ");
    }

    private static Method addURLMethod() throws NoSuchMethodException {
        Class<URLClassLoader> classLoaderClass = URLClassLoader.class;
        @SuppressWarnings("rawtypes")
        final Class[] parameters = new Class[] { URL.class };
        Method addURI = classLoaderClass.getDeclaredMethod("addURL",
                parameters);
        addURI.setAccessible(true);
        return addURI;
    }

    public void addAssets(String... paths) {
        try {
            addAssets(toURIs(paths));
        } catch (Exception e) {
            throw new IllegalArgumentException("Cannot add assets:" + e);
        }
    }

    private URI[] toURIs(String[] paths) {
        URI[] uris = new URI[paths.length];
        for (int i = 0; i < paths.length; i++) {
            File path = new File(paths[i]);
            final File file;
            if (path.isAbsolute()) {
                file = path;
            } else {
                // throw new IllegalArgumentException(path.getPath());
                file = new File(basePath, paths[i]);
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
                TeaseLib.instance().log
                        .info("Using resource location: " + uri.getPath());
            }
        } else {
            // Just warn, since everybody should be able to unpack the archives
            // to explore or change the contents,
            // and to remove them to ensure the unpacked resources are used
            TeaseLib.instance().log
                    .info("Archive not available: " + uri.getPath());
        }
    }

    public InputStream getResource(String resource) throws IOException {
        TeaseLib.instance().log.info("Resource: '" + resource + "'");
        InputStream inputStream = classLoader
                .getResourceAsStream(classLoaderAbsolutePath(resource));
        if (inputStream == null) {
            throw new IOException(resource);
        }
        return inputStream;
    }

    /**
     * Return all resources in a path that match the extension
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
        for (URI uri : enumeratableClassPaths) {
            Collection<String> matches = ResourceList.getResources(uri,
                    pattern);
            for (String match : matches) {
                // assets are stored in the folder specified when instanciating
                // the resource loader
                // As a result, we don't have to mention it again in the script
                // When unpacking a zip, all files are stored into a single
                // folder (the asset root folder) as well
                // Now when enumerating zip entries, we can just search for the
                // full path
                // resources.add("/" + match);
                resources.add(match);
            }
        }
        return resources;
    }

    public URL url(String resource) {
        final URL url = classLoader
                .getResource(classLoaderAbsolutePath(resource));
        if (url == null) {
            TeaseLib.instance().log
                    .info("Resource '" + resource + "' not found");
        }
        return url;
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

    private static String classLoaderAbsolutePath(String path) {
        if (path.startsWith("/")) {
            return path.substring(1);
        } else {
            return path;
        }
    }

    /**
     * Get the absolute path of a resource. Good for creating a File or URL
     * object. The path denotes a directory or file in the file system, not in a
     * jar or zip.
     * 
     * @param resourcePath
     *            The path to the resource relative to the asset root directory.
     * @return The absolute file system path to the resource item.
     * @throws IOException
     */
    public String getAssetPath(String resourcePath) {
        return basePath + "/" + resourcePath;
    }
}
