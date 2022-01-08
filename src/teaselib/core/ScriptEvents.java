package teaselib.core;

import java.util.function.Consumer;

import teaselib.State;
import teaselib.core.ScriptEventArgs.BeforeMessage;
import teaselib.core.ScriptEvents.ScriptEventAction.Run;
import teaselib.core.events.Event;
import teaselib.core.events.EventSource;
import teaselib.util.Item;
import teaselib.util.Items;

public class ScriptEvents {

    public final EventSource<ScriptEventArgs.BeforeMessage> beforeMessage = new EventSource<>("Before Message");
    public final EventSource<ScriptEventArgs> beforePrompt = new EventSource<>("Before Prompt");
    public final EventSource<ScriptEventArgs> afterPrompt = new EventSource<>("After Prompt");
    public final EventSource<ScriptEventArgs.ActorChanged> actorChanged = new EventSource<>("Actor changed");

    public final EventSource<StateChangedEventArgs> stateApplied = new EventSource<>("State applied");
    public final EventSource<StateChangedEventArgs> stateRemoved = new EventSource<>("State removed");
    public final EventSource<ItemChangedEventArgs> itemApplied = new EventSource<>("Item applied");
    public final EventSource<ItemChangedEventArgs> itemDuration = new EventSource<>("Item duration");
    public final EventSource<ItemChangedEventArgs> itemRemember = new EventSource<>("Item remember");
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
        ScriptEventAction<BeforeMessage> scriptEventAction = when().beforeMessage().thenOnce(action);
        scriptEventInputMethod.signalEvent(
                () -> scriptEventAction.run(new ScriptEventArgs.BeforeMessage(BeforeMessage.OutlineType.NewSection)));
    }

    public class ScriptEventSource {
        public ScriptEventTarget<ScriptEventArgs.BeforeMessage> beforeMessage() {
            return new ScriptEventTarget<>(beforeMessage);
        }

        public ScriptEventTarget<ScriptEventArgs> beforePrompt() {
            return new ScriptEventTarget<>(beforePrompt);
        }

        public ScriptEventTarget<ScriptEventArgs> afterChoices() {
            return new ScriptEventTarget<>(afterPrompt);
        }

        public ScriptEventTarget<ScriptEventArgs.ActorChanged> actorChanged() {
            return new ScriptEventTarget<>(actorChanged);
        }
    }

    public static class ScriptEventTarget<E extends ScriptEventArgs> {
        final EventSource<E> eventSource;

        public ScriptEventTarget(EventSource<E> eventSource) {
            this.eventSource = eventSource;
        }

        public ScriptEventAction<E> thenOnce(Runnable action) {
            return new ScriptEventAction<>(this, action, Run.ONCE);
        }

        public ScriptEventAction<E> thenOnce(Consumer<E> action) {
            return new ScriptEventAction<>(this, action, Run.ONCE);
        }

        public ScriptEventAction<E> then(Runnable action) {
            return new ScriptEventAction<>(this, action, Run.FOREVER);
        }

        public ScriptEventAction<E> then(Consumer<E> action) {
            return new ScriptEventAction<>(this, action, Run.FOREVER);
        }
    }

    public static class ScriptEventAction<E extends ScriptEventArgs> implements Event<E> {
        private final ScriptEventTarget<E> target;
        private final Consumer<E> action;
        private final Run until;

        private boolean inProgress = false;

        enum Run {
            ONCE,
            FOREVER
        }

        public ScriptEventAction(ScriptEventTarget<E> target, Runnable action, Run until) {
            this(target, e -> action.run(), until);
        }

        public ScriptEventAction(ScriptEventTarget<E> target, Consumer<E> action, Run until) {
            this.target = target;
            this.action = action;
            this.until = until;
            target.eventSource.add(this);
        }

        @Override
        public void run(E eventArgs) {
            if (!inProgress) {
                // Avoid being called again when actions themselves remove items
                inProgress = true;

                if (until == Run.ONCE) {
                    target.eventSource.remove(this);
                }
                try {
                    action.accept(eventArgs);
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

        public ItemEventTarget remember() {
            return new ItemEventTarget(this, itemRemember);
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
