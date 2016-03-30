/**
 * 
 */
package teaselib.core;

import static org.junit.Assert.*;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;

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
                new Actor(Actor.Dominant, Voice.Gender.Female, "en-us"),
                "test") {
            @Override
            public void run() {
            }
        };
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
    public void testAbsoluteResourceLoading() throws IOException {
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
    public void testResourceLoadingWithWildcards() {
        TeaseScript script = createTestScript();
        assertEquals(1, script.resources("util/Foo.txt").size());
        assertEquals(2, script.resources("util/Foo?.txt").size());
        assertEquals(3, script.resources("util/Foo*.txt").size());
    }

    @Test
    public void testResourceLoadingWithWildcardsAbsolutePaths() {
        TeaseScript script = createTestScript();
        assertEquals(1, script.resources("/teaselib/core/util/Foo.txt")
                .size());
        assertEquals(2, script
                .resources("/teaselib/core/util/Foo?.txt").size());
        assertEquals(3, script
                .resources("/teaselib/core/util/Foo*.txt").size());
        List<String> items = script
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

}
