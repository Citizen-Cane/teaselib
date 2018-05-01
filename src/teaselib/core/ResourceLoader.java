package teaselib.core;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.List;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import teaselib.Config;
import teaselib.core.util.QualifiedItem;
import teaselib.core.util.ReflectionUtils;
import teaselib.core.util.resource.ResourceCache;

public class ResourceLoader {
    private static final Logger logger = LoggerFactory.getLogger(ResourceLoader.class);

    public static final String ResourcesInProjectFolder = "/";

    private final File basePath;
    private final String resourceRoot;

    // TODO Must be global in order to share actor resources between scripts
    private final ResourceCache resourceCache = new ResourceCache();

    /**
     * @param mainScript
     *            The class of the main script, for loading resources.
     */
    public ResourceLoader(Class<?> mainScript) {
        this(mainScript, ReflectionUtils.getPackagePath(mainScript));
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
        // TODO resource root should be absolute path same as ResourcesInProjectFolder
        // -> should fit in nicely with locations and resolve a couple of "/" in the code
        this.resourceRoot = classLoaderCompatibleResourcePath(pathToFolder(resourceRoot));
        logger.info("Using basepath='{}'", basePath.getAbsolutePath());

        addAssets(basePath.getPath());
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
        int startOffset = "File:/".length();
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

    public void addAssets(Class<?> scriptClass) {
        addAssets(ResourceLoader.getProjectPath(scriptClass).getAbsolutePath());
    }

    // TODO Split handling to support mandatory and optional assets
    public void addAssets(String... paths) {
        for (String path : paths) {
            addAssets(path);
        }
    }

    private String addAssets(String path) {
        if (!new File(path).isAbsolute()) {
            path = basePath + absoluteResourcePath(path);
        }
        try {
            resourceCache.add(ResourceCache.location(path, resourceRoot));
        } catch (IOException e) {
            throw new IllegalArgumentException("Cannot add assets " + path + " - " + e.getMessage(), e);
        }
        return path;
    }

    public boolean hasResource(String path) {
        return resourceCache.has(absoluteResourcePath(getClassLoaderAbsoluteResourcePath(path)));
    }

    public InputStream getResource(String path) throws IOException {
        return getResource(path, null);
    }

    public InputStream getResource(String path, Class<?> clazz) throws IOException {
        final String absoluteResourcePath;
        if (isAbsoluteResourcePath(path)) {
            return resource(path);
        } else if (isNearlyAbsoluteResourcePath(path) && clazz == null) {
            return resource("/" + path);
        } else if (clazz != null) {
            absoluteResourcePath = ReflectionUtils.asAbsolutePath(clazz.getPackage()) + path;
            if (resourceCache.has(absoluteResourcePath)) {
                InputStream inputStream;
                try {
                    inputStream = resource(absoluteResourcePath);
                } catch (IOException e) {
                    inputStream = null;
                }
                if (inputStream != null) {
                    return inputStream;
                } else {
                    return resource(absoluteResourcePath(absoluteResourcePath));
                }
            } else {
                return resource(path);
            }
        } else {
            return resource(absoluteResourcePath(resourceRoot + path));
        }
    }

    private InputStream resource(String path) throws IOException {
        return inputStreamOrThrow(path, resourceCache.get(path));
    }

    private InputStream inputStreamOrThrow(String path, InputStream inputStream) throws IOException {
        if (inputStream == null)
            throw new IOException(path);
        return inputStream;
    }

    public static String absoluteResourcePath(String path) {
        return path.startsWith("/") ? path : "/" + path;
    }

    // TODO It's not class loader absolute anymore, since there's a leading / now
    // TODO Code duplicated and extended to getResource(...) > refactor and delete this
    @Deprecated
    public String getClassLoaderAbsoluteResourcePath(String resource) {
        final String classloaderCompatibleResourcePath;
        if (isAbsoluteResourcePath(resource)) {
            classloaderCompatibleResourcePath = classLoaderCompatibleResourcePath(resource);
        } else if (isNearlyAbsoluteResourcePath(resource)) {
            classloaderCompatibleResourcePath = resource;
        } else {
            classloaderCompatibleResourcePath = resourceRoot + resource;
        }
        return "/" + classloaderCompatibleResourcePath;
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
    public List<String> resources(Pattern pattern) {
        // TODO enum local matches to absolute paths
        return resourceCache.get(pattern);
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
            try (InputStream resource = getResource(resourcePath);) {
                if (!file.exists()) {
                    file.getParentFile().mkdirs();
                    Files.copy(resource, Paths.get(file.toURI()));
                }
            }
        }
        return file;
    }

    public String getRoot() {
        return resourceRoot;
    }
}
