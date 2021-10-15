package teaselib.core;

import java.util.Collection;
import java.util.HashMap;
import java.util.Set;
import java.util.function.Predicate;

import teaselib.State;
import teaselib.core.util.Persist;
import teaselib.core.util.PersistedObject;
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
    State state(String domain, QualifiedString name) {
        if (PersistedObject.isPersistedString(name.toString())) {
            var stateMap = stateMap(domain, name);
            var state = stateMap.get(name);
            if (state == null) {
                try {
                    state = Persist.from(name.toString(), clazz -> teaseLib);
                } catch (ReflectiveOperationException e) {
                    throw new IllegalArgumentException("Cannot restore state " + name + ": ", e);
                }
                stateMap.put(name, state);
            }
            return state;
        } else {
            var stateMap = stateMap(domain, name);
            var state = stateMap.get(name);
            if (state == null) {
                state = new StateImpl(this, domain, name.value());
                stateMap.put(name.kind(), state);
            }
            return state;
        }
    }

    State state(String domain, State item) {
        if (item instanceof StateImpl) {
            var state = (StateImpl) item;
            var stateMap = stateMap(domain, state.item);
            var existing = stateMap.get(state.item);
            if (existing == null) {
                stateMap.put(state.item, state);
                return state;
            } else if (existing == state) {
                throw new IllegalArgumentException("States cannot be replaced: " + state + " -> " + existing);
            } else {
                return existing;
            }
        } else {
            throw new UnsupportedOperationException(item.toString());
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
