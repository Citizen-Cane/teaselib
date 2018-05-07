package teaselib.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Collection;

import org.junit.Test;

import teaselib.core.util.ReflectionUtils;
import teaselib.test.TestScript;

/**
 * @author Citizen-Cane
 *
 */
public class ResourceLoaderTest {
    private static final String RESOURCE_1 = "resource1.txt";

    @Test
    public void testRelativeResourceLoadingFromFile() throws IOException {
        TestScript script = TestScript.getOne(getClass());

        String relativePath = RESOURCE_1;
        loadResource(script, relativePath);
    }

    @Test
    public void testRResourceLoadingFromFile() throws IOException {
        TestScript script = TestScript.getOne();

        String name = absolutePath() + RESOURCE_1;
        loadResource(script, name);
    }

    private static void loadResource(TestScript script, String path) throws IOException {
        try (InputStream resourceStream = script.resources.get(path);
                BufferedReader reader = new BufferedReader(new InputStreamReader(resourceStream));) {
            String resource = reader.readLine();
            assertEquals("1", resource);
        }
    }

    @Test
    public void testAbsoluteResourceLoadingFromZip() throws IOException {
        TestScript script = TestScript.getOne();
        script.resources.addAssets("teaselib/core/UnpackResourcesTestData_ResourceRootStructure.zip");
        loadResource(script, absolutePath() + RESOURCE_1);
    }

    private String absolutePath() {
        return ResourceLoader.absolute(ReflectionUtils.packagePath(getClass()));
    }

    @Test
    public void testRelativeResourceLoadingFromZip() throws IOException {
        TestScript script = TestScript.getOne(getClass());
        script.resources.addAssets("teaselib/core/UnpackResourcesTestData_ResourceRootStructure.zip");
        loadResource(script, RESOURCE_1);
    }

    @Test
    public void testResourceLoadingWithWildcardsRelativePaths() {
        TestScript script = TestScript.getOne(getClass());

        assertEquals(1, script.resources("util/Foo.txt").size());
        assertEquals(2, script.resources("util/Foo?.txt").size());
        assertEquals(3, script.resources("util/Foo*.txt").size());
        Collection<String> items = script.resources("util/Foo*.txt");
        assertEquals(3, items.size());
    }

    @Test
    public void testResourceLoadingWithWildcardsAbsolutePaths() {
        TestScript script = TestScript.getOne(getClass());

        String rootDir = "/" + getClass().getPackage().getName().replace(".", "/");
        assertEquals(1, script.resources(rootDir + "/util/Foo.txt").size());
        assertEquals(2, script.resources(rootDir + "/util/Foo?.txt").size());
        assertEquals(3, script.resources(rootDir + "/util/Foo*.txt").size());
        Collection<String> items = script.resources(rootDir + "/util/Foo*.txt");
        assertEquals(3, items.size());
    }

    @Test
    public void testResourceLoadingCaseRelative() {
        TestScript script = TestScript.getOne(getClass());

        assertEquals(1, script.resources("util/bar.txt").size());
        assertEquals(3, script.resources("util/bar?.txt").size());
        assertEquals(4, script.resources("util/bar*.txt").size());
        assertEquals(0, script.resources("util/Bar.txt").size());
        assertEquals(2, script.resources("util/Bar?.txt").size());
        assertEquals(2, script.resources("util/Bar*.txt").size());
    }

    @Test
    public void testUnpackFileAbsolute() throws IOException {
        TestScript script = TestScript.getOne(getClass());
        script.resources.addAssets("/teaselib/core/UnpackResourcesTestData_ResourceRootStructure.zip");

        String path = "/teaselib/core/UnpackResourcesTestData" + "/" + RESOURCE_1;
        assertTrue(script.resources.has(path));
        File res1 = script.resources.unpackToFile(path);
        res1.delete();
        assertFalse(res1.exists());

        readContent(script, path);
    }

    @Test
    public void testUnpackFileRelative() throws IOException {
        TestScript script = TestScript.getOne(getClass());
        script.resources.addAssets("/teaselib/core/UnpackResourcesTestData_ResourceRootStructure.zip");

        String path = "UnpackResourcesTestData" + "/" + RESOURCE_1;
        assertTrue(script.resources.has(path));
        File res1 = script.resources.unpackToFile(path);
        res1.delete();
        assertFalse(res1.exists());

        readContent(script, path);
    }

    @Test
    public void testUnpackFileRelativeToResourceRoot() throws IOException {
        TestScript script = TestScript.getOne("/teaselib/core/UnpackResourcesTestData");
        script.resources.addAssets("/teaselib/core/UnpackResourcesTestData_ResourceRootStructure.zip");

        String path = RESOURCE_1;
        assertTrue(script.resources.has(path));
        File res1 = script.resources.unpackToFile(path);
        res1.delete();
        assertFalse(res1.exists());

        readContent(script, path);
    }

