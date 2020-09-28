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
     * The length of the duration.
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

    boolean expired();

    /**
     * The time at which this duration will expire.
     * 
     * @param unit
     * @return
     */
    long end(TimeUnit unit);
}