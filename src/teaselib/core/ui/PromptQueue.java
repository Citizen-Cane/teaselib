package teaselib.core.ui;

import java.util.concurrent.atomic.AtomicReference;

import teaselib.core.ScriptInterruptedException;
import teaselib.core.TeaseScriptBase;

/**
 * @author Citizen-Cane
 *
 */
public class PromptQueue {
    private final AtomicReference<Prompt> active = new AtomicReference<Prompt>();

    public int show(TeaseScriptBase script, Prompt prompt) throws InterruptedException {
        prompt.lock.lockInterruptibly();
        try {
            // Prevent multiple entry while initializing prompt
            synchronized (this) {
                Prompt activePrompt = active.get();

                if (activePrompt != null && prompt == activePrompt) {
                    throw new IllegalStateException("Prompt " + prompt + " already showing");
                }

                if (activePrompt != null) {
                    // host input method waiting for dismissing "stop"
                    // -> "stop" is never dismissed because active prompt == null -> future is executing
                    // -> Yes/No future is blocked since future "stop" is still running - thread pool size == 1
                    // There is a "Reply [stop]" log entry after TeaseScriptBase prompts "Yes/No"
                    // This suggests that thread "Script Function 1" thread and thread "dummy host" have a concurrency
                    // issue
                    // -> PromptQueue.show() is indeed not protected

                    // TODO
                    // Make sure dummy-host thread "stop" comes before Script-function 1 thread "Yes/No"
                    pause(activePrompt);
                }

                makePromptActive(prompt);

                if (prompt.scriptFunction != null) {
                    prompt.executeScriptTask(script);
                    // script task waits until prompt queue is awaiting the click
                    // - still not good enough - however works with 1000ms delay
                    // TODO wait until all host input methods have realized prompt in makePromptActive
                    // Waiting for the callable to be ready seems to do the trick
                    // -> what about stress tests? -> ?
                }
            }

            try {
                prompt.click.await();
            } finally {
                dismissPrompt(prompt);
            }
            if (prompt.exception != null) {
                if (prompt.exception instanceof RuntimeException) {
                    throw (RuntimeException) prompt.exception;
                } else if (prompt.exception instanceof Error) {
                    throw (Error) prompt.exception;
                } else {
                    throw new RuntimeException(prompt.exception);
                }
            } else {
                return prompt.result();
            }
        } finally {
            prompt.lock.unlock();
        }
    }

    public void resume(Prompt prompt) throws InterruptedException {
        synchronized (this) {
            prompt.lock.lockInterruptibly();
            try {
                Prompt activePrompt = active.get();

                if (activePrompt != null && prompt == activePrompt) {
                    throw new IllegalStateException("Prompt " + prompt + " already showing");
                }

                if (activePrompt != null) {
                    dismiss(activePrompt);
                }

                if (prompt.result() != Prompt.UNDEFINED)
                    return;

                makePromptActive(prompt);
                prompt.paused.set(false);

                // Were're just resuming, no need to wait or cleanup
            } finally {
                prompt.lock.unlock();
            }
        }
    }

    private void makePromptActive(Prompt prompt) throws InterruptedException {
        active.set(prompt);
        for (InputMethod inputMethod : prompt.inputMethods) {
            inputMethod.show(prompt);
        }
    }

    public synchronized boolean dismiss(Prompt prompt) throws InterruptedException {
        prompt.lock.lockInterruptibly();
        try {
            if (prompt.result() == Prompt.UNDEFINED) {
                prompt.setResultOnce(Prompt.DISMISSED);
                return dismissPrompt(prompt);
            } else {
                return false;
            }
        } finally {
            prompt.lock.unlock();
        }
    }

    public synchronized boolean pause(Prompt prompt) throws InterruptedException {
        prompt.lock.lockInterruptibly();
        try {
            prompt.paused.set(true);
            if (prompt.result() == Prompt.UNDEFINED) {
                return dismissPrompt(prompt);
            } else {
                return false;
            }
        } finally {
            prompt.lock.unlock();
        }
    }

    private synchronized boolean dismissPrompt(Prompt prompt) {
        // TODO DummyHost dismisses anything
        // - test what prompt is currently active (don't burden that to the input method
        // - all tests succeed without this check, but that leaves a code smell
        // - tests with "inner reply" succeed often, but may still hang nevertheless (seldom)
        // -> condition must be handled!

        // TODO dismiss only active prompt
        if (prompt != active.get()) {
            throw new IllegalStateException("Can only dismiss active prompt");
        }

        // TODO only dismiss input methods that haven't been dismisseed already
        // - if timed out all need to be dismissed
        // - after user selection the input method that signaled the user input has been dismissed already
        // TODO dismiss only the input methods that haven't signaled user interaction
        try {
            boolean dismissed = false;
            for (InputMethod inputMethod : prompt.inputMethods) {
                dismissed &= inputMethod.dismiss(prompt);
            }
            active.set(null);
            return dismissed;
        } catch (InterruptedException e) {
            throw new ScriptInterruptedException(e);
        } catch (Error e) {
            throw e;
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public Prompt getActive() {
        return active.get();
    }
}
