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
 * @author someone
 *
 */
public class FileUtilityTests {
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
        assertTrue(FileUtilites.sameContent(test, test));
        final File foo = new File(currentDir, "Foo.txt");
        assertFalse(FileUtilites.sameContent(test, foo));
        assertTrue(FileUtilites.sameContent(foo, foo));
    }

    @Test
    public void testFileFilter() throws IOException {
        final File directory = temporaryFolder.newFolder();
        assertEquals(false, FileUtilites.getFileFilter("jpg").accept(directory));
    }

}
