package teaselib.core;

import java.util.HashMap;
import java.util.Map;

import teaselib.core.util.QualifiedString;

public class StateMap {

    private final String domain;
    final Map<QualifiedString, StateImpl> states = new HashMap<>();

    public StateMap(String domain) {
        this.domain = domain;
    }

    public StateImpl get(QualifiedString key) {
        return states.getOrDefault(key, null);
    }

    public void put(QualifiedString key, StateImpl state) {
        states.put(key, state);
    }

    void clear() {
        states.clear();
    }

    public boolean contains(QualifiedString key) {
        return states.containsKey(key);
    }

    @Override
    public String toString() {
        return (domain != TeaseLib.DefaultDomain ? ("domain= + " + domain) : "") + states;
    }

}
