package teaselib.core;

import java.util.Iterator;
import java.util.Map;
import java.util.Stack;

import org.slf4j.LoggerFactory;

public class ChoicesStack implements Iterable<ShowChoices> {
    private static final org.slf4j.Logger logger = LoggerFactory
            .getLogger(ChoicesStack.class);

    private final Object showChoicesSyncObject = new Object();
    private final Stack<ShowChoices> stack = new Stack<ShowChoices>();

    @Override
    public Iterator<ShowChoices> iterator() {
        return stack.iterator();
    }

    public boolean empty() {
        return stack.empty();
    }

    public ShowChoices peek() {
        return stack.peek();
    }

    public ShowChoices pop() {
        return stack.pop();
    }

    public ShowChoices push(ShowChoices showChoices) {
        return stack.push(showChoices);
    }

    public String show(TeaseScriptBase script, ShowChoices showChoices,
            Map<String, PauseHandler> pauseHandlers) {
        String choice = null;
        ShowChoices previous = null;
        synchronized (this) {
            if (!empty()) {
                previous = peek();
            }
            push(showChoices);
            if (previous != null) {
                previous.pause(ShowChoices.Paused);
            }
        }
        while (true) {
            // Ensure only one thread at a time can realize ui elements
            synchronized (showChoicesSyncObject) {
                choice = showChoices.show();
                // End the current set of renderers
                // in order to dismiss the message
                // This also affects script functions,
                // as well as {@link Message#ShowChoices}
                if (pauseHandlers.containsKey(choice)) {
                    if (pauseHandlers.get(choice).endRenderers()) {
                        script.endAll();
                    }
                } else {
                    // Dismiss the current set of renderers
                    script.endAll();
                }
                // Now this instance can exit or enter the pause state,
                // while the next instance can show its choices
            }
            synchronized (this) {
                if (pauseHandlers.containsKey(choice)) {
                    logger.info("Invoking choices handler for choices="
                            + showChoices.derivedChoices.toString() + " reason="
                            + choice.toString());
                    // execute the pause handler
                    pauseHandlers.get(choice).run();
                    // restore us by showing our choices again
                    continue;
                } else {
                    removeCurrentChoices();
                    break;
                }
            }
        }
        return choice;
    }

    private void removeCurrentChoices() {
        pop();
        if (!empty()) {
            notifyAll();
        }
    }

    public boolean containsPauseState(String pauseState) {
        for (ShowChoices choice : this) {
            if (choice.getPauseState() == pauseState) {
                return true;
            }
        }
        return false;
    }
}
