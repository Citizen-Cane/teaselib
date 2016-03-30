package teaselib.core;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;

// Pasted from
// http://stackoverflow.com/questions/3923129/get-a-list-of-resources-from-classpath-directory
// forums.devx.com/showthread.php?t=153784
// and changed to suit TeaseLib

/**
 * list resources available from the class path @ *
 */
public class ResourceList {

    /**
     * 
     */
    private static final String PathSeparator = "/";

    /**
     * for all elements of java.class.path get a Collection of resources Pattern
     * pattern = Pattern.compile(".*"); gets all resources
     * 
     * @param pattern
     *            the pattern to match
     * @return the resources in the order they are found
     */
    public static List<String> getResources(final URI basePath,
            final Pattern pattern) {
        final ArrayList<String> retval = new ArrayList<String>();
        final File file = new File(basePath.getPath());
        if (file.isDirectory()) {
            retval.addAll(getResourcesFromDirectory(basePath, file, pattern));
        } else {
            retval.addAll(getResourcesFromJarFile(file, pattern));
        }
        return retval;
    }

    private static List<String> getResourcesFromJarFile(File file,
            Pattern pattern) {
        ArrayList<String> retval = new ArrayList<String>();
        ZipFile zf;
        try {
            zf = new ZipFile(file);
        } catch (final ZipException e) {
            throw new Error(e);
        } catch (final IOException e) {
            throw new Error(e);
        }
        Enumeration<? extends ZipEntry> e = zf.entries();
        while (e.hasMoreElements()) {
            ZipEntry ze = e.nextElement();
            addMatchingEntry(pattern, retval, ze.getName());
        }
        try {
            zf.close();
        } catch (final IOException e1) {
            throw new Error(e1);
        }
        return retval;
    }

    private static List<String> getResourcesFromDirectory(URI basePath,
            File directory, Pattern pattern) {
        ArrayList<String> retval = new ArrayList<String>();
        File[] fileList = directory.listFiles();
        for (final File file : fileList) {
            if (file.isDirectory()) {
                retval.addAll(
                        getResourcesFromDirectory(basePath, file, pattern));
            } else {
                URI absolute = file.toURI();
                URI relative = basePath.relativize(absolute);
                String resourcePath = relative.toString();
                addMatchingEntry(pattern, retval, resourcePath);
            }
        }
        return retval;
    }

    private static void addMatchingEntry(Pattern pattern,
            ArrayList<String> retval, String resourcePath) {
        String absoluteResourcePath = resourcePath.startsWith(PathSeparator)
                ? resourcePath : PathSeparator + resourcePath;
        boolean accept = pattern.matcher(absoluteResourcePath).matches();
        if (accept) {
            retval.add(absoluteResourcePath);
        }
    }
}
