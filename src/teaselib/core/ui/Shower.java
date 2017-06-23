package teaselib.core.ui;

import java.util.Stack;
import java.util.concurrent.locks.ReentrantLock;

import teaselib.core.Host;
import teaselib.core.ScriptInterruptedException;
import teaselib.core.TeaseScriptBase;

public class Shower {
    static final int PAUSED = -1;

    final Host host;
    final Stack<Prompt> stack = new Stack<Prompt>();
    private final ReentrantLock lock = new ReentrantLock();

    private final PromptPipeline promptPipeline;

    public Shower(Host host) {
        this.host = host;
        this.promptPipeline = new PromptPipeline(host);
    }

    public String show(TeaseScriptBase script, Prompt prompt) {
        pauseCurrent();

        try {
            return showNew(script, prompt);
        } finally {
            resumePrevious();

        }
    }

    private String showNew(TeaseScriptBase script, Prompt prompt) {
        // acquireLock();

        try {
            stack.push(prompt);
            prompt.executeScriptTask(script, promptPipeline);
            while (true) {
                if (stack.peek() == prompt) {
                    // TODO SR
                    int resultIndex = promptPipeline.show(prompt);
                    if (resultIndex == Prompt.PAUSED) {
                        prompt.pauseUntilResumed();
                        // if (prompt.pauseRequested()) {
                        // // releaseLock();
                        // prompt.pauseUntilResumed();
                        // // acquireLock();
                        // }
                    } else if (resultIndex == Prompt.DISMISSED) {
                        if (prompt.scriptTask != null) {
                            prompt.scriptTask.join();
                            prompt.forwardErrorsAsRuntimeException();
                        }
                        String choice = prompt.choice(resultIndex);
                        return choice;
                    } else {
                        prompt.completeScriptTask();

                        return prompt.choice(resultIndex);
                    }
                } else {
                    prompt.pauseUntilResumed();
                }
            }
        } finally {
            // if (lock.isHeldByCurrentThread()) {
            // releaseLock();
            // } else {
            // throw new IllegalStateException("Lock not hold by " + prompt);
            // }
        }
    }

    private void acquireLockHard() {
        if (!lock.tryLock()) {
            throw new IllegalStateException("Failed to acquire lock");
        }
    }

    private void acquireLock() {
        try {
            lock.lockInterruptibly();
        } catch (InterruptedException e) {
            throw new ScriptInterruptedException();
        }
    }

    private void releaseLock() {
        lock.unlock();
    }

    private void pauseCurrent() {
        if (!stack.empty()) {
            pause(stack.peek());
        }
    }

    private void pause(Prompt prompt) {
        prompt.enterPause();
        // TODO useless - remove
        synchronized (prompt.lock) {
            // while (!prompt.pausing()) {
            promptPipeline.dismiss(prompt);
        }

        // // blocks because script task won't finish
        // try {
        // while (!prompt.pausing()) {
        // Thread.sleep(100);
        // }
        // } catch (InterruptedException e) {
        // throw new ScriptInterruptedException();
        // }
        //
        // if (!prompt.pauseRequested()) {
        // throw new IllegalStateException("Stack element " + stack.peek() + "
        // not paused");
        // }
        //
        // // throws because not in pause state
        // if (!prompt.pausing()) {
        // throw new IllegalStateException("Stack element " + stack.peek() + "
        // not waiting on lock");
        // }
    }

    private void resumePrevious() {
        if (!stack.isEmpty()) {
            Prompt prompt = stack.peek();
            synchronized (prompt) {
                if (promptPipeline.getActive() == prompt) {
                    promptPipeline.dismiss(prompt);
                }
                stack.pop();
            }
        } else {
            throw new IllegalStateException("Prompt stack empty");
        }

        if (!stack.isEmpty()) {
            Prompt prompt = stack.peek();
            prompt.resume();
        }
    }
}
