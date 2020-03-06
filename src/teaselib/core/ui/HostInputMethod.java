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
        Prompt.Result reply(Choices choices);

        boolean dismissChoices(List<Choice> choices);
    }

    private final Backend host;

    public HostInputMethod(ExecutorService executorService, Backend host) {
        super(executorService);
        this.host = host;
    }

    @Override
    public Setup getSetup(Choices choices) {
        return Unused;
    }

    @Override
    protected Prompt.Result handleShow(Prompt prompt) throws InterruptedException, ExecutionException {
        return host.reply(prompt.choices);
    }

    @Override
    protected boolean handleDismiss(Prompt prompt) throws InterruptedException {
        return host.dismissChoices(prompt.choices);
    }

    @Override
    public String toString() {
        return host.toString();
    }
}
