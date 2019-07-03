/**
 * 
 */
package teaselib.core.util;

import static org.junit.Assert.*;

import java.io.File;
import java.io.IOException;

import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

/**
 * @author Citizen-Cane
 *
 */
public class FileUtilitiesTests {
    File currentDir = new File(getClass().getResource(
            getClass().getSimpleName() + ".class").getPath()).getParentFile();

    /**
     * @throws java.lang.Exception
     */
    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
    }

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Test
    public void testSameContentSmallFiles() throws IOException {
        File test = new File(currentDir, getClass().getSimpleName() + ".class");
        assertTrue(FileUtilities.sameContent(test, test));
        final File foo = new File(currentDir, "Foo.txt");
        assertFalse(FileUtilities.sameContent(test, foo));
        assertTrue(FileUtilities.sameContent(foo, foo));
    }

    @Test
    public void testFileFilter() throws IOException {
        final File directory = temporaryFolder.newFolder();
        assertEquals(false, FileUtilities.getFileFilter("jpg").accept(directory));
    }

}
