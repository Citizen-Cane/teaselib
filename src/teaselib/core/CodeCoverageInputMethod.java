package teaselib.core;

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import teaselib.core.debug.CheckPoint;
import teaselib.core.debug.CheckPointListener;
import teaselib.core.debug.TimeAdvanceListener;
import teaselib.core.debug.TimeAdvancedEvent;
import teaselib.core.ui.AbstractInputMethod;
import teaselib.core.ui.InputMethod;
import teaselib.core.ui.Prompt;

/**
 * @author Citizen-Cane
 *
 */
public class CodeCoverageInputMethod extends AbstractInputMethod implements DebugInputMethod {
    Set<InputMethod.Listener> eventListeners = new LinkedHashSet<>();

    private final TimeAdvanceListener timeAdvanceListener = this::handleTimeAdvance;
    private final CheckPointListener checkPointListener = this::handleCheckPointReached;

    public CodeCoverageInputMethod(ExecutorService executor) {
        super(executor);
    }

    private final AtomicReference<Prompt> activePrompt = new AtomicReference<>();
    private final AtomicInteger result = new AtomicInteger();

    CyclicBarrier checkPointScriptFunctionFinished = new CyclicBarrier(2);

    @Override
    public int handleShow(Prompt prompt) throws InterruptedException {
        activePrompt.set(prompt);
        result.set(Prompt.UNDEFINED);
        if (prompt.hasScriptFunction()) {
            int choice = firePromptShown(prompt);
            if (choice > Prompt.DISMISSED) {
                result.set(choice);
            }
            try {
                checkPointScriptFunctionFinished.await();
                return result.get();
            } catch (BrokenBarrierException e) {
                throw new InterruptedException();
            } finally {
                activePrompt.set(null);
                checkPointScriptFunctionFinished.reset();
            }
        } else {
            // TODO causes return -1 DISMISSED but no script function
            // java.lang.IndexOutOfBoundsException: -1-> [Gesture=Nod text='Jawohl, #title' display='Jawohl, Frau
            // Streng']
            // waiting result=UNDEFINED: teaselib.core.CodeCoverageInputMethod@5dcd8c7a
            // at teaselib.core.ui.Prompt.setResultOnce(Prompt.java:105)
            // at teaselib.core.ui.Prompt.signalResult(Prompt.java:126)
            // at teaselib.core.ui.AbstractInputMethod.signalResult(AbstractInputMethod.java:73)
            // at teaselib.core.ui.AbstractInputMethod.awaitAndSignalResult(AbstractInputMethod.java:62)
            // at teaselib.core.ui.AbstractInputMethod.access$0(AbstractInputMethod.java:59)
            // at teaselib.core.ui.AbstractInputMethod$1.call(AbstractInputMethod.java:39)
            // at teaselib.core.ui.AbstractInputMethod$1.call(AbstractInputMethod.java:1)
            // at java.base/java.util.concurrent.FutureTask.run(FutureTask.java:264)
            // at java.base/java.util.concurrent.ThreadPoolExecutor.runWorker(ThreadPoolExecutor.java:1128)
            // at java.base/java.util.concurrent.ThreadPoolExecutor$Worker.run(ThreadPoolExecutor.java:628)
            // at java.base/java.lang.Thread.run(Thread.java:834)
            //
            // result of java.lang.IllegalStateException: Indeterministic behavior detected -> coverage returns
            // DISMISSED of previous/next prompt
            // : Expected prompt[Der Sklave ist bereit, #title] but got [Der Sklaven-Saft ist ausgelaufen, #title, Bitte
            // melken Sie mich ab, #title]
            // at teaselib.core.CodeCoverage$1.promptShown(CodeCoverage.java:62)
            // at teaselib.core.CodeCoverageInputMethod.lambda$4(CodeCoverageInputMethod.java:193)
            // at java.base/java.util.stream.ReferencePipeline$3$1.accept(ReferencePipeline.java:195)
            // at java.base/java.util.Iterator.forEachRemaining(Iterator.java:133)
            // at java.base/java.util.Spliterators$IteratorSpliterator.forEachRemaining(Spliterators.java:1801)
            // at java.base/java.util.stream.AbstractPipeline.copyInto(AbstractPipeline.java:484)
            // at java.base/java.util.stream.AbstractPipeline.wrapAndCopyInto(AbstractPipeline.java:474)
            // at java.base/java.util.stream.ReduceOps$ReduceOp.evaluateSequential(ReduceOps.java:913)
            // at java.base/java.util.stream.AbstractPipeline.evaluate(AbstractPipeline.java:234)
            // at java.base/java.util.stream.ReferencePipeline.reduce(ReferencePipeline.java:558)
            // at teaselib.core.CodeCoverageInputMethod.firePromptShown(CodeCoverageInputMethod.java:194)
            // at teaselib.core.CodeCoverageInputMethod.setResult(CodeCoverageInputMethod.java:77)
            // at java.base/java.util.concurrent.atomic.AtomicReference.getAndUpdate(AtomicReference.java:187)
            // at teaselib.core.CodeCoverageInputMethod.handleCheckPointReached(CodeCoverageInputMethod.java:66)
            // at teaselib.core.TeaseLib.checkPointReached(TeaseLib.java:301)
            // at teaselib.core.ScriptFutureTask.lambda$0(ScriptFutureTask.java:38)
            // at java.base/java.util.concurrent.FutureTask.run(FutureTask.java:264)
            // at teaselib.core.ScriptFutureTask.run(ScriptFutureTask.java:54)
            // at java.base/java.util.concurrent.ThreadPoolExecutor.runWorker(ThreadPoolExecutor.java:1128)
            // at java.base/java.util.concurrent.ThreadPoolExecutor$Worker.run(ThreadPoolExecutor.java:628)
            // at java.base/java.lang.Thread.run(Thread.java:834)
            //

            return firePromptShown(activePrompt.get());
        }
    }

