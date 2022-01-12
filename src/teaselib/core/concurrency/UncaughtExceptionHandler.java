package teaselib.core.concurrency;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Citizen-Cane
 *
 */
public class UncaughtExceptionHandler implements java.lang.Thread.UncaughtExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(UncaughtExceptionHandler.class);

    public static final UncaughtExceptionHandler Instance = new UncaughtExceptionHandler();

    private UncaughtExceptionHandler() {
    }

    @Override
    public void uncaughtException(Thread t, Throwable e) {
        logger.error("Uncaught exception {} in thread {}", e.getMessage(), t.getName(), e);
    }

}
