package teaselib.core.ui;

import java.util.Map;

/**
 * @author Citizen-Cane
 *
 */
public interface InputMethod {

    void show(Prompt prompt) throws InterruptedException;

    boolean dismiss(Prompt prompt) throws InterruptedException;

    Map<String, Runnable> getHandlers();

    public interface Listener {
        int promptShown(Prompt prompt);

        void promptDismissed(Prompt prompt);
    }
}