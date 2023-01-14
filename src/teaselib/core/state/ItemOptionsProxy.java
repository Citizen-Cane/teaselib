/**
 * 
 */
package teaselib.core.state;

import java.util.concurrent.TimeUnit;

import teaselib.Duration;
import teaselib.State;
import teaselib.State.Persistence;
import teaselib.core.ItemImpl;
import teaselib.core.ItemLogger;
import teaselib.core.ScriptEvents;
import teaselib.core.ScriptEvents.ItemChangedEventArgs;
import teaselib.util.Item;

public class ItemOptionsProxy extends AbstractProxy<Item.Options> implements State.Options {

    private final ItemImpl itemImpl;
    private final ScriptEvents events;
    private final ItemLogger itemLogger;

    public ItemOptionsProxy(ItemImpl itemImpl, String namespace, State.Options options, ScriptEvents events, ItemLogger itemLogger) {
        super(namespace, options);
        this.itemImpl = itemImpl;
        this.events = events;
        this.itemLogger = itemLogger;
    }

    @Override
    public void remember(Until forget) {
        state.remember(forget);
        itemLogger.log(itemImpl, "remember", forget);
        events.itemRemember.fire(new ItemChangedEventArgs(itemImpl));
    }

    @Override
    public Persistence over(long duration, TimeUnit unit) {
        var persistence = new StatePersistenceProxy(namespace, state.over(duration, unit));
        itemLogger.log(itemImpl, "over", duration, unit);
        return persistence;
    }

    @Override
    public Persistence over(Duration duration) {
        var persistence = new StatePersistenceProxy(namespace, state.over(duration));
        itemLogger.log(itemImpl, "over", duration);
        return persistence;
    }

}
