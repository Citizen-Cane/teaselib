package teaselib.util;

import java.util.Locale;

import org.slf4j.LoggerFactory;

import teaselib.Actor;
import teaselib.core.Persistence;
import teaselib.core.texttospeech.Voice.Gender;
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
    public boolean has(String name) {
        boolean exists = persistence.has(name);
        logger.info("Persistence: " + name
                + (exists ? " exists" : " doesn't exist"));
        return exists;
    }

    @Override
    public String get(String name) {
        String value = persistence.get(name);
        logger.info("Persistence: get(" + name + ") = " + value);
        return value;
    }

    @Override
    public void set(String name, String value) {
        logger.info("Persistence: set " + name + " = " + value);
        persistence.set(name, value);
    }

    @Override
    public boolean getBoolean(String name) {
        boolean value = persistence.getBoolean(name);
        logger.info("Persistence: get(" + name + ") = " + value);
        return value;
    }

    @Override
    public void set(String name, boolean value) {
        logger.info("Persistence: set " + name + " = " + value);
        persistence.set(name, value);
    }

    @Override
    public void clear(String name) {
        logger.info("Persistence: cleared " + name);
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
}
