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
import teaselib.core.util.QualifiedItem;
import teaselib.core.util.QualifiedString;
import teaselib.util.ItemGuid;
import teaselib.util.ItemImpl;
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

    public StateMaps(TeaseLib teaseLib) {
        this.teaseLib = teaseLib;
        clear();
    }

    void clear() {
        cache.clear();
    }

    static String toStringWithoutRecursion(Set<Object> peers) {
        var toString = new StringBuilder();
        toString.append("[");
        for (Object object : peers) {
            if (toString.length() > 1) {
                toString.append(", ");
            }
            if (object instanceof StateImpl) {
                StateImpl state = (StateImpl) object;
                toString.append("state:" + state.item.toString());
            } else if (object instanceof ItemImpl) {
                ItemImpl item = (ItemImpl) object;
                toString.append("item:" + item.guid.name());
            } else if (object instanceof ItemGuid) {
                toString.append(object.toString());
            } else {
                toString.append(QualifiedString.of(object).toString());
            }
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

    public State state(String domain, Object item) {
        return state(domain, QualifiedItem.of(item));
    }

    @SuppressWarnings("unchecked")
    public <T extends State> T state(String domain, T state) {
        return (T) state(domain, QualifiedItem.of(state));
    }

    private State state(String domain, QualifiedItem item) {
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
        } else if (item.value() instanceof StateImpl) {
            var state = (StateImpl) item.value();
            var stateMap = stateMap(domain, state.item);
            String key = item.name().toLowerCase();
            var existing = stateMap.get(key);
            if (existing == null) {
                stateMap.put(key, state);
                return state;
            } else if (!existing.equals(item.value())) {
                throw new IllegalArgumentException("States cannot be replaced: " + state + " -> " + existing);
            } else {
                return existing;
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

    public static boolean hasAllAttributes(Set<Object> available, List<Object> desired) {
        Predicate<? super QualifiedString> predicate = attribute -> available.stream().map(StateMaps::stripState)
                .anyMatch(attribute::equals);
        return desired.stream().map(StateMaps::stripState).filter(predicate).count() == desired.size();
    }

    public static List<Object> flatten(Object[] peers) {
        return flatten(Arrays.asList(peers));
    }

    public static List<Object> flatten(List<Object> peers) {
        List<Object> flattenedPeers = new ArrayList<>(peers.size());
        for (Object peer : peers) {
            if (peer instanceof Items) {
                var items = (Items) peer;
                flattenedPeers.addAll(items.firstOfEachKind());
            } else if (peer instanceof Collection) {
                Collection<?> collection = (Collection<?>) peer;
                flattenedPeers.addAll(collection);
            } else if (peer instanceof Object[]) {
                List<Object> list = Arrays.asList(peer);
                flattenedPeers.addAll(list);
            } else {
                flattenedPeers.add(peer);
            }
        }
        return flattenedPeers;
    }

    private static QualifiedString stripState(Object value) {
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

    StateMap stateMap(String domain, QualifiedItem item) {
        return stateMap(domain.toLowerCase(), item.namespace().toLowerCase());
    }

    private StateMap stateMap(String domain, String namespaceKey) {
        StateMapCache domainCache = getDomainCache(domain);
        final StateMap stateMap;
        if (domainCache.containsKey(namespaceKey)) {
            stateMap = domainCache.get(namespaceKey);
        } else {
            stateMap = new StateMap(domain);
            domainCache.put(namespaceKey, stateMap);
        }
        return stateMap;
    }

    private StateMapCache getDomainCache(String domainKey) {
        final StateMapCache domainCache;
        if (cache.containsKey(domainKey)) {
            domainCache = cache.get(domainKey);
        } else {
            domainCache = new StateMapCache();
            cache.put(domainKey, domainCache);
        }
        return domainCache;
    }
}
