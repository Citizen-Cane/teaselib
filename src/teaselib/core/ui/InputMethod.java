package teaselib.core.ui;

import java.util.Map;

/**
 * @author Citizen-Cane
 *
 */
public interface InputMethod {

    public interface Setup {
        void apply();
    }

    Setup getSetup(Choices choices);

    void show(Prompt prompt) throws InterruptedException;

    boolean dismiss(Prompt prompt) throws InterruptedException;

    Map<String, Runnable> getHandlers();

    public interface Listener {
        Prompt.Result promptShown(Prompt prompt);

        void promptDismissed(Prompt prompt);
    }
}