package teaselib.core.configuration;

import java.util.function.Predicate;

import teaselib.core.Persistence;
import teaselib.core.util.QualifiedName;

/**
 * @author Citizen-Cane
 *
 */
public class PersistenceFilter implements Persistence {

    private final Predicate<QualifiedName> filter;
    private final Persistence match;
    private final Persistence mismatch;

    public PersistenceFilter(Predicate<QualifiedName> filter, Persistence match, Persistence mismatch) {
        this.filter = filter;
        this.match = match;
        this.mismatch = mismatch;
    }

    // TODO SexScripts reads/write case insensitive
    // -> replace SortedProperties to use String::compareToIgnoreCase
    // -> until then settings will not match because we're not going to write lowercase

    @Override
    public boolean has(QualifiedName name) {
        return filter.test(name) ? match.has(name) : mismatch.has(name);
    }

    @Override
    public String get(QualifiedName name) {
        return filter.test(name) ? match.get(name) : mismatch.get(name);
    }

    @Override
    public void set(QualifiedName name, String value) {
        if (filter.test(name)) {
            match.set(name, value);
        } else {
            mismatch.set(name, value);
        }
    }

    @Override
    public boolean getBoolean(QualifiedName name) {
        return filter.test(name) ? match.getBoolean(name) : mismatch.getBoolean(name);
    }

    @Override
    public void set(QualifiedName name, boolean value) {
        if (filter.test(name)) {
            match.set(name, value);
        } else {
            mismatch.set(name, value);
        }
    }

    @Override
    public void clear(QualifiedName name) {
        if (filter.test(name)) {
            match.clear(name);
        } else {
            mismatch.clear(name);
        }
    }

}
