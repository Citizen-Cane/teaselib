package teaselib.core.configuration;

public class LowerCaseNames implements ConfigurationFile {

    private final ConfigurationFile file;

    public LowerCaseNames(ConfigurationFile file) {
        this.file = file;
    }

    @Override
    public boolean has(String key) {
        return file.has(key.toLowerCase());
    }

    @Override
    public String get(String key) {
        return file.get(key.toLowerCase());
    }

    @Override
    public boolean getBoolean(String key) {
        return file.getBoolean(key.toLowerCase());
    }

    @Override
    public void set(String key, String value) {
        file.set(key.toLowerCase(), value);
    }

    @Override
    public void set(String key, boolean value) {
        file.set(key.toLowerCase(), value);
    }

    @Override
    public void clear(String key) {
        file.clear(key.toLowerCase());
    }

}
