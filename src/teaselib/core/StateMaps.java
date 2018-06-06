package teaselib.core;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Set;

import teaselib.State;
import teaselib.core.state.StateProxy;
import teaselib.core.util.Persist;
import teaselib.core.util.QualifiedItem;
import teaselib.util.ItemImpl;

public class StateMaps {
    final TeaseLib teaseLib;

    public interface Attributes {
        void applyAttributes(Object... attributes);
    }

    class StateMapCache extends HashMap<String, StateMap> {
        private static final long serialVersionUID = 1L;
    }

    class Domains extends HashMap<String, StateMapCache> {
        private static final long serialVersionUID = 1L;
    }

    final Domains cache = new Domains();

    public StateMaps(TeaseLib teaseLib) {
        this.teaseLib = teaseLib;
    }

    void clear() {
        cache.clear();
    }

    static String toStringWithoutRecursion(Set<Object> peers) {
        StringBuilder toString = new StringBuilder();
        for (Object object : peers) {
            if (toString.length() == 0) {
                toString.append("[");
            } else {
                toString.append(", ");
            }
            if (object instanceof StateImpl) {
                StateImpl state = (StateImpl) object;
                toString.append("state:" + state.item.toString());
            } else if (object instanceof ItemImpl) {
                ItemImpl item = (ItemImpl) object;
                toString.append("item:" + item.item.toString());
            } else {
                toString.append(QualifiedItem.of(object).toString());
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
        if (Persist.isPersistedString(item.toString())) {
            State state = Persist.from(item.toString(), clazz -> teaseLib);
            item = QualifiedItem.of(((StateImpl) state).item);
            StateMap stateMap = stateMap(domain, item);
            if (!stateMap.contains(item.toString().toLowerCase())) {
                stateMap.put(item.toString().toLowerCase(), state);
            }
            return state;
        } else if (item.value() instanceof StateImpl) {
            StateImpl stateImpl = (StateImpl) item.value();
            StateMap stateMap = stateMap(domain, QualifiedItem.of(stateImpl.item));
            String key = item.name().toLowerCase();
            State existing = stateMap.get(key);
            if (existing == null) {
                State state = (State) item.value();
                stateMap.put(key, state);
                return state;
            } else if (!existing.equals(item.value())) {
                throw new IllegalArgumentException(
                        "States cannot be replaced: " + stateImpl.toString() + " -> " + existing.toString());
            } else {
                return stateImpl;
            }
        } else {
            StateMap stateMap = stateMap(domain, item);
            String key = item.name().toLowerCase();
            State state = stateMap.get(key);
            if (state == null) {
                state = new StateImpl(this, domain, item.value());
                stateMap.put(key, state);
            }
            return state;
        }
    }

    public static boolean hasAllAttributes(Set<Object> availableAttributes, Object[] desiredAttributes) {
        return Arrays.stream(desiredAttributes).map(desiredAttribute -> QualifiedItem.of(stripState(desiredAttribute)))
                .filter(desiredQualifiedAttribute -> availableAttributes.stream()
                        .anyMatch(desiredQualifiedAttribute::equals))
                .count() == desiredAttributes.length;
    }

    private static Object stripState(Object value) {
        if (value instanceof StateImpl) {
            return stripState((StateImpl) value);
        } else if (value instanceof StateProxy) {
            return stripState(((StateProxy) value).state);
        } else {
            return value;
        }
    }

    private static Object stripState(StateImpl state) {
        return state.item;
    }

    private StateMap stateMap(String domain, QualifiedItem item) {
        return stateMap(domain, item.namespace().toLowerCase());
    }

    StateMap stateMap(String domain, String namespaceKey) {
        domain=domain.toLowerCase();
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
