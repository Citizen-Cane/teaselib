/**
 * 
 */
package teaselib.core.util;

import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.IOException;

/**
 * @author someone
 *
 */
public class FileUtilites {
    public static boolean sameContent(File file1, File file2)
            throws IOException {
        final FileInputStream is1 = new FileInputStream(file1);
        final FileInputStream is2 = new FileInputStream(file2);
        boolean sameContent = Stream.sameContent(is1, is2);
        is1.close();
        is2.close();
        return sameContent;
    }

    /**
     * Accepts all files that match one of the {@code extensions}. Directories
     * are not accepted.
     * 
     * @param extensions
     * @return
     */
    public static FileFilter getFileFilter(String... extensions) {
        if (extensions.length < 1) {
            throw new IllegalArgumentException();
        }
        final String[] exts = new String[extensions.length];
        for (int i = 0; i < extensions.length; i++) {
            exts[i] = extensions[i].toLowerCase();
        }
        return new FileFilter() {
            @Override
            public boolean accept(File pathname) {
                if (!pathname.isDirectory()) {
                    String name = pathname.getName().toLowerCase();
                    for (String ext : exts) {
                        if (name.endsWith(ext)) {
                            return true;
                        }
                    }
                }
                return false;
            }
        };
    }
}