    private void handleCheckPointReached(CheckPoint checkPoint) {
        if (checkPoint == CheckPoint.ScriptFunction.Started) {
            // Ignore
        } else if (checkPoint == CheckPoint.Script.NewMessage) {
            // Ignore
        } else if (checkPoint == CheckPoint.ScriptFunction.Finished) {
            activePrompt.getAndUpdate(this::forwardResultAndHandleTimeout);
        }
    }

    private void handleTimeAdvance(TimeAdvancedEvent timeAdvancedEvent) {
        // Ignore
    }

    Prompt forwardResult(Prompt prompt) {
        if (hasScriptFunction(prompt) && resultSet()) {
            forwardResult();
            return null;
        } else {
            return prompt;
        }
    }

    private static boolean hasScriptFunction(Prompt prompt) {
        return prompt != null && prompt.hasScriptFunction();
    }

    private boolean resultNotSet() {
        return result.get() <= Prompt.DISMISSED;
    }

    private boolean resultSet() {
        return result.get() > Prompt.DISMISSED;
    }

    private void forwardResult() {
        try {
            if (resultNotSet()) {
                throw new IllegalArgumentException("Result must be set to choice");
            }
            synchronized (this) {
                if (!Thread.currentThread().isInterrupted()) {
                    checkPointScriptFunctionFinished.await();
                }
            }
        } catch (InterruptedException | BrokenBarrierException e) {
            Thread.currentThread().interrupt();
        }
    }

    Prompt forwardResultAndHandleTimeout(Prompt prompt) {
        if (hasScriptFunction(prompt) && resultSet()) {
            synchronized (this) {
                if (resultSet()) {
                    forwardResult();
                    try {
                        while (!Thread.currentThread().isInterrupted()) {
                            wait();
                        }
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }
            }
            return null;
        } else {
            return prompt;
        }
    }

    @Override
    public boolean handleDismiss(Prompt prompt) {
        synchronized (this) {
            activePrompt.set(null);
            checkPointScriptFunctionFinished.reset();
            firePromptDismissed(prompt);
            notifyAll();
        }
        return true;
    }

    @Override
    public Map<String, Runnable> getHandlers() {
        return new HashMap<>();
    }

    @Override
    public void attach(TeaseLib teaseLib) {
        teaseLib.addTimeAdvancedListener(timeAdvanceListener);
        teaseLib.addCheckPointListener(checkPointListener);
    }

    @Override
    public void detach(TeaseLib teaseLib) {
        teaseLib.removeTimeAdvancedListener(timeAdvanceListener);
        teaseLib.removeCheckPointListener(checkPointListener);
    }

    public void addEventListener(InputMethod.Listener e) {
        eventListeners.add(e);
    }

    public void removeEventListener(InputMethod.Listener e) {
        eventListeners.remove(e);
    }

    private int firePromptShown(Prompt prompt) {
        Optional<Integer> listenerResult = eventListeners.stream().map(e -> e.promptShown(prompt))
                .reduce(CodeCoverageInputMethod::firstResult);
        return listenerResult.isPresent() ? listenerResult.get() : Prompt.UNDEFINED;
    }

    private static int firstResult(int a, int b) {
        return a == Prompt.UNDEFINED ? b : a;
    }

    private void firePromptDismissed(Prompt prompt) {
        eventListeners.stream().forEach(e -> e.promptDismissed(prompt));
    }
}
