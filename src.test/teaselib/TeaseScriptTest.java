/**
 * 
 */
package teaselib;

import static org.junit.Assert.*;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import org.junit.Test;

import teaselib.core.ResourceLoader;
import teaselib.hosts.DummyHost;
import teaselib.hosts.DummyPersistence;

/**
 * @author someone
 *
 */
public class TeaseScriptTest {

    /**
     * 
     */
    private static final String RESOURCE_1 = "resource1.txt";

    private static TeaseScript createTestScript() {
        TeaseScript script = new TeaseScript(TeaseLib.init(new DummyHost(),
                new DummyPersistence()), new ResourceLoader(
                "x:/projects/teaseLib/", "bin.test"), "en-us", "test") {
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
                .replace(".", "/")
                + "/";
        final String expected = path + name;
        final String actual = script.resource(name);
        assertEquals(expected, actual);
    }

    @Test
    public void testResourceLoading() throws IOException {
        TeaseScript script = createTestScript();
        final String name = RESOURCE_1;
        final String path = script.resource(name);
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

}
