package teaselib.core;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashSet;
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
            String path = getUndecoratedPath(url);
            int classOffset = classFile.length();
            return new File(path.substring(0, path.length() - classOffset));
        } else if (protocol.equals("jar")) {
            String path = getUndecoratedPath(url);
            int startOffset = new String("File:/").length();
            int jarOffset = path.indexOf(".jar!");
            return new File(path.substring(startOffset, jarOffset))
                    .getParentFile();
        } else {
            throw new IllegalArgumentException(
                    "Unsupported protocol: " + url.toString());
        }
    }

    /**
     * {@link java.net.URL} paths have white space is escaped ({@code %20}), so
     * to work with resources, these decorations must be removed.
     * 
     * @param url
     *            The url to retrieve the undecorated path from.
     * @return A string containing the undecorated path part of the URL.
     */
    private static String getUndecoratedPath(URL url) {
        final String path = url.getPath();
        return path.replace("%20", " ");
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
     * Retrieves all resource entries matching the given pattern.
     * <p>
     * 
     * @param pattern
     *            RegEx pattern for resource selection.
     * @return List of resource paths matching the pattern. All resources in all
     *         asset paths are enumerated, then matched against the pattern.
     */
    public Collection<String> resources(Pattern pattern) {
        Collection<String> resources = new LinkedHashSet<String>();
        for (URI classsPathEntry : enumeratableClassPaths) {
            Collection<String> matches = ResourceList
                    .getResources(classsPathEntry, pattern);
            resources.addAll(matches);
        }
        return resources;
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
    public File getAssetPath(String resourcePath) {
        return new File(basePath, resourcePath);
    }

    /**
     * Unpacks the enclosing folder of the requested resource, including all
     * other resources and all sub folders.
     * 
     * @param path
     *            The path to the requested resource
     * @return The requested resource file.
     * @throws IOException
     */
    public File unpackEnclosingFolder(String path) throws IOException {
        File match = null;
        String parentPath = path.substring(0, path.lastIndexOf("/"));
        Collection<String> folder = resources(
                Pattern.compile(parentPath + "/.*"));
        for (String file : folder) {
            File unpacked = unpackToFile(file);
            if (match == null && file.equals(path)) {
                match = unpacked;
            }
        }
        return match;
    }

    /**
     * Unpacks a resource into the file system. If the resource is accessable as
     * a file already, nothing is done, and the method just returns the absolute
     * file path.
     * 
     * @param path
     *            A resource path.
     * @return Absolute file path to the resource denoted by the {@code path}
     *         parameter.
     * @throws IOException
     */
    public File unpackToFile(String path) throws IOException {
        File file = getAssetPath(path);
        if (!file.exists()) {
            InputStream resource = null;
            try {
                resource = getResource(path);
                if (!file.exists()) {
                    file.getParentFile().mkdirs();
                    Files.copy(resource, Paths.get(file.toURI()));
                }
            } finally {
                if (resource != null) {
                    resource.close();
                }
            }
        }
        return file;
    }
}
