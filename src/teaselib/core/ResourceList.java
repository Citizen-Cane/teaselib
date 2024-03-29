package teaselib.core;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import teaselib.core.util.ExceptionUtil;

// Pasted from
// http://stackoverflow.com/questions/3923129/get-a-list-of-resources-from-classpath-directory
// forums.devx.com/showthread.php?t=153784
// and improved for teaselib

/**
 * list resources available from the class path @ *
 */
@Deprecated
public class ResourceList {
    public static final String PathDelimiter = "/";

    private final String resourceRoot;

    public ResourceList(String resourceRoot) {
        this.resourceRoot = resourceRoot;
    }

    /**
     * for all elements of java.class.path get a Collection of resources Pattern pattern = Pattern.compile(".*"); gets
     * all resources
     * 
     * @param pattern
     *            the pattern to match
     * @return the resources in the order they are found
     */
    public List<String> getResources(URI basePath, Pattern pattern) {
        ArrayList<String> retval = new ArrayList<>();
        File file = new File(basePath.getPath());
        if (file.isDirectory()) {
            retval.addAll(getResourcesFromDirectory(basePath, file, pattern));
        } else {
            retval.addAll(getResourcesFromJarFile(file, pattern));
        }
        return retval;
    }

    private List<String> getResourcesFromJarFile(File file, Pattern pattern) {
        ArrayList<String> retval = new ArrayList<>();
        try (ZipFile zf = new ZipFile(file);) {
            Enumeration<? extends ZipEntry> e = zf.entries();
            while (e.hasMoreElements()) {
                ZipEntry ze = e.nextElement();
                if (!ze.isDirectory()) {
                    addMatchingEntry(pattern, retval, ze.getName());
                }
            }
        } catch (final IOException e1) {
            throw ExceptionUtil.asRuntimeException(e1);
        }
        return retval;
    }

    private List<String> getResourcesFromDirectory(URI basePath, File directory, Pattern pattern) {
        ArrayList<String> retval = new ArrayList<>();
        File[] fileList = directory.listFiles();
        for (final File file : fileList) {
            if (file.isDirectory()) {
                retval.addAll(getResourcesFromDirectory(basePath, file, pattern));
            } else {
                URI absolute = file.toURI();
                URI relative = basePath.relativize(absolute);
                String resourcePath = relative.getPath();
                addMatchingEntry(pattern, retval, resourcePath);
            }
        }
        return retval;
    }

    private void addMatchingEntry(Pattern pattern, List<String> retval, String resourcePath) {
        if (resourcePath.startsWith(resourceRoot) && pattern.matcher(resourcePath).matches()) {
            retval.add(resourcePath);
        }
    }
}
