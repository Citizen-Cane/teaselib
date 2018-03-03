package teaselib.core;

import java.io.File;
import java.io.FileNotFoundException;
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
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import teaselib.Config;
import teaselib.core.util.QualifiedItem;

public class ResourceLoader {
    private static final Logger logger = LoggerFactory.getLogger(ResourceLoader.class);

    public static final String ResourcesInProjectFolder = "/";

    private final ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
    private final Method addURL;
    private final Set<URI> resourceLocations = new HashSet<>();
    private final File basePath;
    private final String resourceRoot;

    /**
     * @param mainScript
     *            The class of the main script, for loading resources.
     */
    public ResourceLoader(Class<?> mainScript) {
        this(mainScript, getPackagePath(mainScript));
    }

    public ResourceLoader(Class<?> mainScript, String resourceRoot, String[] assets) {
        this(mainScript, resourceRoot);
        addAssets(assets);
    }

    public ResourceLoader(Class<?> mainScript, String resourceRoot, String[] assets, String[] optionalAssets) {
        this(mainScript, resourceRoot);
        addAssets(assets);
        addAssets(optionalAssets);
    }

    /**
     * @param mainScript
     *            The class of the main script, for loading resources.
     * @param resourceRoot
     *            The resource path under which to start looking for resources.
     */
    public ResourceLoader(Class<?> mainScript, String resourceRoot) {
        this(getBasePath(getProjectPath(mainScript)), resourceRoot);
    }

    private static File getBasePath(File mainScript) {
        String string = QualifiedItem.of(Config.Assets).toString();
        String overiddenAssetPath = System.getProperty(string, "");
        if (classLoaderCompatibleResourcePath(overiddenAssetPath).isEmpty()) {
            return mainScript;
        } else {
            return new File(classLoaderCompatibleResourcePath(overiddenAssetPath));
        }
    }

    public ResourceLoader(File basePath, String resourceRoot, String[] assets, String[] optionalAssets) {
        this(basePath, resourceRoot);
        addAssets(assets);
        addAssets(optionalAssets);
    }

    public ResourceLoader(File basePath, String resourceRoot) {
        this.basePath = getBasePath(basePath);
        this.resourceRoot = classLoaderCompatibleResourcePath(pathToFolder(resourceRoot));
        logger.info("Using basepath='" + basePath.getAbsolutePath() + "'");
        try {
            addURL = addURLMethod();
        } catch (NoSuchMethodException e) {
            throw new IllegalArgumentException(e.getMessage(), e);
        }
        // The base path already part of the class path,
        // but not listed in resource locations
        URI uri = basePath.toURI();
        if (isValidResourceLocation(uri)) {
            resourceLocations.add(uri);
        }
    }

    private static String pathToFolder(String path) {
        if (path.endsWith("/"))
            return path;
        else
            return path + "/";
    }

    private static String classLoaderCompatibleResourcePath(String path) {
        if (path.startsWith("/")) {
            return path.substring(1);
        } else {
            return path;
        }
    }

    private static String getPackagePath(Class<?> mainScript) {
        return mainScript.getPackage().getName().replace(".", "/") + "/";
    }

    public static File getProjectPath(Class<?> mainScript) {
        String classFile = getClassFilePath(mainScript);
        URL url = mainScript.getClassLoader().getResource(classLoaderCompatibleResourcePath(classFile));
        String protocol = url.getProtocol().toLowerCase();
        if (classLoaderCompatibleResourcePath(protocol).equals("file")) {
            return projectPathFromFile(url, classLoaderCompatibleResourcePath(classFile));
        } else if (classLoaderCompatibleResourcePath(protocol).equals("jar")) {
            return projectParentPathFromJar(url);
        } else {
            throw new IllegalArgumentException("Unsupported protocol: " + url.toString());
        }
    }

    private static String getClassFilePath(Class<?> mainScript) {
        String classFile = "/" + mainScript.getName().replace(".", "/") + ".class";
        return classLoaderCompatibleResourcePath(classFile);
    }

    private static File projectPathFromFile(URL url, String classFile) {
        String path = getUndecoratedPath(url);
        int classOffset = classLoaderCompatibleResourcePath(classFile).length();
        return new File(classLoaderCompatibleResourcePath(path).substring(0,
                classLoaderCompatibleResourcePath(path).length() - classOffset));
    }

