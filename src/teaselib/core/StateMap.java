package teaselib.core;

import java.util.HashMap;
import java.util.Map;

import teaselib.State;

public class StateMap {
    final String domain;
    final Map<Object, State> states = new HashMap<>();

    public StateMap(String domain) {
        this.domain = domain;
    }

    public State get(String item) {
        return states.getOrDefault(item.toLowerCase(), null);
    }

    public void put(String item, State state) {
        states.put(item, state);
    }

    void clear() {
        states.clear();
    }

    public boolean contains(String item) {
        return states.containsKey(item);
    }

    @Override
    public String toString() {
        return (domain != TeaseLib.DefaultDomain ? ("domain= + " + domain) : "") + states.toString();
    }

}