/**
 * 
 */
package teaselib.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Collection;

import org.junit.Test;

import teaselib.core.util.ReflectionUtils;
import teaselib.test.TestScript;

/**
 * @author someone
 *
 */
public class TeaseScriptResourceLoadingTest {
    private static final String RESOURCE_1 = "resource1.txt";

    @Test
    public void testRelativeResourceLoadingFromFile() throws IOException {
        TestScript script = TestScript
                .getOne(TeaseScriptResourceLoadingTest.class);

        String relativePath = RESOURCE_1;
        loadResource(script, relativePath);
    }

    @Test
    public void testAbsoluteResourceLoadingFromFile() throws IOException {
        TestScript script = TestScript
                .getOne(TeaseScriptResourceLoadingTest.class);

        String name = "/" + ReflectionUtils.classParentName(this.getClass())
                .replace(".", "/") + "/" + RESOURCE_1;
        loadResource(script, name);
    }

    private static void loadResource(TestScript script, String path)
            throws IOException {
        InputStream resourceStream = script.resources.getResource(path);
        String resource = null;
        BufferedReader reader = new BufferedReader(
                new InputStreamReader(resourceStream));
        try {
            resource = reader.readLine();
        } finally {
            reader.close();
        }
        assertEquals("1", resource);
    }

    @Test
    public void testAbsoluteResourceLoadingFromZip() throws IOException {
        TestScript script = TestScript.getOne();
        script.resources
                .addAssets("/teaselib/core/UnpackResourcesTestData.zip");

        String path = "UnpackResourcesTestData" + "/" + RESOURCE_1;
        loadResource(script, path);
    }

    @Test
    public void testResourceLoadingWithWildcardsRelativePaths() {
        TestScript script = TestScript
                .getOne(TeaseScriptResourceLoadingTest.class);

        assertEquals(1, script.resources("util/Foo.txt").size());
        assertEquals(2, script.resources("util/Foo?.txt").size());
        assertEquals(3, script.resources("util/Foo*.txt").size());
        Collection<String> items = script.resources("util/Foo*.txt");
        assertEquals(3, items.size());
    }

    @Test
    public void testResourceLoadingWithWildcardsAbsolutePaths() {
        TestScript script = TestScript
                .getOne(TeaseScriptResourceLoadingTest.class);

        String rootDir = "/"
                + getClass().getPackage().getName().replace(".", "/");
        assertEquals(1, script.resources(rootDir + "/util/Foo.txt").size());
        assertEquals(2, script.resources(rootDir + "/util/Foo?.txt").size());
        assertEquals(3, script.resources(rootDir + "/util/Foo*.txt").size());
        Collection<String> items = script.resources(rootDir + "/util/Foo*.txt");
        assertEquals(3, items.size());
    }

    @Test
    public void testResourceLoadingCaseRelative() {
        TestScript script = TestScript
                .getOne(TeaseScriptResourceLoadingTest.class);

        assertEquals(1, script.resources("util/bar.txt").size());
        assertEquals(3, script.resources("util/bar?.txt").size());
        assertEquals(4, script.resources("util/bar*.txt").size());
        assertEquals(0, script.resources("util/Bar.txt").size());
        assertEquals(2, script.resources("util/Bar?.txt").size());
        assertEquals(2, script.resources("util/Bar*.txt").size());
    }

    @Test
    public void testUnpackFile() throws IOException {
        TestScript script = TestScript.getOne();
        script.resources
                .addAssets("/teaselib/core/UnpackResourcesTestData.zip");

        final String path = "UnpackResourcesTestData" + "/" + RESOURCE_1;
        File res1 = script.resources.unpackToFile(path);
        res1.delete();
        assertFalse(res1.exists());

        res1 = script.resources.unpackToFile(path);
        try {
            String resource1 = null;
            BufferedReader reader = new BufferedReader(new FileReader(res1));
            try {
                resource1 = reader.readLine();
            } finally {
                reader.close();
            }
            assertEquals("1", resource1);
        } finally {
            res1.delete();
        }
    }
}