    private static File projectParentPathFromJar(URL url) {
        String path = getUndecoratedPath(url);
        int startOffset = new String("File:/").length();
        int jarOffset = classLoaderCompatibleResourcePath(path).indexOf(".jar!");
        return new File(classLoaderCompatibleResourcePath(path).substring(startOffset, jarOffset)).getParentFile();
    }

    /**
     * {@link java.net.URL} paths have white space is escaped ({@code %20}), so to work with resources, these
     * decorations must be removed.
     * 
     * @param url
     *            The url to retrieve the undecorated path from.
     * @return A string containing the undecorated path part of the URL.
     */
    private static String getUndecoratedPath(URL url) {
        final String path = url.getPath();
        return classLoaderCompatibleResourcePath(path).replace("%20", " ");
    }

    private static Method addURLMethod() throws NoSuchMethodException {
        Method addURI = URLClassLoader.class.getDeclaredMethod("addURL", URL.class);
        addURI.setAccessible(true);
        return addURI;
    }

    public void addAssets(Class<?> scriptClass) {
        addAssets(ResourceLoader.getProjectPath(scriptClass).getAbsolutePath());
    }

    public void addAssets(String... paths) {
        try {
            if (haveAntClassLoader()) {
                addAssetsToAntClassLoader(paths);
            } else {
                addAssets(toURIs(paths));
            }
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalArgumentException("Cannot add assets " + Arrays.toString(paths) + " - " + e.getMessage(),
                    e);
        }
    }

    private void addAssetsToAntClassLoader(String[] paths)
            throws NoSuchMethodException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {
        Method addPathComponent = addPathComponentMethod();
        for (String path : paths) {
            File file = new File(basePath, path);
            logger.info("Using resource location: " + file.getAbsolutePath());
            addPathComponent.invoke(classLoader, new Object[] { file });
        }
    }

    private boolean haveAntClassLoader() {
        return classLoader.getClass().getSimpleName().startsWith("AntClassLoader");
    }

    private Method addPathComponentMethod() throws NoSuchMethodException {
        Class<?> classLoaderClass = classLoader.getClass().getSuperclass();
        Method addPathComponent = classLoaderClass.getDeclaredMethod("addPathComponent", File.class);
        addPathComponent.setAccessible(true);
        return addPathComponent;
    }

    private URI[] toURIs(String[] paths) {
        URI[] uris = new URI[paths.length];
        for (int i = 0; i < paths.length; i++) {
            String entry = paths[i];
            URI uri = toURI(classLoaderCompatibleResourcePath(entry));
            uris[i] = uri;
        }
        return uris;
    }

    private URI toURI(String path) {
        File file = new File(classLoaderCompatibleResourcePath(path));
        if (!file.isAbsolute()) {
            file = new File(basePath, classLoaderCompatibleResourcePath(path));
        }
        URI uri = file.toURI();
        return uri;
    }

    private void addAssets(URI[] assets)
            throws IllegalAccessException, InvocationTargetException, MalformedURLException {
        for (URI uri : assets) {
            addAsset(uri);
        }
    }

    private void addAsset(URI uri) throws IllegalAccessException, InvocationTargetException, MalformedURLException {
        boolean isValid = isValidResourceLocation(uri);
        if (isValid) {
            addURL.invoke(classLoader, new Object[] { uri.toURL() });
            resourceLocations.add(uri);
            logger.info("Using resource location: " + uri.getPath());
        } else {
            // Just warn, since everybody should be able to unpack the archives
            // to explore or change the contents,
            // and to remove them to ensure the unpacked resources are used
            logger.warn("Archive not available: " + uri.getPath());
        }
    }

    private boolean isValidResourceLocation(URI uri) {
        File file = new File(uri);
        boolean isValidAndNotAddYet = file.exists() && !resourceLocations.contains(uri);
        boolean isArchiveOrDirectory = uri.getPath().endsWith(".jar") || uri.getPath().endsWith(".zip")
                || file.isDirectory();
        boolean isValid = isValidAndNotAddYet && isArchiveOrDirectory;
        return isValid;
    }