    // TODO unpack to temporary folder to improve stability
    private void readContent(TestScript script, String path) throws IOException, FileNotFoundException {
        File res1;
        res1 = script.resources.unpackToFile(path);
        try {
            try (BufferedReader reader = new BufferedReader(new FileReader(res1));) {
                String resource1 = reader.readLine();
                assertEquals("1", resource1);
            }
        } finally {
            res1.delete();
        }
    }

    @Test
    public void testResourceFilteredOutBecauseNotInScriptPath() throws IOException {
        TestScript script = TestScript.getOne(getClass());
        script.resources.addAssets("/teaselib/core/UnpackResourcesTestData_flat.zip");

        String path = "UnpackResourcesTestData" + "/" + RESOURCE_1;
        assertFalse(script.resources.has(path));
    }

    @Test
    public void testThatPathsAreCompatibleBetweenDifferentResourceLoaders() {
        TestScript script1 = TestScript.getOne(getClass());
        TestScript script2 = TestScript.getOne();

        assertFalse(script1.resources.equals(script2.resources));
        assertFalse(script1.resources.getAssetPath("").equals(script2.resources.getAssetPath("")));

        Collection<String> itemsFromRelative = script1.resources("util/Foo*.txt");
        Collection<String> itemsFromAbsolute = script2.resources("/teaselib/core/util/Foo*.txt");

        assertEquals(3, itemsFromRelative.size());
        assertEquals(itemsFromRelative, itemsFromAbsolute);
    }

    @Test
    public void testUnpackFolderAbsolute() throws IOException {
        TestScript script = TestScript.getOne();
        script.resources.addAssets("/teaselib/core/UnpackResourcesTestData_flat.zip");

        String resourcesFolder = "/" + "UnpackResourcesTestData" + "/";
        testUnpackResourcesToFolder(script, resourcesFolder);
    }

    @Test
    public void testUnpackFolderRelative() throws IOException {
        TestScript script = TestScript.getOne();
        script.resources.addAssets("/teaselib/core/UnpackResourcesTestData_flat.zip");

        String resourcesFolder = "UnpackResourcesTestData" + "/";
        testUnpackResourcesToFolder(script, resourcesFolder);
    }

    @Test
    public void testUnpackFolderRelativeToResourceRoot() throws IOException {
        TestScript script = TestScript.getOne("/teaselib/core/");
        script.resources.addAssets("/teaselib/core/UnpackResourcesTestData_ResourceRootStructure.zip");

        String resourcesFolder = "UnpackResourcesTestData/";
        testUnpackResourcesToFolder(script, resourcesFolder);
    }

    @Test
    public void testUnpackFolderReferencedViaClass() throws IOException {
        TestScript script = TestScript.getOne(ResourceLoaderTest.class);
        script.resources.addAssets("/teaselib/core/UnpackResourcesTestData_ResourceRootStructure.zip");

        String resourcesFolder = "/teaselib/core/UnpackResourcesTestData" + "/";
        testUnpackResourcesToFolder(script, resourcesFolder);
    }

    private void testUnpackResourcesToFolder(TestScript script, String resourcesFolder) throws IOException {
        String path = resourcesFolder + RESOURCE_1;

        deleteTestData(script, path);

        try {
            Collection<String> itemsBefore = script.resources(resourcesFolder + "*");
            // 1 txt, 3 jpg , 3 png
            assertEquals(7, itemsBefore.size());
            // Cache test data
            File res1 = script.resources.unpackEnclosingFolder(path);
            String resource1Content = null;
            try (BufferedReader reader = new BufferedReader(new FileReader(res1));) {
                resource1Content = reader.readLine();
            }
            assertEquals("1", resource1Content);
            // Test that duplicate resources aren't listed
            Collection<String> itemsAfter = script.resources(resourcesFolder + "*");
            assertEquals(itemsBefore, itemsAfter);
            assertEquals(itemsBefore.size(), getFilesCount(res1.getParentFile()));
            // Repeat with cached content
            res1 = script.resources.unpackEnclosingFolder(path);
            try (BufferedReader reader = new BufferedReader(new FileReader(res1));) {
                resource1Content = reader.readLine();
            }
            assertEquals("1", resource1Content);
            assertEquals(itemsBefore.size(), getFilesCount(res1.getParentFile()));
        } finally {
            deleteTestData(script, path);
        }
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

    private void deleteTestData(TestScript script, String path) throws IOException {
        File res1 = script.resources.unpackEnclosingFolder(path);
        deleteFolder(res1.getParentFile());
        assertFalse(res1.exists());
    }

    @Test
    public void testScriptRelativeResources() throws IOException {
        TestScript script = TestScript.getOne();
        assertEquals(ResourceLoader.ResourcesInProjectFolder, script.resources.getRoot());
        String relativePath = RESOURCE_1;
        try (InputStream resourceStream = script.resources.getResource(relativePath, getClass());) {
            assertNotNull(resourceStream);
        }
    }
}
