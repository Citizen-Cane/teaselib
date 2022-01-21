package teaselib.core;

import teaselib.core.util.QualifiedName;

public interface Persistence {

    boolean has(QualifiedName name);

    String get(QualifiedName name);

    void set(QualifiedName name, String value);

    boolean getBoolean(QualifiedName name);

    void set(QualifiedName name, boolean value);

    void clear(QualifiedName name);

}
