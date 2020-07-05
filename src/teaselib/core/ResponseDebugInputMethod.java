package teaselib.core;

import java.util.Objects;
import java.util.concurrent.ExecutionException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import teaselib.core.Debugger.Response;
import teaselib.core.Debugger.ResponseAction;
import teaselib.core.concurrency.NamedExecutorService;
import teaselib.core.debug.CheckPoint;
import teaselib.core.debug.CheckPointListener;
import teaselib.core.debug.DebugResponses;
import teaselib.core.debug.DebugResponses.Result;
import teaselib.core.debug.TimeAdvanceListener;
import teaselib.core.debug.TimeAdvancedEvent;
import teaselib.core.ui.AbstractInputMethod;
import teaselib.core.ui.Choices;
import teaselib.core.ui.InputMethod;
import teaselib.core.ui.Prompt;

/**
 * @author Citizen-Cane
 *
 */
public class ResponseDebugInputMethod extends AbstractInputMethod implements DebugInputMethod {
    private static final Logger logger = LoggerFactory.getLogger(ResponseDebugInputMethod.class);

    public enum Notification implements InputMethod.Notification {
        DebugInput
    }

    private final DebugResponses responses = new DebugResponses();
    private final Runnable debugAction;

    Prompt.Result result;

    public ResponseDebugInputMethod(Runnable debugAction) {
        super(NamedExecutorService.singleThreadedQueue(ResponseDebugInputMethod.class.getSimpleName()));
        this.debugAction = debugAction;
    }

    private final TimeAdvanceListener timeAdvanceListener = this::handleRealTime;
    private final CheckPointListener checkPointListener = this::handleFrozenTime;

    private void handleRealTime(TimeAdvancedEvent e) {
        synchronized (this) {
            Prompt prompt = activePrompt.get();
            if (prompt != null) {
                Runnable respond = () -> {
                    prompt.lock.lock();
                    try {
                        if (prompt.result() == Prompt.Result.UNDEFINED && !prompt.paused()) {
                            dismissExpectedPromptOrIgnore(prompt);
                        } else {
                            logger.warn("Time advance skipped for {}", prompt);
                        }
                    } finally {
                        prompt.lock.unlock();
                    }
                };
                Thread t = new Thread(respond);
                t.setName(ResponseDebugInputMethod.class.getSimpleName() + " " + getClass().getSimpleName());
                t.start();
            }
        }
    }

    private void dismissExpectedPromptOrIgnore(Prompt prompt) {
        Result debugResponse = responses.getResponse(prompt.choices);
        if (debugResponse.response == Response.Choose) {
            synchronized (this) {
                result = new Prompt.Result(debugResponse.index);
                logger.info("Signalling {} to {}", result, prompt);
                notifyAll();
            }
        } else if (debugResponse.response == Response.Invoke) {
            logger.info("Signalling handler invocation {} to {}", debugResponse, prompt);
            invokeHandlerOnce(prompt, debugResponse);
        } else if (debugResponse.response == Response.Ignore) {
            logger.info("Ignoring {}", result);
        } else {
            throw new UnsupportedOperationException(debugResponse.toString());
        }
    }

    private void handleFrozenTime(CheckPoint checkPoint) {
        if (checkPoint == CheckPoint.ScriptFunction.Finished) {
            synchronized (this) {
                Prompt prompt = activePrompt.get();
                if (prompt != null) {
                    Result debugResponse = responses.getResponse(prompt.choices);
                    if (debugResponse.response == Response.Choose) {
                        waitUntilScriptFunctionIsCancelled();
                    } else if (debugResponse.response == Response.Invoke) {
                        // TODO This might not work with frozen time
                        logger.info("Signalling handler invocation {} to {}", debugResponse, prompt);
                        invokeHandlerOnce(prompt, debugResponse);
                    } else if (debugResponse.response == Response.Ignore) {
                        logger.info("Ignoring {}", result);
                    } else {
                        throw new UnsupportedOperationException(debugResponse.toString());
                    }
                }
            }
        }
    }

    private void waitUntilScriptFunctionIsCancelled() {
        try {
            wait(Long.MAX_VALUE);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    @Override
    public Setup getSetup(Choices choices) {
        return () -> result = Prompt.Result.UNDEFINED;
    }

    @Override
    protected Prompt.Result handleShow(Prompt prompt) throws InterruptedException, ExecutionException {
        synchronized (this) {
            Objects.requireNonNull(activePrompt.get());
            Objects.requireNonNull(prompt);

            result = dismissExpectedPromptOrShow(prompt);
            while (result == Prompt.Result.UNDEFINED) {
                wait();
            }
            return result;
        }
    }

    @Override
    protected void handleDismiss(Prompt prompt) throws InterruptedException {
        synchronized (this) {
            notifyAll();
        }

        if (prompt.result().equals(Prompt.Result.UNDEFINED) && dismissed()) {
            prompt.setResultOnce(this, result);
        }
    }

    private boolean dismissed() {
        return !result.equals(Prompt.Result.UNDEFINED) && !result.equals(Prompt.Result.DISMISSED);
    }

    private Prompt.Result dismissExpectedPromptOrShow(Prompt prompt) {
        Result debugResponse = responses.getResponse(prompt.choices);
        if (debugResponse.response == Response.Choose) {
            result = new Prompt.Result(debugResponse.index);
        } else if (debugResponse.response == Response.Invoke) {
            invokeHandlerOnce(prompt, debugResponse);
        }
        return result;
    }

    private void invokeHandlerOnce(Prompt prompt, Result debugResponse) {
        AbstractInputMethod.signal(prompt, debugAction, new ResponseDebugInputMethodEventArgs(Notification.DebugInput));
        responses.replace(new ResponseAction(debugResponse.match, Response.Choose));
    }

    public DebugResponses getResponses() {
        return responses;
    }

    @Override
    public void attach(TeaseLib teaseLib) {
        if (teaseLib.isTimeFrozen()) {
            teaseLib.addCheckPointListener(checkPointListener);
        } else {
            teaseLib.addTimeAdvancedListener(timeAdvanceListener);
        }
    }

    @Override
    public void detach(TeaseLib teaseLib) {
        teaseLib.removeTimeAdvancedListener(timeAdvanceListener);
        teaseLib.removeCheckPointListener(checkPointListener);
    }

    public void replyScriptFunction(String match) {
        synchronized (this) {
            if (!dismissed()) {
                Prompt prompt = activePrompt.get();
                if (prompt == null) {
                    throw new IllegalStateException("No active prompt: " + match);
                } else {
                    Result response = DebugResponses.getResponse(prompt.choices, new ResponseAction(match), null);
                    if (response != null) {
                        result = new Prompt.Result(response.index);
                        notifyAll();
                    } else {
                        throw new IllegalArgumentException("Prompt " + prompt + " doesn't match '" + match + "'");
                    }
                }
            }
        }
    }

    @Override
    public String toString() {
        Prompt prompt = activePrompt.get();
        if (prompt != null) {
            return prompt.toString();
        } else {
            return "<no active prompt>";
        }
    }

}
