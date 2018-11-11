package teaselib.core;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Supplier;

import teaselib.core.CodeCoverageDecisionCollector.DecisionList;
import teaselib.core.concurrency.NamedExecutorService;
import teaselib.core.ui.InputMethod;
import teaselib.core.ui.Prompt;
import teaselib.functional.RunnableScript;

/**
 * @author Citizen-Cane
 *
 */
public class CodeCoverage<T extends Script> {
    private final Supplier<T> supplier;
    private final Consumer<T> runner;
    private final CodeCoverageInputMethod debugInputMethod = new CodeCoverageInputMethod(
            NamedExecutorService.singleThreadedQueue(getClass().getSimpleName(), 10, TimeUnit.SECONDS));
    private final CodeCoverageDecisionCollector decisionVariants = new CodeCoverageDecisionCollector();
    private final Iterator<DecisionList> current = decisionVariants.iterator();

    @SuppressWarnings("unchecked")
    public <S extends RunnableScript> CodeCoverage(Supplier<S> scriptSupplier) {
        this.supplier = (Supplier<T>) scriptSupplier;
        this.runner = s -> ((S) s).run();
    }

    public CodeCoverage(Supplier<T> scriptSupplier, Consumer<T> runner) {
        this.supplier = scriptSupplier;
        this.runner = runner;
    }

    public boolean hasNext() {
        return current.hasNext();
    }

    public void run() {
        T script = supplier.get();
        Debugger debugger = new Debugger(script, debugInputMethod);
        debugger.freezeTime();

        DecisionList decisions = current.next();
        Iterator<Integer> index = new ArrayList<>(decisions).iterator();

        Set<Prompt> used = new LinkedHashSet<>();
        InputMethod.Listener e = new InputMethod.Listener() {
            @Override
            public int promptShown(Prompt prompt) {
                if (used.contains(prompt)) {
                    throw new IllegalAccessError();
                } else {
                    used.add(prompt);
                }

                int result;
                if (index.hasNext()) {
                    result = index.next();
                    if (result >= prompt.choices.size()) {
                        throw new IndexOutOfBoundsException(result + "->" + toString() + ": " + prompt);
                    }
                } else {
                    result = 0;

                    int choices = prompt.choices.size();
                    if (choices > 1) {
                        for (int i = 1; i < choices; i++) {
                            DecisionList clone = new DecisionList(decisions, i);
                            decisionVariants.add(clone);
                        }
                    }
                    decisions.add(0);
                }

                return result;
            }

            @Override
            public void promptDismissed(Prompt prompt) {
                // Ignore
            }
        };
        debugInputMethod.addEventListener(e);

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
            debugInputMethod.removeEventListener(e);
            debugger.detach();
        }
    }
}
