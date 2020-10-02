package teaselib;

import java.util.concurrent.TimeUnit;

public interface Duration {

    static final long INFINITE = Long.MAX_VALUE;

    static final Duration Expired = new Duration() {

        @Override
        public long start(TimeUnit unit) {
            return 0;
        }

        @Override
        public long limit(TimeUnit unit) {
            return 0;
        }

        @Override
        public long elapsed(TimeUnit unit) {
            return 0;
        }

        @Override
        public long remaining(TimeUnit unit) {
            return 0;
        }

        @Override
        public boolean expired() {
            return true;
        }

        @Override
        public long end(TimeUnit unit) {
            return 0;
        }

        @Override
        public long since(TimeUnit unit) {
            return Long.MAX_VALUE;
        }

    };

    public static int compare(Duration a, Duration b) {
        return Long.compare(a.remaining(TimeUnit.SECONDS), b.remaining(TimeUnit.SECONDS));
    }

    /**
     * The start time of the duration.
     * 
     * @param unit
     * @return
     */
    long start(TimeUnit unit);

    /**
     * The supposed time span of the duration.
     * 
     * @param unit
     * @return
     */
    long limit(TimeUnit unit);

    /**
     * The elapsed time since the start.
     * 
     * @param unit
     * @return
     */
    long elapsed(TimeUnit unit);

    /**
     * The remaining time units until this duration expires.
     * 
     * @param unit
     * @return
     */
    long remaining(TimeUnit unit);

    /**
     * Whether the duration has passed its limit.
     * 
     * @return
     */
    boolean expired();

    /**
     * 
     * The time at which this duration ends.
     * 
     * @param unit
     * @return For elapsing durations, returns the current time.
     *         <p>
     *         For elapsed duration, returns the time the duration has ended.
     */
    long end(TimeUnit unit);

    /**
     * The time passed since the end of the duration.
     * 
     * @param unit
     * @return For elapsing durations, returns 0 because the duration hasn't ended yet.
     *         <p>
     *         However if the duration has elapsed, e.g. the process it describes has come to an end, since() returns
     *         the passed time.
     * 
     */
    long since(TimeUnit unit);
}