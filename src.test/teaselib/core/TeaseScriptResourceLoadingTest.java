/**
 * 
 */
package teaselib.core;

import static org.junit.Assert.*;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Collection;
import java.util.regex.Pattern;

import org.junit.Test;

import teaselib.Actor;
import teaselib.TeaseLib;
import teaselib.TeaseScript;
import teaselib.core.texttospeech.Voice;
import teaselib.hosts.DummyHost;
import teaselib.hosts.DummyPersistence;

/**
 * @author someone
 *
 */
public class TeaseScriptResourceLoadingTest {

    /**
     * 
     */
    private static final String RESOURCE_1 = "resource1.txt";

    private static TeaseScript createTestScript() {
        TeaseScript script = new TeaseScript(
                TeaseLib.init(new DummyHost(), new DummyPersistence()),
                new ResourceLoader(TeaseScriptResourceLoadingTest.class),
                new Actor(Actor.Key.Dominant, Voice.Gender.Female, "en-us"),
                "test") {
            @Override
            public void run() {
            }
        };
        script.resources.addAssets(
                script.absoluteResource("UnpackResourcesTestData.zip"));
        return script;
    }

    @Test
    public void testResourcePathBuilding() {
        TeaseScript script = createTestScript();
        final String name = "dummy";
        final String path = script.getClass().getPackage().getName()
                .replace(".", "/") + "/";
        final String expected = "/" + path + name;
        final String actual = script.absoluteResource(name);
        assertEquals(expected, actual);
    }

    @Test
    public void testAbsoluteResourceLoadingFromFile() throws IOException {
        TeaseScript script = createTestScript();
        final String name = RESOURCE_1;
        final String path = script.absoluteResource(name);
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
    public void testAbsoluteResourceLoadingFromZip() throws IOException {
        TeaseScript script = createTestScript();
        final String path = "UnpackResourcesTestData" + "/" + RESOURCE_1;
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
    public void testResourceLoadingWithWildcards() {
        TeaseScript script = createTestScript();
        assertEquals(1, script.resources("util/Foo.txt").size());
        assertEquals(2, script.resources("util/Foo?.txt").size());
        assertEquals(3, script.resources("util/Foo*.txt").size());
    }

    @Test
    public void testResourceLoadingWithWildcardsAbsolutePaths() {
        TeaseScript script = createTestScript();
        assertEquals(1, script.resources("/teaselib/core/util/Foo.txt").size());
        assertEquals(2,
                script.resources("/teaselib/core/util/Foo?.txt").size());
        assertEquals(3,
                script.resources("/teaselib/core/util/Foo*.txt").size());
        Collection<String> items = script
                .resources("/teaselib/core/util/Foo*.txt");
        assertEquals(3, items.size());
    }

    @Test
    public void testResourceLoadingCase() {
        TeaseScript script = createTestScript();
        assertEquals(1, script.resources("util/bar.txt").size());
        assertEquals(3, script.resources("util/bar?.txt").size());
        assertEquals(4, script.resources("util/bar*.txt").size());
        assertEquals(0, script.resources("util/Bar.txt").size());
        assertEquals(2, script.resources("util/Bar?.txt").size());
        assertEquals(2, script.resources("util/Bar*.txt").size());
    }

    @Test
    public void testUnpackFile() throws IOException {
        TeaseScript script = createTestScript();
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
        TeaseScript script = createTestScript();
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
