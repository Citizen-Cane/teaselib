/**
 * 
 */
package teaselib.core.util;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Citizen-Cane
 *
 */
public class FileUtilitiesTests {
    File currentDir = new File(getClass().getResource(getClass().getSimpleName() + ".class").getPath()).getParentFile();

    @TempDir
    Path temporaryFolder;

    @Test
    public void testSameContentSmallFiles() throws IOException {
        File test = new File(currentDir, getClass().getSimpleName() + ".class");
        assertTrue(FileUtilities.sameContent(test, test));
        File foo = new File(currentDir, "Foo.txt");
        assertFalse(FileUtilities.sameContent(test, foo));
        assertTrue(FileUtilities.sameContent(foo, foo));
    }

    @Test
    public void testFileFilter() throws IOException {
        File directory = Files.createDirectory(temporaryFolder.resolve("test")).toFile();
        assertFalse(FileUtilities.getFileFilter("jpg").accept(directory));
    }

}
