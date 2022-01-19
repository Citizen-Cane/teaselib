package teaselib.core.configuration;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.Properties;

import org.junit.Rule;
import org.junit.Test;
import org.junit.function.ThrowingRunnable;
import org.junit.rules.TemporaryFolder;

import teaselib.core.util.ExceptionUtil;
import teaselib.core.util.QualifiedName;
import teaselib.test.TestScript;

public class PersistentConfigurationFileTest {
    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    @Test
    public void testIO() throws IOException {
        DebugSetup setup = new DebugSetup().withUserPath(folder.getRoot());
        try (TestScript script = new TestScript(setup)) {
            File settingsFolder = new File(folder.getRoot(), Configuration.SCRIPT_SETTINGS);
            File file = new File(settingsFolder, script.namespace + Configuration.PROPERTIES_EXTENSION);

            script.persistentBoolean("testVariableName").set(true);
            assertFalse(file.exists());
            script.teaseLib.config.close(); // flush files
            assertTrue(file.exists());

            Properties test = new Properties();
            try (FileInputStream fileInputStream = new FileInputStream(file)) {
                test.load(fileInputStream);
            }

            assertEquals("true", test
                    .getProperty(new QualifiedName("", script.namespace, "testVariableName").toString().toLowerCase()));
        }
    }

    @Test
    public void testRegisterAgain() throws IOException {
        try (TestScript script = new TestScript(new DebugSetup().withUserPath(folder.getRoot()))) {
            Configuration config = script.teaseLib.config;
            File settingsFolder = new File(folder.getRoot(), Configuration.SCRIPT_SETTINGS);
            assertThrows(IllegalArgumentException.class, changeStorageLocation(script, config, settingsFolder));
        }
    }

    private static ThrowingRunnable changeStorageLocation(TestScript script, Configuration config,
            File settingsFolder) {
        return () -> config.addPersistentUserProperties("test.properties", settingsFolder, script.namespace);
    }

    @Test
    public void testPersistentSettings() throws IOException {
        DebugSetup setupWithUerPath = new DebugSetup().withUserPath(folder.getRoot());

        try (TestScript script = new TestScript(setupWithUerPath)) {
            File file = new File(new File(folder.getRoot(), Configuration.SCRIPT_SETTINGS),
                    script.namespace + ".properties");

            script.persistentBoolean("testVariableName").set(true);
            assertFalse(file.exists());
            script.say("Write async on say");
            script.teaseLib.config.close(); // flush files
            assertTrue(file.exists());
        }

        try (TestScript script = new TestScript(new DebugSetup())) {
            assertFalse(script.persistentBoolean("testVariableName").value());
        }

        try (TestScript script = new TestScript(setupWithUerPath)) {
            assertTrue(script.persistentBoolean("testVariableName").value());
        }
    }

    @Test
    public void testCasePropertyFile() throws IOException {
        ConfigurationFile caseSensitive = new PersistentConfigurationFile(
                Paths.get(folder.getRoot().getAbsolutePath(), Configuration.SCRIPT_SETTINGS), f -> {
                    try {
                        f.store();
                    } catch (IOException e) {
                        throw ExceptionUtil.asRuntimeException(e);
                    }
                });
        caseSensitive.set("test", true);
        assertEquals(true, caseSensitive.getBoolean("test"));
        assertEquals(false, caseSensitive.getBoolean("TEST"));

        ConfigurationFile caseInvariant = new LowerCaseNames(caseSensitive);
        assertEquals(true, caseInvariant.getBoolean("test"));
        assertEquals(true, caseInvariant.getBoolean("TEST"));
    }

    @Test
    public void testCaseIgnoredForScriptSettings() throws IOException {
        DebugSetup setup = new DebugSetup().withUserPath(folder.getRoot());

        try (TestScript script = new TestScript(setup)) {
            script.persistentBoolean("testVariableName").set(true);
            assertTrue(script.persistentBoolean("testVariableName").value());
            assertTrue(script.persistentBoolean("TESTVARIABLENAME").value());
            assertTrue(script.persistentBoolean("testvariablename").value());
            script.teaseLib.config.close(); // flush files
        }

        try (TestScript script = new TestScript(new DebugSetup())) {
            assertFalse(script.persistentBoolean("testVariableName").value());
        }

        try (TestScript script = new TestScript(setup)) {
            assertTrue(script.persistentBoolean("testVariableName").value());
            assertTrue(script.persistentBoolean("TESTVARIABLENAME").value());
            assertTrue(script.persistentBoolean("testvariablename").value());
        }
    }

    @Test
    public void testCaseIgnoredForClear() throws IOException {
        DebugSetup setup = new DebugSetup().withUserPath(folder.getRoot());

        try (TestScript script = new TestScript(setup)) {
            script.persistentBoolean("testVariableName").set(true);
            script.teaseLib.config.close(); // flush files
        }

        try (TestScript script = new TestScript(setup)) {
            assertTrue(script.persistentBoolean("testVariableName").value());
            script.persistentBoolean("testVariableName").clear();
            assertFalse(script.persistentBoolean("testVariableName").value());
        }

        try (TestScript script = new TestScript(setup)) {
            assertFalse(script.persistentBoolean("testVariableName").value());
        }
    }

}
