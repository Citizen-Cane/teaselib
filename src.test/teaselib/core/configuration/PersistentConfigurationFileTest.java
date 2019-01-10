package teaselib.core.configuration;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Optional;
import java.util.Properties;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import teaselib.core.util.QualifiedName;
import teaselib.test.TestScript;

public class PersistentConfigurationFileTest {
    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    @Test
    public void testIO() throws IOException, InterruptedException {
        TestScript script = TestScript.getOne(new DebugSetup());
        Configuration config = script.teaseLib.config;
        File file = new File(folder.getRoot(), "test.properties");
        assertFalse(file.exists());

        config.addPersistentUserProperties(Optional.empty(), "test.properties", folder.getRoot(),
                Arrays.asList(script.namespace));
        script.persistentBoolean("testVariableName").set(true);
        config.persistentConfigurationFiles.flush();

        Properties test = new Properties();
        try (FileInputStream fileInputStream = new FileInputStream(file)) {
            test.load(fileInputStream);
        }

        assertEquals("true", test.getProperty(new QualifiedName("", script.namespace, "testVariableName").toString()));
    }

}
