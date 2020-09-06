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

    long start(TimeUnit unit);

    long limit(TimeUnit unit);

    long elapsed(TimeUnit unit);

    long remaining(TimeUnit unit);

    boolean expired();

    long end(TimeUnit unit);
}