package teaselib.core.util;

import teaselib.core.configuration.Configuration;

public class ConfigFileMapping extends PropertyNameMapping {

    private final Configuration config;
    private final PropertyNameMapping nameMapping;

    public ConfigFileMapping(Configuration config, PropertyNameMapping nameMapping) {
        super(nameMapping);
        this.config = config;
        this.nameMapping = nameMapping;
    }

    @Override
    public String get(QualifiedName name) {
        // TODO Auto-generated method stub
        return super.get(name);
    }

    @Override
    public boolean getBoolean(QualifiedName name) {
        // TODO Auto-generated method stub
        return super.getBoolean(name);
    }

    @Override
    public void set(QualifiedName name, String value) {
        // TODO Auto-generated method stub
        super.set(name, value);
    }

    @Override
    public void set(QualifiedName name, boolean value) {
        // TODO Auto-generated method stub
        super.set(name, value);
    }

    @Override
    public void clear(QualifiedName name) {
        // TODO Auto-generated method stub
        super.clear(name);
    }

    @Override
    public PropertyNameMapping getNameMapping() {
        // TODO Auto-generated method stub
        return super.getNameMapping();
    }

}
