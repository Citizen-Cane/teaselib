/**
 * 
 */
package teaselib.core.state;

import java.util.concurrent.TimeUnit;

import teaselib.Duration;
import teaselib.State;
import teaselib.State.Persistence;
import teaselib.core.ScriptEvents;

public class StateOptionsProxy extends AbstractProxy<State.Options> implements State.Options {
    final ScriptEvents events;

    public StateOptionsProxy(String namespace, State.Options options, ScriptEvents events) {
        super(namespace, options);
        this.events = events;
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