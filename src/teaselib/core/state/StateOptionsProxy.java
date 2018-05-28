/**
 * 
 */
package teaselib.core.state;

import java.util.concurrent.TimeUnit;

import teaselib.Duration;
import teaselib.State;
import teaselib.State.Persistence;

public class StateOptionsProxy extends AbstractProxy<State.Options> implements State.Options {
    public StateOptionsProxy(String namespace, State.Options options) {
        super(namespace, options);
    }

    @Override
    public void remember() {
        state.remember();
    }

    @Override
    public Persistence over(long duration, TimeUnit unit) {
        return new StatePersistenceProxy(namespace, state.over(duration, unit));
    }

    @Override
    public Persistence over(Duration duration) {
        return new StatePersistenceProxy(namespace, state.over(duration));
    }
}