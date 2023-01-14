/**
 * 
 */
package teaselib.core.state;

import java.util.concurrent.TimeUnit;

import teaselib.Duration;
import teaselib.State;
import teaselib.State.Persistence;
import teaselib.core.ItemLogger;
import teaselib.core.StateImpl;

public class StateOptionsProxy extends AbstractProxy<State.Options> implements State.Options {

    private final StateImpl stateImpl;
    private final ItemLogger itemLogger;

    public StateOptionsProxy(StateImpl item, String namespace, State.Options options, ItemLogger itemLogger) {
        super(namespace, options);
        this.stateImpl = item;
        this.itemLogger = itemLogger;
    }

    @Override
    public void remember(Until forget) {
        state.remember(forget);
        itemLogger.log(stateImpl, "remember", forget);
    }

    @Override
    public Persistence over(long duration, TimeUnit unit) {
        var persistence = new StatePersistenceProxy(namespace, state.over(duration, unit));
        itemLogger.log(stateImpl, "over", duration, unit);
        return persistence;
    }

    @Override
    public Persistence over(Duration duration) {
        var persistence = new StatePersistenceProxy(namespace, state.over(duration));
        itemLogger.log(stateImpl, "over", duration);
        return persistence;
    }

}
