package teaselib.core;

import java.util.function.Consumer;
import java.util.function.Supplier;

import teaselib.functional.RunnableScript;

/**
 * @author Citizen-Cane
 *
 */
public class CodeCoverage<T extends Script> {
    private final Supplier<T> supplier;
    private final Consumer<T> runner;

    private boolean hasMorePaths = true;

    @SuppressWarnings("unchecked")
    public <S extends RunnableScript> CodeCoverage(Supplier<S> scriptSupplier) {
        this.supplier = (Supplier<T>) scriptSupplier;
        this.runner = (Consumer<T>) new Consumer<S>() {
            @Override
            public void accept(S t) {
                t.run();
            }
        };
    }

    public CodeCoverage(Supplier<T> scriptSupplier, Consumer<T> runner) {
        this.supplier = scriptSupplier;
        this.runner = runner;
    }

    public boolean hasMorePaths() {
        return hasMorePaths;
    }

    public void run() {
        T script = supplier.get();
        Debugger debugger = new Debugger(script, new CodeCoverageInputMethod());
        debugger.freezeTime();

        runner.accept(script);
        hasMorePaths = false;
    }
}
