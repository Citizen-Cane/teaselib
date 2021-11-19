package teaselib.core;

import java.util.Collection;
import java.util.HashMap;
import java.util.Set;
import java.util.function.Predicate;

import teaselib.core.util.QualifiedString;

public class StateMaps {

    class StateMapCache extends HashMap<String, StateMap> {
        private static final long serialVersionUID = 1L;
    }

    class Domains extends HashMap<String, StateMapCache> {
        private static final long serialVersionUID = 1L;
    }

    final TeaseLib teaseLib;
    final Domains cache = new Domains();

    StateMaps(TeaseLib teaseLib) {
        this.teaseLib = teaseLib;
        clear();
    }

    void clear() {
        cache.clear();
    }

    /**
     * Return the state of an enumeration member
     * 
     * @param domain
     *            The domain name
     * @param name
     *            The enumeration member to return the state for
     * @return The state.
     */
    StateImpl state(String domain, QualifiedString name) {
        checkNotItem(name);

        var stateMap = stateMap(domain, name);
        var state = stateMap.get(name);
        if (state == null) {
            state = new StateImpl(this, domain, name);
            stateMap.put(name, state);
        }
        return state;
    }

    static void checkNotItem(QualifiedString name) {
        if (name.guid().isPresent()) {
            throw new IllegalArgumentException("Guids cannot be states: " + name);
        }
    }

    public static boolean hasAllAttributes(Set<QualifiedString> available, Collection<QualifiedString> desired) {
        Predicate<? super QualifiedString> predicate = attribute -> available.stream().anyMatch(attribute::is);
        return desired.stream().filter(predicate).count() == desired.size();
    }

    StateMap stateMap(String domain, QualifiedString item) {
        return stateMap(domain, item.namespace());
    }

    private StateMap stateMap(String domain, String namespace) {
        StateMapCache domainCache = getDomainCache(domain);
        return domainCache.computeIfAbsent(namespace.toLowerCase(), key -> new StateMap(domain));
    }

    private StateMapCache getDomainCache(String name) {
        return cache.computeIfAbsent(name.toLowerCase(), key -> new StateMapCache());
    }

}
