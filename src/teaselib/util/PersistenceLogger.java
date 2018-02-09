package teaselib.util;

import java.io.IOException;
import java.util.Locale;

import org.slf4j.LoggerFactory;

import teaselib.Actor;
import teaselib.Sexuality.Gender;
import teaselib.core.Persistence;
import teaselib.core.TeaseLib;
import teaselib.core.UserItems;
import teaselib.core.util.PropertyNameMapping;

public class PersistenceLogger implements Persistence {
    private final org.slf4j.Logger logger;
    private final Persistence persistence;

    public PersistenceLogger(Persistence persistence) {
        super();
        this.persistence = persistence;
        this.logger = LoggerFactory.getLogger(persistence.getClass());
    }

    @Override
    public PropertyNameMapping getNameMapping() {
        return persistence.getNameMapping();
    }

    @Override
    public UserItems getUserItems(TeaseLib teaseLib) throws IOException {
        return persistence.getUserItems(teaseLib);
    }

    @Override
    public boolean has(String name) {
        boolean exists = persistence.has(name);
        logger.debug(name + (exists ? " exists" : " doesn't exist"));
        return exists;
    }

    @Override
    public String get(String name) {
        String value = persistence.get(name);
        logger.debug("get(" + name + ") = " + value);
        return value;
    }

    @Override
    public void set(String name, String value) {
        logger.debug("set " + name + " = " + value);
        persistence.set(name, value);
    }

    @Override
    public boolean getBoolean(String name) {
        boolean value = persistence.getBoolean(name);
        logger.debug("get(" + name + ")  = " + value);
        return value;
    }

    @Override
    public void set(String name, boolean value) {
        logger.debug("set " + name + " = " + value);
        persistence.set(name, value);
    }

    @Override
    public void clear(String name) {
        if (persistence.has(name)) {
            logger.debug("cleared " + name);
        }
        persistence.clear(name);
    }

    @Override
    public TextVariables getTextVariables(Locale locale) {
        return persistence.getTextVariables(locale);
    }

    @Override
    public Actor getDominant(Gender gender, Locale locale) {
        return persistence.getDominant(gender, locale);
    }

    @Override
    public String toString() {
        return persistence.toString();
    }

}
