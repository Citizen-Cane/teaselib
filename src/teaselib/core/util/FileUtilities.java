package teaselib.core.util;

import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

/**
 * @author Citizen-Cane
 *
 */
public class FileUtilities {
    private FileUtilities() {
    }

    public static boolean sameContent(File file1, File file2) throws IOException {
        try (FileInputStream is1 = new FileInputStream(file1); FileInputStream is2 = new FileInputStream(file2);) {
            return Stream.sameContent(is1, is2);
        }
    }

    /**
     * Accepts all files that match one of the {@code extensions}. Directories are not accepted.
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
        return pathname -> {
            if (!pathname.isDirectory()) {
                String name = pathname.getName().toLowerCase();
                for (String ext : exts) {
                    if (name.endsWith(ext)) {
                        return true;
                    }
                }
            }
            return false;
        };
    }

    public static void copy(File source, File destination) throws IOException {
        Files.copy(source.toPath(), destination.toPath(), StandardCopyOption.REPLACE_EXISTING);
    }

    public static void copy(String resource, File destination) throws IOException {
        try (InputStream source = FileUtilities.class.getResourceAsStream(resource)) {
            if (source == null) {
                throw new IOException(resource);
            }
            copy(source, destination);
        }
    }

    public static void copy(InputStream resource, File destination) throws IOException {
        Files.copy(resource, destination.toPath(), StandardCopyOption.REPLACE_EXISTING);
    }

    public static File currentDir() {
        return new File("").getAbsoluteFile();
    }
}
