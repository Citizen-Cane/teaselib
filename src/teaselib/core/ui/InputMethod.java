package teaselib.core.ui;

/**
 * @author Citizen-Cane
 *
 */
public interface InputMethod {

    public interface Setup {
        void apply();
    }

    public interface Notification {
        // tag interface
    }

    Setup getSetup(Choices choices);

    void show(Prompt prompt) throws InterruptedException;

    void dismiss(Prompt prompt) throws InterruptedException;

    public interface Listener {
        Prompt.Result promptShown(Prompt prompt);

        void promptDismissed(Prompt prompt);
    }
}