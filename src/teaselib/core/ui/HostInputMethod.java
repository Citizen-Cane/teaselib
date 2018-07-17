package teaselib.core.ui;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * @author Citizen-Cane
 *
 */
public class HostInputMethod extends AbstractInputMethod {
    public interface Backend {
        int reply(Choices choices);

        boolean dismissChoices(List<Choice> choices);
    }

    private final Backend host;

    public HostInputMethod(ExecutorService executorService, Backend host) {
        super(executorService);
        this.host = host;
    }

    @Override
    protected int awaitResult(Prompt prompt) throws InterruptedException, ExecutionException {
        return host.reply(prompt.choices);
    }

    @Override
    protected boolean handleDismiss(Prompt prompt) throws InterruptedException {
        boolean dismissChoices = false;

        boolean tryLock = replySection.tryLock();
        while (!tryLock) {
            dismissChoices = host.dismissChoices(prompt.choices);
            tryLock = replySection.tryLock();
            if (tryLock) {
                break;
            }
            prompt.lock.lockInterruptibly();
            try {
                tryLock = prompt.click.await(100, TimeUnit.MILLISECONDS);
            } finally {
                prompt.lock.unlock();
            }
        }

        return dismissChoices;
    }

    @Override
    public Map<String, Runnable> getHandlers() {
        return Collections.emptyMap();
    }

    @Override
    public String toString() {
        return host.toString();
    }
}
