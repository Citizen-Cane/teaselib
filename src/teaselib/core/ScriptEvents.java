package teaselib.core;

import teaselib.State;
import teaselib.core.events.Event;
import teaselib.core.events.EventSource;
import teaselib.util.Item;

public class ScriptEvents {
    public final EventSource<ScriptEventArgs> beforeChoices = new EventSource<>("Before Choices");
    public final EventSource<ScriptEventArgs> afterChoices = new EventSource<>("After Choices");
    public final EventSource<StateChangedEventArgs> stateApplied = new EventSource<>("State applied");
    public final EventSource<StateChangedEventArgs> stateRemoved = new EventSource<>("State removed");
    public final EventSource<ItemChangedEventArgs> itemApplied = new EventSource<>("Item applied");
    public final EventSource<ItemChangedEventArgs> itemRemoved = new EventSource<>("Item removed");

    public ScriptEventSource when(Item item) {
        return new ScriptEventSource(item);
    }

    public class ScriptEventSource {
        final Item item;

        private ScriptEventSource(Item item) {
            this.item = item;
        }

        public ScriptEventTarget applied() {
            return new ScriptEventTarget(this, itemApplied);
        }

        public ScriptEventTarget removed() {
            // TODO attach to apply/remove handler
            return new ScriptEventTarget(this, itemRemoved);
        }
    }

    public class ScriptEventTarget {
        final ScriptEventSource source;
        final EventSource<ItemChangedEventArgs> eventSource;

        public ScriptEventTarget(ScriptEventSource source, EventSource<ItemChangedEventArgs> eventSource) {
            this.source = source;
            this.eventSource = eventSource;
        }

        public ScriptEventAction then(Runnable action) {
            return new ScriptEventAction(source, this, action);
        }

        public ScriptEventAction thenOnce(Runnable action) {
            return new ScriptEventAction(source, this, action);
        }
    }

    public static class StateChangedEventArgs extends ScriptEventArgs {
        final State state;

        public StateChangedEventArgs(State state) {
            this.state = state;
        }
    }

    public static class ItemChangedEventArgs extends ScriptEventArgs {
        final Item item;

        public ItemChangedEventArgs(Item item) {
            this.item = item;
        }
    }

    public class ScriptEventAction implements Event<ItemChangedEventArgs> {
        final ScriptEventSource source;
        final ScriptEventTarget target;
        final Runnable action;

        public ScriptEventAction(ScriptEventSource source, ScriptEventTarget target, Runnable action) {
            this.source = source;
            this.target = target;
            this.action = action;

            target.eventSource.add(this);
        }

        public void run() {
            action.run();
        }

        @Override
        public void run(ItemChangedEventArgs eventArgs) throws Exception {
            if (source.item.equals(eventArgs.item)) {
                try {
                    action.run();
                } finally {
                    target.eventSource.remove(this);
                }
            }
        }
    }
}