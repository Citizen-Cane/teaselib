package teaselib.functional;

/**
 * @author Citizen-Cane
 *
 */
@FunctionalInterface
public interface RunnableScript extends Runnable {
    @Override
    public void run();
}
