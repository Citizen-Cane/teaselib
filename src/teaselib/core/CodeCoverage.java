package teaselib.core;

import static java.util.concurrent.TimeUnit.SECONDS;
import static teaselib.core.concurrency.NamedExecutorService.singleThreadedQueue;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.function.Consumer;
import java.util.function.Supplier;

import teaselib.core.CodeCoverageDecisionCollector.DecisionList;
import teaselib.core.CodeCoverageDecisionCollector.DecisionList.Entry;
import teaselib.core.ui.InputMethod;
import teaselib.core.ui.Prompt;
import teaselib.functional.RunnableScript;

/**
 * @author Citizen-Cane
 *
 */
public class CodeCoverage<T extends Script> implements Closeable {
    private final ExecutorService executor = singleThreadedQueue(getClass().getSimpleName(), 10, SECONDS);
    private final CodeCoverageInputMethod debugInputMethod = new CodeCoverageInputMethod(executor);
    final CodeCoverageDecisionCollector decisionVariants = new CodeCoverageDecisionCollector();
    private final Iterator<DecisionList> current = decisionVariants.iterator();

    @Override
    public void close() {
        executor.shutdown();
        debugInputMethod.close();
    }

    public boolean hasNext() {
        return current.hasNext();
    }

    @SuppressWarnings("unchecked")
    public <S extends RunnableScript> void run(Supplier<S> scriptSupplier) {
        run((Supplier<T>) scriptSupplier, s -> ((S) s).run());
    }

    public void run(Supplier<T> scriptSupplier, Consumer<T> runner) {
        T script = scriptSupplier.get();
        Debugger debugger = new Debugger(script, debugInputMethod);
        debugger.freezeTime();
        debugger.advanceTimeAllThreads();

        DecisionList decisions = current.next();
        Iterator<DecisionList.Entry> index = new ArrayList<>(decisions).iterator();

        Set<Prompt> used = new LinkedHashSet<>();
        InputMethod.Listener inputMethodListener = new InputMethod.Listener() {
            @Override
            public Prompt.Result promptShown(Prompt prompt) {
                if (used.contains(prompt)) {
                    throw new IllegalAccessError("Reused prompt: " + prompt.choices.toText());
                } else {
                    used.add(prompt);
                }

                int result;
                if (index.hasNext()) {
                    Entry next = index.next();
                    if (prompt.choices.equals(next.prompt.choices)) {
                        result = next.result;
                    } else {
                        throw new IllegalStateException("Indeterministic behavior detected: Expected prompt"
                                + next.prompt.choices.toText() + " but got " + prompt.choices.toText());
                    }
                } else {
                    // TODO sanity check prompt indeterministic
                    // TODO Seems to hang in endless script function (Fotze ficken und abspritzen)
                    // - how to deal with that if it's indeed endless?
                    // TODO handle multiple choices
                    result = prompt.hasScriptTask() ? Prompt.Result.DISMISSED.elements.get(0) : 0;
                    int choices = prompt.choices.size();
                    int timeoutPaths = prompt.hasScriptTask() ? 1 : 0;
                    if (choices + timeoutPaths > 1) {
                        for (int i = 1 - timeoutPaths; i < choices; i++) {
                            DecisionList clone = new DecisionList(decisions, i, prompt);
                            decisionVariants.add(clone);
                        }
                    }
                    decisions.add(result, prompt);
                }

                // TODO advance time when frozen to simulate elapsed time while answering prompts and performing tasks
                // -> otherwise toy durations in scripts will be too long,
                // and code coverage paths will be different from real script execution

                return new Prompt.Result(result);
            }

            @Override
            public void promptDismissed(Prompt prompt) {
                // All done
            }

        };
        debugInputMethod.addEventListener(inputMethodListener);

        script.teaseLib.transcript.info("");
        script.teaseLib.transcript.info("");
        script.teaseLib.transcript.info("-----------------------------------------------------------");
        script.teaseLib.transcript.info("Run " + current.toString());
        script.teaseLib.transcript.info("-----------------------------------------------------------");
        try {
            runner.accept(script);
            script.teaseLib.transcript.info("-----------------------------------------------------------");
            script.teaseLib.transcript.info("Decisions: " + decisions.toString());
            script.teaseLib.transcript.info("-----------------------------------------------------------");
        } finally {
            debugInputMethod.removeEventListener(inputMethodListener);
            debugger.detach();
            try {
                script.teaseLib.close();
            } catch (Exception e) {
                script.teaseLib.transcript.error(e.getMessage(), e);
            }
        }
    }

    /**
     * Run all branches
     */
    public <S extends RunnableScript> void runAll(Supplier<S> scriptSupplier) {
        while (hasNext()) {
            run(scriptSupplier);
        }
    }

    public void runAll(Supplier<T> scriptSupplier, Consumer<T> runner) {
        while (hasNext()) {
            run(scriptSupplier, runner);
        }
    }
}
