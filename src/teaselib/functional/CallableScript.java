package teaselib.functional;

/**
 * @author Citizen-Cane
 *
 */
@FunctionalInterface
public interface CallableScript<T> {
    T call();
}
