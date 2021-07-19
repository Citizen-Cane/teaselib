package teaselib.core.ui;

import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;

/**
 * @author Citizen-Cane
 *
 */
public class HostInputMethod extends AbstractInputMethod {
    public interface Backend {
        void setup();

        Prompt.Result reply(Choices choices) throws InterruptedException;

        void updateUI(InputMethod.UiEvent event);

        boolean dismissChoices(List<Choice> choices);
    }

    private final Backend host;

    public HostInputMethod(ExecutorService executorService, Backend host) {
        super(executorService);
        this.host = host;
    }

    @Override
    public Setup getSetup(Choices choices) {
        return () -> host.setup();
    }

    @Override
    protected Prompt.Result handleShow(Prompt prompt) throws InterruptedException, ExecutionException {
        return host.reply(prompt.choices);
    }

    @Override
    public void updateUI(UiEvent event) {
        host.updateUI(event);
    }

    @Override
    protected void handleDismiss(Prompt prompt) throws InterruptedException {
        host.dismissChoices(prompt.choices);
    }

    @Override
    public String toString() {
        return host.toString();
    }
}
