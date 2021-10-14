package teaselib.core;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

import teaselib.State;
import teaselib.core.state.StateProxy;
import teaselib.core.util.Persist;
import teaselib.core.util.PersistedObject;
import teaselib.core.util.QualifiedString;
import teaselib.util.Items;

public class StateMaps {

    public interface Attributes {
        void applyAttributes(Object... attributes);
    }

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

    static String toStringWithoutRecursion(Set<QualifiedString> peers) {
        var toString = new StringBuilder();
        toString.append("[");
        for (QualifiedString peer : peers) {
            if (toString.length() > 1) {
                toString.append(", ");
            }
            toString.append(peer.toString());
        }
        toString.append("]");
        return toString.toString();
    }

    /**
     * Return the state of an enumeration member
     * 
     * @param item
     *            The enumeration member to return the state for
     * @return The item state.
     */
    @Deprecated
    State state(String domain, Object item) {
        return state(domain, QualifiedString.of(item));
    }

    State state(String domain, QualifiedString item) {
        if (PersistedObject.isPersistedString(item.toString())) {
            var stateMapForPersistedKey = stateMap(domain, item);
            var existing = stateMapForPersistedKey.get(item.toString());
            if (existing != null) {
                return existing;
            } else {
                var persistedKey = item.toString();
                State state;
                try {
                    state = Persist.from(persistedKey, clazz -> teaseLib);
                } catch (ReflectiveOperationException e) {
                    throw new IllegalArgumentException("Cannot restore state " + item + ": ", e);
                }
                stateMapForPersistedKey.put(persistedKey, state);

                var qualifiedItem = ((StateImpl) state).item;
                var stateMapForQualifiedKey = stateMap(domain, qualifiedItem);
                String qualifiedKey = qualifiedItem.name().toLowerCase();
                stateMapForQualifiedKey.put(qualifiedKey, state);
                return state;
            }
        } else {
            var stateMap = stateMap(domain, item);
            String key = item.name().toLowerCase();
            var state = stateMap.get(key);
            if (state == null) {
                state = new StateImpl(this, domain, item.value());
                stateMap.put(key, state);
            }
            return state;
        }
    }

    State state(String domain, State item) {
        if (item instanceof StateImpl) {
            var state = (StateImpl) item;
            var stateMap = stateMap(domain, state.item);
            String key = state.item.name();
            var existing = stateMap.get(key);
            if (existing == null) {
                stateMap.put(key, state);
                return state;
            } else if (!existing.equals(state)) {
                throw new IllegalArgumentException("States cannot be replaced: " + state + " -> " + existing);
            } else {
                return existing;
            }
        } else {
            throw new UnsupportedOperationException(item.toString());
        }
    }

    public static boolean hasAllAttributes(Set<? extends Object> available, Collection<? extends Object> desired) {
        Predicate<? super QualifiedString> predicate = attribute -> available.stream().map(StateMaps::stripState)
                .anyMatch(attribute::is);
        return desired.stream().map(StateMaps::stripState).filter(predicate).count() == desired.size();
    }

    public static Collection<Object> flatten(Object[] peers) {
        return flatten(Arrays.asList(peers));
    }

    public static Collection<Object> flatten(Collection<? extends Object> peers) {
        List<Object> flattenedPeers = new ArrayList<>(peers.size());
        for (Object peer : peers) {
            if (peer instanceof Items) {
                var items = (Items) peer;
                flattenedPeers.addAll(items.firstOfEachKind());
            } else if (peer instanceof Collection) {
                var collection = (Collection<?>) peer;
                flattenedPeers.addAll(collection);
            } else if (peer instanceof Object[]) {
                var list = Arrays.asList(peer);
                flattenedPeers.addAll(list);
            } else {
                flattenedPeers.add(peer);
            }
        }
        return flattenedPeers;
    }

    static QualifiedString stripState(Object value) {
        if (value instanceof StateImpl) {
            return stripState((StateImpl) value);
        } else if (value instanceof StateProxy) {
            return stripState(((StateProxy) value).state);
        } else if (value instanceof QualifiedString) {
            return (QualifiedString) value;
        } else {
            return QualifiedString.of(value);
        }
    }

    private static QualifiedString stripState(StateImpl state) {
        return state.item;
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
