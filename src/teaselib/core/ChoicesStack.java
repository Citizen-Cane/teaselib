package teaselib.core;

import java.util.Iterator;
import java.util.Map;
import java.util.Stack;

public class ChoicesStack implements Iterable<ShowChoices> {

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
            Map<String, Runnable> pauseHandlers) {
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
                script.endAll();
            }
            synchronized (this) {
                if (pauseHandlers.containsKey(choice)) {
                    script.teaseLib.log
                            .info("Invoking choices handler for choices="
                                    + showChoices.derivedChoices.toString()
                                    + " reason=" + choice.toString());
                    pauseHandlers.get(choice).run();
                    continue;
                } else {
                    pop();
                    if (!empty()) {
                        notifyAll();
                    }
                    break;
                }
            }
        }
        return choice;
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
