package teaselib.core;

import java.util.HashMap;
import java.util.Map;

import teaselib.State;
import teaselib.core.util.QualifiedString;

public class StateMap {
    final String domain;
    final Map<String, State> states = new HashMap<>();

    public StateMap(String domain) {
        this.domain = domain;
    }

    public State get(QualifiedString key) {
        return get(key.name());
    }

    public State get(String key) {
        return states.getOrDefault(key.toLowerCase(), null);
    }

    public void put(QualifiedString key, State state) {
        put(key.name(), state);
    }

    public void put(String key, State state) {
        states.put(key.toLowerCase(), state);
    }

    void clear() {
        states.clear();
    }

    public boolean contains(QualifiedString key) {
        return contains(key.name());
    }

    public boolean contains(String key) {
        return states.containsKey(key.toLowerCase());
    }

    @Override
    public String toString() {
        return (domain != TeaseLib.DefaultDomain ? ("domain= + " + domain) : "") + states.toString();
    }

}
