package teaselib.core.ui;

/**
 * @author Citizen-Cane
 *
 */
public interface InputMethod extends teaselib.core.Closeable {

    public interface Setup {
        void apply();
    }

    public interface Notification {
        // tag interface
    }

    public class UiEvent {
        public final boolean enabled;

        public UiEvent(boolean enabled) {
            this.enabled = enabled;
        }
    }

    Setup getSetup(Choices choices);

    void show(Prompt prompt) throws InterruptedException;

    void updateUI(UiEvent event);

    void dismiss(Prompt prompt) throws InterruptedException;

    public interface Listener {

        Prompt.Result promptShown(Prompt prompt);

        void promptDismissed(Prompt prompt);
    }

}