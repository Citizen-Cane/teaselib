package teaselib.host.sexscripts;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ss.IScript;
import teaselib.core.Persistence;
import teaselib.core.util.QualifiedName;
import teaselib.core.util.Stream;

/**
 * @author Citizen-Cane
 *
 */
public class SexScriptsPersistence implements Persistence {
    private static final Logger logger = LoggerFactory.getLogger(SexScriptsHost.class);

    private static final String DATA_PROPERTIES = "data.properties";
    private static final String DATA_PROPERTIES_BACKUP = "data backup.properties";
    private static final String PROPERTY_FILE_VALID_TAG = "intro.start_script";
    private static final String FALSE = "false";
    private static final String TRUE = "true";

    private final ss.IScript host;

    public SexScriptsPersistence(IScript host) {
        this.host = host;
        if (has(PROPERTY_FILE_VALID_TAG)) {
            makeDataPropertiesBackup();
        } else {
            if (new File(DATA_PROPERTIES_BACKUP).exists()) {
                throw new IllegalStateException("Data property file corrupt - restore it from backup.");
            } else {
                throw new IllegalStateException("Please run SexScripts intro first.");
            }
        }
    }

    private static void makeDataPropertiesBackup() {
        try (FileInputStream dataProperties = new FileInputStream(new File(DATA_PROPERTIES));
                FileOutputStream dataPropertiesBackup = new FileOutputStream(new File(DATA_PROPERTIES_BACKUP));) {
            Stream.copy(dataProperties, dataPropertiesBackup);
        } catch (IOException e) {
            logger.error(e.getMessage(), e);
        }
    }

    @Override
    public boolean has(QualifiedName name) {
        return has(name.toString());
    }

    private boolean has(String name) {
        return get(name) != null;
    }

    @Override
    public boolean getBoolean(QualifiedName name) {
        String value = get(name);
        if (value == null) {
            return false;
        } else {
            return !value.equals(FALSE);
        }
    }

    @Override
    public String get(QualifiedName name) {
        return get(name.toString());
    }

    private String get(String name) {
        return host.loadString(name);
    }

    @Override
    public void set(QualifiedName name, String value) {
        set(name.toString(), value);
    }

    @Override
    public void set(QualifiedName name, boolean value) {
        set(name.toString(), value ? TRUE : FALSE);
    }

    private void set(String name, String value) {
        host.save(name, value);
    }

    @Override
    public void clear(QualifiedName name) {
        host.save(name.toString(), null);
    }

}
