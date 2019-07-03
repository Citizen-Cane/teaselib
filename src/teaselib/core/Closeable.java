package teaselib.core;

/**
 * @author Citizen-Cane
 *
 */
public interface Closeable extends java.lang.AutoCloseable {
    @Override
    void close();
}
