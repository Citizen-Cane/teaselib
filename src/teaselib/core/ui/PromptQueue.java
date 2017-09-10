package teaselib.core.ui;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicReference;

import teaselib.core.ScriptInterruptedException;
import teaselib.core.TeaseScriptBase;

/**
 * @author Citizen-Cane
 *
 */
public class PromptQueue {
    private final AtomicReference<Prompt> active = new AtomicReference<Prompt>();
    private final Set<Prompt> dismissedPermanent = new HashSet<Prompt>();

    public int show(TeaseScriptBase script, Prompt prompt) throws InterruptedException {
        prompt.lock.lockInterruptibly();
        try {
            synchronized (dismissedPermanent) {
                if (dismissedPermanent.contains(prompt)) {
                    return Prompt.DISMISSED;
                }
            }

            Prompt activePrompt = active.get();

            if (activePrompt != null && prompt == activePrompt) {
                throw new IllegalStateException("Prompt " + prompt + " already showing");
            }

            if (activePrompt != null) {
                pause(activePrompt);
            }

            try {
                makePromptActive(prompt);

                if (prompt.scriptFunction != null) {
                    prompt.executeScriptTask(script, getDismissCallable(prompt));
                }

                prompt.click.await();
            } finally {
                for (InputMethod inputMethod : prompt.inputMethods) {
                    inputMethod.dismiss(prompt);
                }

                active.set(null);
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
        } catch (InterruptedException e) {
            throw new ScriptInterruptedException();
        } finally {
            prompt.lock.unlock();
        }
    }

    public void resume(Prompt prompt) throws InterruptedException {
        prompt.lock.lockInterruptibly();
        try {
            synchronized (dismissedPermanent) {
                if (dismissedPermanent.contains(prompt)) {
                    return;
                }
            }

            Prompt activePrompt = active.get();

            if (activePrompt != null && prompt == activePrompt) {
                throw new IllegalStateException("Prompt " + prompt + " already showing");
            }

            if (activePrompt != null) {
                dismiss(activePrompt);
            }

            if (prompt.result() != Prompt.UNDEFINED)
                return;

            prompt.paused.set(false);

            makePromptActive(prompt);
            // Were're just resuming, no need to wait or cleanup
        } finally {
            prompt.lock.unlock();
        }
    }

    private void makePromptActive(Prompt prompt) {
        active.set(prompt);
        for (InputMethod inputMethod : prompt.inputMethods) {
            inputMethod.show(prompt);
        }
    }

    private boolean dismissUntilLater(Prompt prompt) throws InterruptedException {
        prompt.lock.lockInterruptibly();
        try {
            synchronized (dismissedPermanent) {
                dismissedPermanent.add(prompt);
            }
            return dismiss(prompt);
        } finally {
            prompt.lock.unlock();
        }
    }

    public boolean dismiss(Prompt prompt) throws InterruptedException {
        prompt.lock.lockInterruptibly();
        try {
            prompt.setResultOnce(Prompt.DISMISSED);
            return dismissPrompt(prompt);
        } finally {
            prompt.lock.unlock();
        }
    }

    public boolean pause(Prompt prompt) throws InterruptedException {
        prompt.lock.lockInterruptibly();
        try {
            prompt.paused.set(true);
            return dismissPrompt(prompt);
        } finally {
            prompt.lock.unlock();
        }
    }

    private boolean dismissPrompt(Prompt prompt) {
        try {
            boolean dismissed = false;
            for (InputMethod inputMethod : prompt.inputMethods) {
                dismissed |= inputMethod.dismiss(prompt);
            }
            active.set(null);
            return dismissed;
        } catch (Error e) {
            throw e;
        } catch (RuntimeException e) {
            throw e;
        } catch (InterruptedException e) {
            throw new ScriptInterruptedException();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public Prompt getActive() {
        return active.get();
    }

    public Callable<Boolean> getDismissCallable(final Prompt prompt) {
        Callable<Boolean> dismiss = new Callable<Boolean>() {
            @Override
            public Boolean call() throws Exception {
                return dismissUntilLater(prompt);
                // TODO remove method dismissUntilLater()
                // TODO also remove dismissedPermaent map
                // return dismiss(prompt);
            }
        };
        return dismiss;
    }

    public void clear(Prompt prompt) {
        dismissedPermanent.remove(prompt);
    }
}
