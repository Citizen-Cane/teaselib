package teaselib.core.ui;

/**
 * @author Citizen-Cane
 *
 */
public interface InputMethod extends teaselib.core.Closeable {

    /**
     * The setup interface allows to perform time-consuming operations before realizing an input method.
     * 
     * {@link InputMethod#getSetup} is called once when creating a new prompt, whereas {@link Setup#apply} is called
     * each time a prompt is realized. This way the script engine can perform the time-consuming operation together with
     * the paragraph preceding the prompt and the user interface becomes snappier.
     * 
     * @author Citizen-Cane
     *
     */
    public interface Setup {
        static final Setup None = () -> {};

        /**
         * Called by the input method before realizing the input method.
         */
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

        @Override
        public String toString() {
            return "enabled=" + enabled;
        }
    }

    Setup getSetup(Choices choices);

    void show(Prompt prompt);

    void updateUI(UiEvent event);

    void dismiss(Prompt prompt);

    public interface Listener {

        Prompt.Result promptShown(Prompt prompt);

        void promptDismissed(Prompt prompt);
    }

}