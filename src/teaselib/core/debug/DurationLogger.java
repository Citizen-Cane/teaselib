package teaselib.core.debug;

import java.util.ArrayDeque;
import java.util.Deque;

import org.slf4j.Logger;

/**
 * @author Citizen-Cane
 *
 */
public class DurationLogger {

    final Logger logger;

    private final Deque<Long> frametimes = new ArrayDeque<>(100);

    public DurationLogger(Logger logger) {
        this.logger = logger;
    }

    public void log(Runnable task) {
        long start = System.currentTimeMillis();
        task.run();
        if (logger.isInfoEnabled()) {
            long now = System.currentTimeMillis();
            long frameTime = now - start;
            if (frametimes.size() > 100) {
                frametimes.remove();
            }
            frametimes.add(frameTime);
            logger.info("Frame time: {}ms", frametimes.stream().reduce(0L, Math::addExact) / frametimes.size());
        }
    }

}
