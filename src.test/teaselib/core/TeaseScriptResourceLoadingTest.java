package teaselib.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
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
public class TeaseScriptResourceLoadingTest {
    private static final String RESOURCE_1 = "resource1.txt";

    @Test
    public void testRelativeResourceLoadingFromFile() throws IOException {
        TestScript script = TestScript.getOne(TeaseScriptResourceLoadingTest.class);

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
        try (InputStream resourceStream = script.resources.getResource(path);
                BufferedReader reader = new BufferedReader(new InputStreamReader(resourceStream));) {
            String resource = reader.readLine();
            assertEquals("1", resource);
        }
    }

    @Test
    public void testAbsoluteResourceLoadingFromZip() throws IOException {
        TestScript script = TestScript.getOne();
        script.resources.addAssets("teaselib/core/UnpackResourcesTestData_ResourceRootStructure.zip");

        String path = absolutePath() + RESOURCE_1;
        loadResource(script, path);
    }

    private String absolutePath() {
        return ReflectionUtils.asAbsolutePath(ReflectionUtils.classParentName(this.getClass()));
    }

    @Test
    public void testRelativeResourceLoadingFromZip() throws IOException {
        TestScript script = TestScript.getOne(TeaseScriptResourceLoadingTest.class);
        script.resources.addAssets("teaselib/core/UnpackResourcesTestData_ResourceRootStructure.zip");

        String path = RESOURCE_1;
        loadResource(script, path);
    }

    @Test
    public void testResourceLoadingWithWildcardsRelativePaths() {
        TestScript script = TestScript.getOne(TeaseScriptResourceLoadingTest.class);

        assertEquals(1, script.resources("util/Foo.txt").size());
        assertEquals(2, script.resources("util/Foo?.txt").size());
        assertEquals(3, script.resources("util/Foo*.txt").size());
        Collection<String> items = script.resources("util/Foo*.txt");
        assertEquals(3, items.size());
    }

    @Test
    public void testResourceLoadingWithWildcardsAbsolutePaths() {
        TestScript script = TestScript.getOne(TeaseScriptResourceLoadingTest.class);

        String rootDir = "/" + getClass().getPackage().getName().replace(".", "/");
        assertEquals(1, script.resources(rootDir + "/util/Foo.txt").size());
        assertEquals(2, script.resources(rootDir + "/util/Foo?.txt").size());
        assertEquals(3, script.resources(rootDir + "/util/Foo*.txt").size());
        Collection<String> items = script.resources(rootDir + "/util/Foo*.txt");
        assertEquals(3, items.size());
    }

    @Test
    public void testResourceLoadingCaseRelative() {
        TestScript script = TestScript.getOne(TeaseScriptResourceLoadingTest.class);

        assertEquals(1, script.resources("util/bar.txt").size());
        assertEquals(3, script.resources("util/bar?.txt").size());
        assertEquals(4, script.resources("util/bar*.txt").size());
        assertEquals(0, script.resources("util/Bar.txt").size());
        assertEquals(2, script.resources("util/Bar?.txt").size());
        assertEquals(2, script.resources("util/Bar*.txt").size());
    }

    @Test
    public void testUnpackFileAbsolute() throws IOException {
        TestScript script = TestScript.getOne(TeaseScriptResourceLoadingTest.class);
        script.resources.addAssets("/teaselib/core/UnpackResourcesTestData_ResourceRootStructure.zip");

        String path = "/teaselib/core/UnpackResourcesTestData" + "/" + RESOURCE_1;
        assertTrue(script.resources.hasResource(path));
        File res1 = script.resources.unpackToFile(path);
        res1.delete();
        assertFalse(res1.exists());

        readContent(script, path);
    }

    @Test
    public void testUnpackFileRelative() throws IOException {
        TestScript script = TestScript.getOne(TeaseScriptResourceLoadingTest.class);
        script.resources.addAssets("/teaselib/core/UnpackResourcesTestData_ResourceRootStructure.zip");

        String path = "UnpackResourcesTestData" + "/" + RESOURCE_1;
        assertTrue(script.resources.hasResource(path));
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
        TestScript script = TestScript.getOne(TeaseScriptResourceLoadingTest.class);
        script.resources.addAssets("/teaselib/core/UnpackResourcesTestData_flat.zip");

        String path = "UnpackResourcesTestData" + "/" + RESOURCE_1;
        assertFalse(script.resources.hasResource(path));
    }

    @Test
    public void testThatPathsAreCompatibleBetweenDifferentResourceLoaders() {
        TestScript script1 = TestScript.getOne(TeaseScriptResourceLoadingTest.class);
        TestScript script2 = TestScript.getOne();

        assertFalse(script1.resources.equals(script2.resources));
        assertFalse(script1.resources.getAssetPath("").equals(script2.resources.getAssetPath("")));

        Collection<String> itemsFromRelative = script1.resources("util/Foo*.txt");
        Collection<String> itemsFromAbsolute = script2.resources("/teaselib/core/util/Foo*.txt");

        assertEquals(3, itemsFromRelative.size());
        assertEquals(itemsFromRelative, itemsFromAbsolute);
    }
}
