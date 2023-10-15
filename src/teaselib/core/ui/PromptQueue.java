package teaselib.core.ui;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.concurrent.atomic.AtomicReference;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import teaselib.core.util.ExceptionUtil;

/**
 * @author Citizen-Cane
 *
 */
class PromptQueue {

    private static final Logger logger = LoggerFactory.getLogger(PromptQueue.class);

    private final Deque<Prompt> stack = new ArrayDeque<>();
    private final AtomicReference<Prompt> activePrompt = new AtomicReference<>();

    void show(Prompt prompt) throws InterruptedException {
        updateActivePrompt(active -> {
            stack.push(prompt);
            if (prompt == active) {
                throw new IllegalStateException("Already showing " + prompt);
            } else if (active != null) {
                active.lock.lockInterruptibly();
                try {
                    if (!active.paused()) {
                        pause(active);
                    }
                } finally {
                    active.lock.unlock();
                }
            }

            try {
                prompt.show();
            } catch (Exception e) {
                if (prompt.hasScriptTask()) {
                    prompt.cancelScriptTask();
                }
                throw ExceptionUtil.asRuntimeException(e);
            }

            if (prompt.hasScriptTask()) {
                prompt.executeScriptTask();
            }
            return prompt;
        });
        awaitResult(prompt);
    }

    void awaitResult(Prompt prompt) throws InterruptedException {
        if (prompt.undefined()) {
            prompt.click.await();
        }
        updateActivePrompt(active -> dismiss(active, prompt));
    }

    void resumePrevious() throws InterruptedException {
        if (!stack.isEmpty()) {
            var prompt = stack.peek();
            if (activePrompt.get() == prompt) {
                throw new IllegalStateException("Prompt not dismissed: " + prompt);
            }
            stack.pop();
        } else {
            throw new IllegalStateException("Prompt stack empty");
        }

        if (!stack.isEmpty()) {
            Prompt previous = stack.peek();
            previous.lock.lock();
            try {
                resume(previous);
            } finally {
                previous.lock.unlock();
            }
        }
    }

    private void resume(Prompt prompt) throws InterruptedException {
        updateActivePrompt(active -> {
            if (active != null) {
                if (prompt == active) {
                    throw new IllegalStateException("Prompt already showing: " + prompt);
                } else {
                    throw new IllegalStateException("Previous prompt still showing:" + active);
                }
            } else if (!prompt.result().equals(Prompt.Result.UNDEFINED)) {
                return null;
            }

            prompt.resume();
            prompt.show();
            return prompt;
        });
    }

    void resumePreviousPromptAfterException(Prompt prompt, Exception e) {
        try {
            updateActivePrompt(active -> {
                if (stack.isEmpty()) {
                    return null;
                }

                if (prompt.hasScriptTask()) {
                    prompt.scriptTask.cancel(true);
                    prompt.scriptTask.awaitCompleted();
                }

                if (!stack.isEmpty() && stack.peek() != prompt) {
                    throw new IllegalStateException("Nested prompts not dismissed: " + prompt, e);
                }

                // active prompt UI must be unrealized explicitly
                if (prompt == active) {
                    if (prompt.undefined()) {
                        dismiss(active, prompt);
                    }
                }
                stack.remove(prompt);
                if (!stack.isEmpty()) {
                    return stack.peek();
                }
                return null;
            });

            if (prompt.isActive()) {
                throw new IllegalStateException("Input methods not dismissed: " + prompt, e);
            }
        } catch (InterruptedException ignore) {
            Thread.currentThread().interrupt();
        } catch (RuntimeException ignore) {
            logger.warn(ignore.getMessage(), ignore);
        }
    }

    private class HiddenPrompt {

        private final Prompt prompt = new Prompt(null, new InputMethods());

        HiddenPrompt() throws InterruptedException {
            prompt.lock.lock();
            try {
                updateActivePrompt(active -> {
                    stack.push(prompt);
                    prompt.show();
                    return prompt;
                });
            } finally {
                prompt.lock.unlock();
            }
        }

        public void dismiss() {
            PromptQueue.this.activePrompt.updateAndGet(active -> {
                if (stack.peek() == prompt) {
                    stack.pop();
                }
                return null;
            });
        }
    }

    void invokeHandler(Prompt prompt, InputMethodEventArgs eventArgs) throws InterruptedException {
        if (activePrompt.get() != null) {
            throw new IllegalStateException("No active prompt expected");
        }
        prompt.pause();

        if (!prompt.undefined()) {
            throw new IllegalStateException("Prompt result already set when invoking handler: " + prompt);
        }

        // Ensure parent prompt is only resumed once the handler has completed
        var hidden = new HiddenPrompt();
        try {
            if (!contains(eventArgs.source)) {
                prompt.executeInputMethodHandler(eventArgs);
            }
        } finally {
            hidden.dismiss();
        }

        if (stack.peek() != prompt) {
            throw new IllegalStateException("Not top-most: " + prompt);
        }

        resume(prompt);
    }

    private boolean contains(InputMethod.Notification eventType) {
        Prompt current = stack.peek();
        for (Prompt prompt : stack) {
            if (prompt != current) {
                if (prompt.inputMethodEventArgs.get().source == eventType) {
                    return true;
                }
            }
        }
        return false;
    }

    void pauseCurrent() throws InterruptedException {
        updateActivePrompt(active -> {
            if (!stack.isEmpty()) {
                var prompt = stack.peek();
                prompt.lock.lockInterruptibly();
                try {
                    if (!prompt.paused()) {
                        pause(prompt);
                    }
                } finally {
                    prompt.lock.unlock();
                }
                return prompt;
            } else {
                return null;
            }
        });
    }

    private static void pause(Prompt prompt) {
        prompt.pause();
        if (prompt.undefined()) {
            prompt.dismiss();
        } else {
            throw new IllegalStateException("Prompt result already set: " + prompt);
        }
    }

    private static Prompt dismiss(Prompt active, Prompt prompt) {
        if (active == null) {
            throw new IllegalStateException("Prompt already dismissed: " + prompt);
        }
        if (prompt != active) {
            throw new IllegalStateException("Can only dismiss active prompt: " + active);
        }
        prompt.dismiss();
        return null;
    }

    private interface UpdatePrompt {
        Prompt apply(Prompt active) throws InterruptedException;
    }

    private void updateActivePrompt(UpdatePrompt updateFunction) throws InterruptedException {
        activePrompt.updateAndGet(active -> {
            try {
                return updateFunction.apply(active);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return null;
            }
        });
        if (Thread.interrupted()) {
            throw new InterruptedException();
        }
    }

    void updateUI(InputMethod.UiEvent event) {
        var prompt = stack.peek();
        // if locked either because realizing or dismissing
        // -> no update necessary
        if (prompt != null) {
            if (prompt.lock.tryLock()) {
                try {
                    prompt.updateUI(event);
                } finally {
                    prompt.lock.unlock();
                }
            } else if (prompt.isActive()) {
                logger.info("skipping UI update for locked prompt {}", this);
            }
        }
    }

    void updateUI_(InputMethod.UiEvent event) throws InterruptedException {
        if (!stack.isEmpty()) {
            var prompt = stack.peek();
            prompt.lock.lockInterruptibly();
            try {
                prompt.updateUI(event);
            } finally {
                prompt.lock.unlock();
            }
        }
    }

    @Override
    public String toString() {
        return activePrompt.toString();
    }

}
