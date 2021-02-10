package teaselib.core.util;

import java.util.function.Supplier;

import org.slf4j.Logger;

/**
 * @author Citizen-Cane
 *
 */
public class CodeDuration {

    private CodeDuration() {
    }

    public static long executionTimeMillis(Runnable code) {
        long start = System.currentTimeMillis();
        code.run();
        long end = System.currentTimeMillis();
        return end - start;
    }

    public static <T> T executionTimeMillis(Logger logger, String message, Supplier<T> code) {
        long start = System.currentTimeMillis();
        T result = code.get();
        long end = System.currentTimeMillis();
        logger.info(message, end - start);
        return result;
    }

}
