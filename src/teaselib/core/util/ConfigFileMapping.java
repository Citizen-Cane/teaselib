package teaselib.core.util;

import java.io.IOException;
import java.util.Locale;

import teaselib.Actor;
import teaselib.Sexuality.Gender;
import teaselib.core.Persistence;
import teaselib.core.TeaseLib;
import teaselib.core.UserItems;
import teaselib.core.configuration.Configuration;
import teaselib.util.TextVariables;

public class ConfigFileMapping implements Persistence {

    private final Configuration config;
    private final Persistence persistence;

    public ConfigFileMapping(Configuration config, Persistence persistence) {
        this.config = config;
        this.persistence = persistence;
    }

    @Override
    public String get(QualifiedName name) {
        return persistence.get(name);
    }

    @Override
    public boolean getBoolean(QualifiedName name) {
        return persistence.getBoolean(name);
    }

    @Override
    public void set(QualifiedName name, String value) {
        persistence.set(name, value);
    }

    @Override
    public void set(QualifiedName name, boolean value) {
        persistence.set(name, value);
    }

    @Override
    public void clear(QualifiedName name) {
        persistence.clear(name);
    }

    @Override
    public UserItems getUserItems(TeaseLib teaseLib) throws IOException {
        return persistence.getUserItems(teaseLib);
    }

    @Override
    public boolean has(QualifiedName name) {
        return persistence.has(name);
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
