package teaselib.core;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import teaselib.Config;
import teaselib.core.util.QualifiedString;
import teaselib.core.util.ReflectionUtils;
import teaselib.core.util.WildcardPattern;
import teaselib.core.util.resource.ResourceCache;

// TODO replace with Path-based implementation

public class ResourceLoader {
    private static final Logger logger = LoggerFactory.getLogger(ResourceLoader.class);

    public static final String ResourcesInProjectFolder = "/";

    public static final String separator = "/";

    private final File basePath;
    private final String resourceRoot;

    // TODO Must be global in order to share actor resources between scripts
    private final ResourceCache resourceCache = new ResourceCache();

    /**
     * @param mainScript
     *            The class of the main script, for loading resources.
     */
    public ResourceLoader(Class<?> mainScript) {
        this(mainScript, ReflectionUtils.packagePath(mainScript));
    }

    public ResourceLoader(Class<?> mainScript, String resourceRoot, String[] assets) {
        this(mainScript, resourceRoot, assets, new String[] {});
    }

    public ResourceLoader(Class<?> mainScript, String resourceRoot, String[] assets, String[] optionalAssets) {
        this(getBasePath(getProjectPath(mainScript)), resourceRoot, assets, optionalAssets);
    }

    /**
     * @param mainScript
     *            The class of the main script, for loading resources.
     * @param resourceRoot
     *            The resource path under which to start looking for resources.
     */
    public ResourceLoader(Class<?> mainScript, String resourceRoot) {
        this(mainScript, resourceRoot, new String[] {}, new String[] {});
    }

    private static File getBasePath(File mainScript) {
        String string = QualifiedString.of(Config.Assets).toString();
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
        this.resourceRoot = absolute(pathToFolder(resourceRoot));
        logger.info("Using basepath='{}'", basePath.getAbsolutePath());
        addProjectFolder();
    }

    private void addProjectFolder() {
        addAssets("");
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

    public static File getProjectPath(Class<?> clazz) {
        String classFile = getClassFilePath(clazz);
        URL url = clazz.getClassLoader().getResource(classLoaderCompatibleResourcePath(classFile));
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

    // TODO Split handling to support mandatory and optional assets
    public void addAssets(String... paths) {
        for (String path : paths) {
            addAssets(path);
        }
    }

    private String addAssets(String path) {
        if (!new File(path).isAbsolute()) {
            return addAssetsImpl(basePath + absolute(path));
        } else {
            return addAssetsImpl(path);
        }
    }

    /**
     * @param path
     * @return
     */
    private String addAssetsImpl(String path) {
        try {
            resourceCache.add(ResourceCache.location(path, resourceRoot));
        } catch (IOException e) {
            throw new IllegalArgumentException("Cannot add assets " + path + " - " + e.getMessage(), e);
        }
        return path;
    }

    public boolean has(String path) {
        return resourceCache.has(absolutePathOrNull(path, null));
    }

    public InputStream get(String path) throws IOException {
        return getResource(path, null);
    }

    public InputStream getResource(String path, Class<?> clazz) throws IOException {
        String absoluteResourcePath = absolutePathOrNull(path, clazz);
        if (absoluteResourcePath != null) {
            return inputStreamOrThrow(path, resource(absoluteResourcePath));
        } else {
            // TODO probably not used anymore - review
            absoluteResourcePath = absolute(ReflectionUtils.packagePath(clazz) + path);
            InputStream inputStream = resourceCache.get(absoluteResourcePath);
            if (inputStream != null) {
                return inputStream;
            } else {
                return inputStreamOrThrow(path, resource(projectRelative(path)));
            }
        }
    }

    private String absolutePathOrNull(String path, Class<?> clazz) {
        if (isAbsolute(path)) {
            return path;
        } else if (isNearlyAbsolute(path) && clazz == null) {
            return absolute(path);
        } else if (clazz == null || resourceRoot.equals(absolute(ReflectionUtils.packagePath(clazz)))) {
            return projectRelative(path);
        } else {
            return null;
        }
    }

    private InputStream resource(String path) throws IOException {
        return inputStreamOrThrow(path, resourceCache.get(path));
    }

    private static InputStream inputStreamOrThrow(String path, InputStream inputStream) throws IOException {
        if (inputStream == null)
            throw new IOException(path);
        return inputStream;
    }

    public static String absolute(String path) {
        return path.startsWith("/") ? path : "/" + path;
    }

    private boolean isNearlyAbsolute(String resource) {
        return resource.startsWith(resourceRoot.substring(1));
    }

    public static boolean isAbsolute(String resource) {
        return resource.startsWith("/");
    }

    public class Paths {
        final List<String> elements = new ArrayList<>();
        final HashMap<String, String> mapping = new HashMap<>();

        private void addAll(String wildcardPattern) {
            elements.addAll(resources(WildcardPattern.compile(wildcardPattern)));
            // absolute
            elements.forEach(element -> mapping.put(element, element));
            // relative to wildcard path
            String basePath = basePath(wildcardPattern);
            int length = basePath.length();
            elements.forEach(element -> mapping.put(element.substring(length), element));
        }

        public static String basePath(String wildcardPattern) {
            int index = wildcardPattern.indexOf('*');
            if (index < 0) {
                return wildcardPattern;
            } else {
                return wildcardPattern.substring(0, index);
            }
        }
    }

    public Paths resources(String wildcardPattern, Class<?> clazz) {
        var info = new Paths();
        String absolutePattern = absolutePathOrNull(wildcardPattern, clazz);
        if (absolutePattern != null) {
            info.addAll(absolutePattern);
        } else {
            info.addAll(classRelative(wildcardPattern, clazz));
            info.addAll(projectRelative(wildcardPattern));
        }
        return info;
    }

    private String classRelative(String wildcardPattern, Class<?> clazz) {
        return ReflectionUtils.absolutePath(clazz.getPackage()) + wildcardPattern;
    }

    private String projectRelative(String wildcardPattern) {
        return resourceRoot + wildcardPattern;
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
            return new File(basePath, projectRelative(resourcePath));
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
        List<String> folder = resources(absolutePathOrNull(parentPath + "/*", null), null).elements;
        for (String file : folder) {
            File unpacked = unpackFileFromFolder(file);
            if (match == null && file.equals(absolutePathOrNull(resourcePath, null))) {
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
            try (InputStream resource = get(resourcePath);) {
                if (!file.exists()) {
                    file.getParentFile().mkdirs();
                    Files.copy(resource, java.nio.file.Paths.get(file.toURI()));
                }
            }
        }
        return file;
    }

    public String getRoot() {
        return resourceRoot;
    }
}
