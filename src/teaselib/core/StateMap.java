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

    public State get(Object item) {
        if (states.containsKey(item)) {
            return states.get(item);
        } else {
            return null;
        }
    }

    public void put(Object item, State state) {
        states.put(item, state);
    }

    void clear() {
        states.clear();
    }

    public boolean contains(Object item) {
        return states.containsKey(item);
    }
}