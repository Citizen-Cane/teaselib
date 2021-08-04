package teaselib.core.configuration;

public interface ConfigurationFile {

    boolean has(String key);

    String get(String key);

    boolean getBoolean(String key);

    void set(String key, String value);

    void set(String key, boolean value);

    void clear(String key);

}
