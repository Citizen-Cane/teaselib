/**
 * 
 */
package teaselib.core;

import static org.junit.Assert.*;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Collection;

import org.junit.Test;

import teaselib.test.TestScript;

/**
 * @author someone
 *
 */
public class ResourceUnpackToFolderTest {
    private static final String RESOURCE_1 = "resource1.txt";

    @Test
    public void testUnpackFolderAbsolute() throws IOException {
        TestScript script = TestScript.getOne();
        script.resources
                .addAssets("/teaselib/core/UnpackResourcesTestData_flat.zip");

        String resourcesFolder = "/" + "UnpackResourcesTestData" + "/";
        testUnpackResourcesToFolder(script, resourcesFolder);
    }

    @Test
    public void testUnpackFolderRelative() throws IOException {
        TestScript script = TestScript.getOne();
        script.resources
                .addAssets("/teaselib/core/UnpackResourcesTestData_flat.zip");

        String resourcesFolder = "UnpackResourcesTestData" + "/";
        testUnpackResourcesToFolder(script, resourcesFolder);
    }

    @Test
    public void testUnpackFolderReferencedViaClass() throws IOException {
        TestScript script = TestScript.getOne(ResourceUnpackToFolderTest.class);
        script.resources.addAssets(
                "/teaselib/core/UnpackResourcesTestData_ResourceRootStructure.zip");

        String resourcesFolder = "UnpackResourcesTestData" + "/";
        testUnpackResourcesToFolder(script, resourcesFolder);
    }

    private void testUnpackResourcesToFolder(TestScript script,
            String resourcesFolder) throws IOException {
        String path = resourcesFolder + RESOURCE_1;

        deleteTestData(script, path);

        Collection<String> itemsBefore = script
                .resources(resourcesFolder + "*");
        // 1 txt, 3 jpg , 3 png
        assertEquals(7, itemsBefore.size());

        // Cache test data
        File res1 = script.resources.unpackEnclosingFolder(path);
        String resource1Content = null;
        BufferedReader reader = new BufferedReader(new FileReader(res1));
        try {
            resource1Content = reader.readLine();
        } finally {
            reader.close();
        }
        assertEquals("1", resource1Content);

        // Test that duplicate resources aren't listed
        Collection<String> itemsAfter = script.resources(resourcesFolder + "*");
        assertEquals(itemsBefore, itemsAfter);
        assertEquals(itemsBefore.size(), getFilesCount(res1.getParentFile()));

        // Repeat with cached content
        res1 = script.resources.unpackEnclosingFolder(path);
        reader = new BufferedReader(new FileReader(res1));
        try {
            resource1Content = reader.readLine();
        } finally {
            reader.close();
        }
        assertEquals("1", resource1Content);
        assertEquals(itemsBefore.size(), getFilesCount(res1.getParentFile()));
    }

    public static int getFilesCount(File folder) {
        File[] files = folder.listFiles();
        int count = 0;
        for (File f : files) {
            if (f.isDirectory()) {
                count += getFilesCount(f);
            } else {
                count++;
            }
        }
        return count;
    }

    private void deleteFolder(File path) {
        File[] files = path.listFiles();
        for (File file : files) {
            if (file.isDirectory()) {
                deleteFolder(file);
            } else {
                file.delete();
            }
        }
        path.delete();
    }

    private void deleteTestData(TestScript script, String path)
            throws IOException {
        File res1 = script.resources.unpackEnclosingFolder(path);
        deleteFolder(res1.getParentFile());
        assertFalse(res1.exists());
    }
}
