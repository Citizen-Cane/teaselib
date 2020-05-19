package teaselib.core;

import teaselib.State;
import teaselib.core.events.Event;
import teaselib.core.events.EventSource;
import teaselib.util.Item;
import teaselib.util.Items;

public class ScriptEvents {
    public final EventSource<ScriptEventArgs> beforeMessage = new EventSource<>("Before Message");
    public final EventSource<ScriptEventArgs> beforeChoices = new EventSource<>("Before Choices");
    public final EventSource<ScriptEventArgs> afterChoices = new EventSource<>("After Choices");
    public final EventSource<ScriptEventArgs.ActorChanged> actorChanged = new EventSource<>("Actor changed");

    public final EventSource<StateChangedEventArgs> stateApplied = new EventSource<>("State applied");
    public final EventSource<StateChangedEventArgs> stateRemoved = new EventSource<>("State removed");
    public final EventSource<ItemChangedEventArgs> itemApplied = new EventSource<>("Item applied");
    public final EventSource<ItemChangedEventArgs> itemDuration = new EventSource<>("Item duration");
    public final EventSource<ItemChangedEventArgs> itemRemoved = new EventSource<>("Item removed");

    public final ScriptEventInputMethod scriptEventInputMethod;

    public ScriptEvents(ScriptEventInputMethod scriptEventInputMethod) {
        this.scriptEventInputMethod = scriptEventInputMethod;
    }

    public ItemEventSource when(Item item) {
        return new ItemEventSource(new Items(item));
    }

    public ScriptEventSource when() {
        return new ScriptEventSource();
    }

    /**
     * Inject a runnable sub-script into the current script flow.
     * <p>
     * The injected action will be executed once either after the current message, or pause the active prompt. Signaling
     * is a non-persistent operation, if the action is invoked via the prompt, then the action will automatically be
     * removed from the event source as well. Otherwise the action will be invoked once by the event source.
     * <p>
     * If the current prompt runs a script function, no handler is invoked, and the action will be invoked when the
     * script function progresses.
     * 
     * @param action
     *            A script fragment to be executed as soon as possible
     */
    public void interjectScriptFragment(Runnable action) {
        ScriptEventAction scriptEventAction = when().beforeMessage().thenOnce(action);
        scriptEventInputMethod.signalEvent(() -> scriptEventAction.run(new ScriptEventArgs()));
    }

    public class ScriptEventSource {
        public ScriptEventTarget beforeMessage() {
            return new ScriptEventTarget(this, beforeMessage);
        }
    }

    public static class ScriptEventTarget {
        final ScriptEventSource source;
        final EventSource<ScriptEventArgs> eventSource;

        public ScriptEventTarget(ScriptEventSource source, EventSource<ScriptEventArgs> eventSource) {
            this.source = source;
            this.eventSource = eventSource;
        }

        public ScriptEventAction thenOnce(Runnable action) {
            return new ScriptEventAction(source, this, action);
        }
    }

    public static class ScriptEventAction implements Event<ScriptEventArgs> {
        final ScriptEventSource source;
        final ScriptEventTarget target;
        final Runnable action;
        boolean inProgress = false;

        public ScriptEventAction(ScriptEventSource source, ScriptEventTarget target, Runnable action) {
            this.source = source;
            this.target = target;
            this.action = action;

            target.eventSource.add(this);
        }

        @Override
        public void run(ScriptEventArgs eventArgs) {
            if (!inProgress) {
                target.eventSource.remove(this);
                // Avoid being called again when actions themselves remove items
                inProgress = true;
                try {
                    action.run();
                } finally {
                    inProgress = false;
                }
            }
        }
    }

    /**
     * Trigger event when any of the items changes its state
     * 
     * @param items
     * @return
     */
    public ItemEventSource when(Items items) {
        return new ItemEventSource(items);
    }

    public class ItemEventSource {
        final Items items;

        ItemEventSource(Items items) {
            this.items = items;
        }

        public ItemEventTarget applied() {
            return new ItemEventTarget(this, itemApplied);
        }

        public ItemEventTarget duration() {
            return new ItemEventTarget(this, itemDuration);
        }

        public ItemEventTarget removed() {
            return new ItemEventTarget(this, itemRemoved);
        }
    }

    public static class ItemEventTarget {
        final ItemEventSource source;
        final EventSource<ItemChangedEventArgs> eventSource;

        public ItemEventTarget(ItemEventSource source, EventSource<ItemChangedEventArgs> eventSource) {
            this.source = source;
            this.eventSource = eventSource;
        }

        public ItemEventAction thenOnce(Runnable action) {
            return new ItemEventAction(source, this, action);
        }
    }

    public static class StateChangedEventArgs extends ScriptEventArgs {
        final State state;

        public StateChangedEventArgs(State state) {
            this.state = state;
        }
    }

    public static class ItemChangedEventArgs extends ScriptEventArgs {
        public final Item item;

        public ItemChangedEventArgs(Item item) {
            this.item = item;
        }
    }

    public static class ItemEventAction implements Event<ItemChangedEventArgs> {
        final ItemEventSource source;
        final ItemEventTarget target;
        final Runnable action;
        boolean inProgress = false;

        public ItemEventAction(ItemEventSource source, ItemEventTarget target, Runnable action) {
            this.source = source;
            this.target = target;
            this.action = action;

            target.eventSource.add(this);
        }

        public Items items() {
            return source.items;
        }

        @Override
        public void run(ItemChangedEventArgs eventArgs) {
            if (!inProgress && source.items.contains(eventArgs.item)) {
                target.eventSource.remove(this);
                // Avoid being called again when actions themselves remove items
                inProgress = true;
                try {
                    action.run();
                } finally {
                    inProgress = false;
                }
            }
        }
    }

}
