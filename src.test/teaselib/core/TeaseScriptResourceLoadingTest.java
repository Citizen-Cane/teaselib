/**
 * 
 */
package teaselib.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Collection;
import java.util.regex.Pattern;

import org.junit.Test;

import teaselib.test.TestScript;

/**
 * @author someone
 *
 */
public class TeaseScriptResourceLoadingTest {
    private static final String RESOURCE_1 = "resource1.txt";

    @Test
    public void testResourcePathBuilding() {
        TestScript script = TestScript.getOne();

        final String name = "dummy";
        final String path = script.getClass().getPackage().getName()
                .replace(".", "/") + "/";
        final String expected = "/" + path + name;
        final String actual = script.absoluteResource(name);
        assertEquals(expected, actual);
    }

    @Test
    public void testAbsoluteResourceLoadingFromFile() throws IOException {
        TestScript script = TestScript
                .getOne(TeaseScriptResourceLoadingTest.class);

        String name = RESOURCE_1;
        InputStream res1 = script.resources.getResource(name);
        String resource1 = null;
        BufferedReader reader = new BufferedReader(new InputStreamReader(res1));
        try {
            resource1 = reader.readLine();
        } finally {
            reader.close();
        }
        assertEquals("1", resource1);
    }

    @Test
    public void testAbsoluteResourceLoadingFromZip() throws IOException {
        TestScript script = TestScript.getOne();
        script.resources.addAssets(script.absoluteResource(
                "/teaselib/core/UnpackResourcesTestData.zip"));

        String path = "UnpackResourcesTestData" + "/" + RESOURCE_1;
        InputStream res1 = script.resources.getResource(path);
        String resource1 = null;
        BufferedReader reader = new BufferedReader(new InputStreamReader(res1));
        try {
            resource1 = reader.readLine();
        } finally {
            reader.close();
        }
        assertEquals("1", resource1);
    }

    @Test
    public void testResourceLoadingAbsolutePathWithWildcards() {
        TestScript script = TestScript
                .getOne(TeaseScriptResourceLoadingTest.class);

        assertEquals(1, script.resources("/util/Foo.txt").size());
        assertEquals(2, script.resources("/util/Foo?.txt").size());
        assertEquals(3, script.resources("/util/Foo*.txt").size());
    }

    @Test
    public void testResourceLoadingWithWildcardsAbsolutePaths() {
        TestScript script = TestScript
                .getOne(TeaseScriptResourceLoadingTest.class);

        assertEquals(1, script.resources("/util/Foo.txt").size());
        assertEquals(2, script.resources("/util/Foo?.txt").size());
        assertEquals(3, script.resources("/util/Foo*.txt").size());
        Collection<String> items = script.resources("/util/Foo*.txt");
        assertEquals(3, items.size());
    }

    @Test
    public void testResourceLoadingCase() {
        TestScript script = TestScript
                .getOne(TeaseScriptResourceLoadingTest.class);

        assertEquals(1, script.resources("/util/bar.txt").size());
        assertEquals(3, script.resources("/util/bar?.txt").size());
        assertEquals(4, script.resources("/util/bar*.txt").size());
        assertEquals(0, script.resources("/util/Bar.txt").size());
        assertEquals(2, script.resources("/util/Bar?.txt").size());
        assertEquals(2, script.resources("/util/Bar*.txt").size());
    }

    @Test
    public void testUnpackFile() throws IOException {
        TestScript script = TestScript.getOne();
        script.resources.addAssets(script.absoluteResource(
                "/teaselib/core/UnpackResourcesTestData.zip"));

        final String path = "UnpackResourcesTestData" + "/" + RESOURCE_1;
        File res1 = script.resources.unpackToFile(path);
        String resource1 = null;
        BufferedReader reader = new BufferedReader(new FileReader(res1));
        try {
            resource1 = reader.readLine();
        } finally {
            reader.close();
        }
        assertEquals("1", resource1);
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

    @Test
    public void testUnpackFolder() throws IOException {
        TestScript script = TestScript.getOne();
        script.resources.addAssets(script.absoluteResource(
                "/teaselib/core/UnpackResourcesTestData.zip"));

        final String resources = "/" + "UnpackResourcesTestData" + "/";
        final String path = resources + RESOURCE_1;
        File res1 = script.resources.unpackEnclosingFolder(path);

        // Delete test data
        deleteFolder(res1.getParentFile());
        assertFalse(res1.exists());

        Collection<String> itemsBefore = script.resources
                .resources(Pattern.compile(resources + ".*"));
        assertTrue(itemsBefore.size() > 0);

        // Cache test data
        res1 = script.resources.unpackEnclosingFolder(path);
        String resource1Content = null;
        BufferedReader reader = new BufferedReader(new FileReader(res1));
        try {
            resource1Content = reader.readLine();
        } finally {
            reader.close();
        }
        assertEquals("1", resource1Content);

        // Test whether duplicate resources aren't listed
        Collection<String> itemsAfter = script.resources
                .resources(Pattern.compile(resources + ".*"));
        assertEquals(itemsBefore, itemsAfter);

        // Repeat with cached content
        res1 = script.resources.unpackEnclosingFolder(path);
        reader = new BufferedReader(new FileReader(res1));
        try {
            resource1Content = reader.readLine();
        } finally {
            reader.close();
        }
        assertEquals("1", resource1Content);
    }
}
