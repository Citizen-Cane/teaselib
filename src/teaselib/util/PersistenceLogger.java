package teaselib.util;

import org.slf4j.LoggerFactory;

import teaselib.core.Persistence;
import teaselib.core.util.QualifiedName;

public class PersistenceLogger implements Persistence {
    private final org.slf4j.Logger logger;
    private final Persistence persistence;

    public PersistenceLogger(Persistence persistence) {
        super();
        this.persistence = persistence;
        this.logger = LoggerFactory.getLogger(persistence.getClass());
    }

    @Override
    public boolean has(QualifiedName name) {
        boolean exists = persistence.has(name);
        logger.debug(name + (exists ? " exists" : " doesn't exist"));
        return exists;
    }

    @Override
    public String get(QualifiedName name) {
        String value = persistence.get(name);
        logger.debug("get(" + name + ") = " + value);
        return value;
    }

    @Override
    public void set(QualifiedName name, String value) {
        logger.debug("set " + name + " = " + value);
        persistence.set(name, value);
    }

    @Override
    public boolean getBoolean(QualifiedName name) {
        boolean value = persistence.getBoolean(name);
        logger.debug("get(" + name + ")  = " + value);
        return value;
    }

    @Override
    public void set(QualifiedName name, boolean value) {
        logger.debug("set " + name + " = " + value);
        persistence.set(name, value);
    }

    @Override
    public void clear(QualifiedName name) {
        if (persistence.has(name)) {
            logger.debug("cleared " + name);
        }
        persistence.clear(name);
    }

    @Override
    public String toString() {
        return persistence.toString();
    }

}
