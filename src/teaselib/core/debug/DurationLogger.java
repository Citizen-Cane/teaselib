package teaselib.core.debug;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.function.BiConsumer;
import java.util.function.Supplier;

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

    public void debug(Runnable task) {
        log(logger::isDebugEnabled, logger::debug, task);
    }

    public void info(Runnable task) {
        log(logger::isInfoEnabled, logger::info, task);
    }

    public void trace(Runnable task) {
        log(logger::isDebugEnabled, logger::debug, task);
    }

    private void log(Supplier<Boolean> canRun, BiConsumer<String, Object> log, Runnable task) {
        Boolean isEnabled = canRun.get();
        long start = isEnabled ? System.currentTimeMillis() : 0;
        task.run();
        if (isEnabled) {
            long now = System.currentTimeMillis();
            long frameTime = now - start;
            if (frametimes.size() > 100) {
                frametimes.remove();
            }
            frametimes.add(frameTime);
            log.accept("Frame time: {}ms", frametimes.stream().reduce(0L, Math::addExact) / frametimes.size());
        }
    }

}
