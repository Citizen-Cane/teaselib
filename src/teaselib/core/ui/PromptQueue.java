package teaselib.core.ui;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.concurrent.atomic.AtomicReference;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import teaselib.core.Closeable;
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
        stack.push(prompt);
        Prompt active = activePrompt.get();

        if (prompt == active) {
            throw new IllegalStateException("Already showing " + prompt);
        } else if (active != null) {
            pause(active);
        }

        try {
            activate(prompt);
        } catch (Exception e) {
            if (prompt.hasScriptTask()) {
                prompt.cancelScriptTask();
            }
            throw ExceptionUtil.asRuntimeException(e);
        }

        if (prompt.hasScriptTask()) {
            prompt.executeScriptTask();
        }

        awaitResult(prompt);
    }

    void awaitResult(Prompt prompt) throws InterruptedException {
        if (prompt.undefined()) {
            prompt.click.await();
        }
        dismiss(prompt);
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
        Prompt active = activePrompt.get();
        if (active != null) {
            if (prompt == active) {
                throw new IllegalStateException("Prompt already showing: " + prompt);
            } else {
                throw new IllegalStateException("Previous prompt still showing:" + active);
            }
        } else if (!prompt.result().equals(Prompt.Result.UNDEFINED)) {
            return;
        }

        prompt.resume();
        activate(prompt);
    }

    void resumePreviousPromptAfterException(Prompt prompt, Exception e) {
        try {
            if (stack.isEmpty()) {
                throw new IllegalStateException("Prompt stack empty: " + prompt, e);
            }

            if (prompt.hasScriptTask()) {
                prompt.scriptTask.cancel(true);
                try {
                    prompt.scriptTask.awaitCompleted();
                } catch (InterruptedException interrupted) {
                    Thread.currentThread().interrupt();
                }
            }

            if (!stack.isEmpty() && stack.peek() != prompt) {
                throw new IllegalStateException("Nested prompts not dismissed: " + prompt, e);
            }

            // active prompt UI must be unrealized explicitly
            if (prompt == activePrompt.get()) {
                if (prompt.undefined()) {
                    dismiss(prompt);
                }
            }
            stack.remove(prompt);
            if (!stack.isEmpty()) {
                setActive(stack.peek());
            }
            if (prompt.isActive()) {
                throw new IllegalStateException("Input methods not dismissed: " + prompt, e);
            }
        } catch (RuntimeException ignore) {
            logger.warn(ignore.getMessage(), ignore);
            throw ExceptionUtil.asRuntimeException(e);
        }
    }

    private class HiddenPrompt implements Closeable {

        private final Prompt prompt = new Prompt(null, new InputMethods());

        HiddenPrompt() throws InterruptedException {
            prompt.lock.lock();
            stack.push(prompt);
            PromptQueue.this.activate(prompt);
            prompt.lock.unlock();
        }

        @Override
        public void close() {
            PromptQueue.this.setActive(null);
            if (stack.peek() == prompt) {
                stack.pop();
            }
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
        try (var hidden = new HiddenPrompt()) {
            if (!contains(eventArgs.source)) {
                prompt.executeInputMethodHandler(eventArgs);
            }
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

    void activate(Prompt prompt) throws InterruptedException {
        prompt.show();
        setActive(prompt);
    }

    void pauseCurrent() throws InterruptedException {
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
        }
    }

    private void pause(Prompt prompt) {
        prompt.pause();
        if (prompt.undefined()) {
            dismiss(prompt);
        } else {
            throw new IllegalStateException("Prompt result already set: " + prompt);
        }
    }

    private void dismiss(Prompt prompt) {
        Prompt active = activePrompt.get();

        if (active == null) {
            throw new IllegalArgumentException("Prompt already dismissed: " + prompt);
        }
        if (prompt != active) {
            throw new IllegalArgumentException("Can only dismiss active prompt: " + active);
        }

        try {
            prompt.dismiss();
        } finally {
            setActive(null);
        }
    }

    void setActive(Prompt prompt) {
        activePrompt.set(prompt);
    }

    void updateUI(InputMethod.UiEvent event) throws InterruptedException {
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
