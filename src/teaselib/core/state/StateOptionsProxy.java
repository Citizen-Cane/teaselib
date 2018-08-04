/**
 * 
 */
package teaselib.core.state;

import java.util.concurrent.TimeUnit;

import teaselib.Duration;
import teaselib.State;

public class StateOptionsProxy extends AbstractProxy<State.Options> implements State.Options {
    public StateOptionsProxy(String namespace, State.Options options) {
        super(namespace, options);
    }

    @Override
    public State over(long duration, TimeUnit unit) {
        return new StateProxy(namespace, state.over(duration, unit));
    }

    @Override
    public State over(Duration duration) {
        return new StateProxy(namespace, state.over(duration));
    }
}