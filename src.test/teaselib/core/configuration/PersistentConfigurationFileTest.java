package teaselib.core.configuration;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.function.Executable;
import org.junit.jupiter.api.io.TempDir;

import teaselib.core.util.ExceptionUtil;
import teaselib.core.util.QualifiedName;
import teaselib.test.TestScript;

import static org.junit.jupiter.api.Assertions.*;

public class PersistentConfigurationFileTest {
    @TempDir
    Path folder;

    @Test
    public void testIO() throws IOException {
        DebugSetup setup = new DebugSetup().withUserPath(folder.toFile());
        try (TestScript script = new TestScript(setup)) {
            File settingsFolder = new File(folder.toFile(), Configuration.SCRIPT_SETTINGS);
            File file = new File(settingsFolder, script.namespace + Configuration.PROPERTIES_EXTENSION);

            script.persistence.newBoolean("testVariableName").set(true);
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
        try (TestScript script = new TestScript(new DebugSetup().withUserPath(folder.toFile()))) {
            Configuration config = script.teaseLib.config;
            File settingsFolder = new File(folder.toFile(), Configuration.SCRIPT_SETTINGS);
            Assertions.assertThrows(IllegalArgumentException.class, changeStorageLocation(script, config, settingsFolder));
        }
    }

    private static Executable changeStorageLocation(TestScript script, Configuration config,
                                                    File settingsFolder) {
        return () -> config.addPersistentUserProperties("test.properties", settingsFolder, script.namespace);
    }

    @Test
    public void testPersistentSettings() throws IOException {
        DebugSetup setupWithUerPath = new DebugSetup().withUserPath(folder.toFile());

        try (TestScript script = new TestScript(setupWithUerPath)) {
            File file = new File(new File(folder.toFile(), Configuration.SCRIPT_SETTINGS),
                    script.namespace + ".properties");

            script.persistence.newBoolean("testVariableName").set(true);
            assertFalse(file.exists());
            script.say("Write async on say");
            script.teaseLib.config.close(); // flush files
            assertTrue(file.exists());
        }

        try (TestScript script = new TestScript(new DebugSetup())) {
            assertFalse(script.persistence.newBoolean("testVariableName").value());
        }

        try (TestScript script = new TestScript(setupWithUerPath)) {
            assertTrue(script.persistence.newBoolean("testVariableName").value());
        }
    }

    @Test
    public void testCasePropertyFile() throws IOException {
        ConfigurationFile caseSensitive = new PersistentConfigurationFile(
                Paths.get(folder.toFile().getAbsolutePath(), Configuration.SCRIPT_SETTINGS), f -> {
                    try {
                        f.store();
                    } catch (IOException e) {
                        throw ExceptionUtil.asRuntimeException(e);
                    }
                });
        caseSensitive.set("test", true);
        assertTrue(caseSensitive.getBoolean("test"));
        assertFalse(caseSensitive.getBoolean("TEST"));

        ConfigurationFile caseInvariant = new LowerCaseNames(caseSensitive);
        assertTrue(caseInvariant.getBoolean("test"));
        assertTrue(caseInvariant.getBoolean("TEST"));
    }

    @Test
    public void testCaseIgnoredForScriptSettings() throws IOException {
        DebugSetup setup = new DebugSetup().withUserPath(folder.toFile());

        try (TestScript script = new TestScript(setup)) {
            script.persistence.newBoolean("testVariableName").set(true);
            assertTrue(script.persistence.newBoolean("testVariableName").value());
            assertTrue(script.persistence.newBoolean("TESTVARIABLENAME").value());
            assertTrue(script.persistence.newBoolean("testvariablename").value());
            script.teaseLib.config.close(); // flush files
        }

        try (TestScript script = new TestScript(new DebugSetup())) {
            assertFalse(script.persistence.newBoolean("testVariableName").value());
        }

        try (TestScript script = new TestScript(setup)) {
            assertTrue(script.persistence.newBoolean("testVariableName").value());
            assertTrue(script.persistence.newBoolean("TESTVARIABLENAME").value());
            assertTrue(script.persistence.newBoolean("testvariablename").value());
        }
    }

    @Test
    public void testCaseIgnoredForClear() throws IOException {
        DebugSetup setup = new DebugSetup().withUserPath(folder.toFile());

        try (TestScript script = new TestScript(setup)) {
            script.persistence.newBoolean("testVariableName").set(true);
            script.teaseLib.config.close(); // flush files
        }

        try (TestScript script = new TestScript(setup)) {
            assertTrue(script.persistence.newBoolean("testVariableName").value());
            script.persistence.newBoolean("testVariableName").clear();
            assertFalse(script.persistence.newBoolean("testVariableName").value());
        }

        try (TestScript script = new TestScript(setup)) {
            assertFalse(script.persistence.newBoolean("testVariableName").value());
        }
    }

}
