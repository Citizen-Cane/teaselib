/**
 * 
 */
package teaselib.core.state;

import java.util.concurrent.TimeUnit;

import teaselib.Duration;
import teaselib.State;
import teaselib.core.ScriptEvents;

public class StateOptionsProxy extends AbstractProxy<State.Options> implements State.Options {
    final ScriptEvents events;

    public StateOptionsProxy(String namespace, State.Options options, ScriptEvents events) {
        super(namespace, options);
        this.events = events;
    }

    @Override
    public State over(long duration, TimeUnit unit) {
        return new StateProxy(namespace, state.over(duration, unit), events);
    }

    @Override
    public State over(Duration duration) {
        return new StateProxy(namespace, state.over(duration), events);
    }
}