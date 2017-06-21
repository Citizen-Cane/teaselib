package teaselib.core.ui;

import java.util.Stack;
import java.util.concurrent.locks.ReentrantLock;

import teaselib.core.Host;
import teaselib.core.ScriptInterruptedException;

public class Shower {
    static final int PAUSED = -1;

    final Host host;
    final Stack<Prompt> stack = new Stack<Prompt>();

    public Shower(Host host) {
        this.host = host;
    }

    public String show(Prompt prompt) {
        pauseCurrent();

        try {
            return showNew(prompt);
        } finally {
            resumePrevious();
        }
    }

    private ReentrantLock lock = new ReentrantLock();

    private String showNew(Prompt prompt) {
        acquireLock();

        try {
            stack.push(prompt);
            prompt.executeScriptTask();
            while (true) {
                if (stack.peek() == prompt) {
                    // TODO SR
                    int resultIndex = host.reply(prompt.choices);
                    if (prompt.pauseRequested()) {
                        lock.unlock();
                        prompt.pauseUntilResumed();
                        acquireLock();

                        if (prompt.scriptTask != null) {
                            if (prompt.scriptTask.isDone() || prompt.scriptTask.isCancelled()) {
                                prompt.scriptTask.join();
                                prompt.forwardErrorsAsRuntimeException();
                                return prompt.scriptTask.getScriptFunctionResult();
                            }
                        }
                    } else {
                        prompt.completeScriptTask();

                        return prompt.choice(resultIndex);
                    }
                } else {
                    prompt.pauseUntilResumed();
                }
            }
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    private void acquireLock() {
        if (!lock.tryLock()) {
            throw new IllegalStateException("Failed to acquire lock");
        }
    }

    private void pauseCurrent() {
        if (!stack.empty()) {
            pause(stack.peek());
        }
    }

    private void pause(Prompt prompt) {
        prompt.enterPause();
        synchronized (prompt.lock) {
            while (!prompt.pausing()) {
                host.dismissChoices(prompt.choices);
                try {
                    prompt.lock.wait(100);
                } catch (InterruptedException e) {
                    throw new ScriptInterruptedException();
                }
            }
        }

        if (!prompt.pauseRequested()) {
            throw new IllegalStateException("Stack element " + stack.peek() + " not paused");
        }

        if (!prompt.pausing()) {
            throw new IllegalStateException("Stack element " + stack.peek() + " not waiting on lock");
        }
    }

    private void resumePrevious() {
        stack.pop();

        if (!stack.isEmpty()) {
            stack.peek().resume();
        }
    }
}