    public InputStream getResource(String path) throws IOException {
        String classloaderCompatibleResourcePath = getClassLoaderAbsoluteResourcePath(path);
        logger.info("Resource: '" + classloaderCompatibleResourcePath + "'");
        InputStream inputStream = classLoader.getResourceAsStream(classloaderCompatibleResourcePath);
        if (inputStream == null) {
            throw new IOException(path);
        }
        return inputStream;
    }

    public String getClassLoaderAbsoluteResourcePath(String resource) {
        final String classloaderCompatibleResourcePath;
        if (isAbsoluteResourcePath(resource)) {
            classloaderCompatibleResourcePath = classLoaderCompatibleResourcePath(resource);
        } else if (isNearlyAbsoluteResourcePath(resource)) {
            classloaderCompatibleResourcePath = resource;
        } else {
            classloaderCompatibleResourcePath = resourceRoot + resource;
        }
        return classloaderCompatibleResourcePath;
    }

    private boolean isNearlyAbsoluteResourcePath(String resource) {
        return resource.startsWith(resourceRoot);
    }

    private static boolean isAbsoluteResourcePath(String resource) {
        return resource.startsWith("/");
    }

    /**
     * Retrieves all resource entries matching the given pattern.
     * <p>
     * 
     * @param pattern
     *            RegEx pattern for resource selection.
     * @return List of resource paths matching the pattern. All resources in all asset paths are enumerated, then
     *         matched against the pattern.
     */
    public Collection<String> resources(Pattern pattern) {
        Collection<String> resources = new LinkedHashSet<>();
        for (URI classsPathEntry : resourceLocations) {
            Collection<String> matches = new ResourceList(resourceRoot).getResources(classsPathEntry, pattern);
            resources.addAll(matches);
        }
        return resources;
    }

    /**
     * Get the absolute file path of a resource. Good for creating a File or URL object. The path denotes a directory or
     * file in the file system, not in a jar or zip.
     * <p>
     * The directory is writable in order to cache resources that must exist as a file.
     * 
     * @param classLoaderCompatibleResourcePath(resourcePath)
     *            The path to the resource relative to the asset root directory.
     * @return The absolute file system path to the resource item.
     */
    public File getAssetPath(String resourcePath) {
        if (resourcePath.startsWith("/")) {
            return new File(basePath, resourcePath);
        } else {
            return new File(basePath, resourceRoot + classLoaderCompatibleResourcePath(resourcePath));
        }
    }

    /**
     * Unpacks the enclosing folder of the requested resource, including all other resources and all sub folders.
     * 
     * @param classLoaderCompatibleResourcePath(path)
     *            The path to the requested resource
     * @return The requested resource file.
     * @throws IOException
     */
    public File unpackEnclosingFolder(String resourcePath) throws IOException {
        File match = null;
        String parentPath = resourcePath.substring(0, resourcePath.lastIndexOf('/'));
        Collection<String> folder = resources(Pattern.compile(getClassLoaderAbsoluteResourcePath(parentPath + "/.*")));
        for (String file : folder) {
            File unpacked = unpackFileFromFolder(file);
            if (match == null && classLoaderCompatibleResourcePath(file)
                    .equals(classLoaderCompatibleResourcePath(getClassLoaderAbsoluteResourcePath(resourcePath)))) {
                match = unpacked;
            }
        }
        if (match != null) {
            return match;
        } else {
            throw new FileNotFoundException(resourcePath);
        }
    }

    /**
     * Unpacks a resource into the file system. If the file already, the method just returns the absolute file path.
     * 
     * @param classLoaderCompatibleResourcePath(resourcePath)
     *            A resource path.
     * @return Absolute file path to the resource denoted by the {@code path} parameter.
     * @throws IOException
     */
    public File unpackToFile(String resourcePath) throws IOException {
        File file = getAssetPath(resourcePath);
        return unpackToFileInternal(resourcePath, file);
    }

    private File unpackFileFromFolder(String resourcePath) throws IOException {
        String classLoaderCompatibleResourcePath = classLoaderCompatibleResourcePath(resourcePath);
        File file = new File(basePath, classLoaderCompatibleResourcePath);
        return unpackToFileInternal(classLoaderCompatibleResourcePath, file);
    }

    private File unpackToFileInternal(String resourcePath, File file) throws IOException {
        if (!file.exists()) {
            InputStream resource = null;
            try {
                resource = getResource(resourcePath);
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

    public String getRoot() {
        return resourceRoot;
    }
}
