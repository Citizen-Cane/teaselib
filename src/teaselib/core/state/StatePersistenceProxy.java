package teaselib.core.state;

import teaselib.State;
import teaselib.State.Persistence;

public class StatePersistenceProxy extends AbstractProxy<State.Persistence> implements State.Persistence {

    public StatePersistenceProxy(String namespace, Persistence persistence) {
        super(namespace, persistence);
    }

    @Override
    public void remember() {
        state.remember();
    }
}