package teaselib.core.ui;

/**
 * @author Citizen-Cane
 *
 */
public abstract class AnimationPath {

    final double start;
    final double end;

    final long startMillis;
    final long durationMillis;

    public AnimationPath(double start, double end, long startMillis, long durationMillis) {
        this.start = start;
        this.end = end;
        this.startMillis = startMillis;
        this.durationMillis = durationMillis;
    }

    public abstract double get(long timePoint);

    @Override
    public String toString() {
        return start + "->" + end + " in " + durationMillis + "ms";
    }

    static class Constant extends AnimationPath {

        public Constant(double value) {
            super(value, value, 0, 0);
        }

        @Override
        public double get(long timePoint) {
            return end;
        }

        @Override
        public String toString() {
            return "" + end;
        }

    }

    static class Delay extends AnimationPath {

        private final AnimationPath delayed;

        public Delay(long durationMillis, AnimationPath path) {
            super(0.0, 0.0, path.startMillis, durationMillis);
            this.delayed = path;
        }

        @Override
        public double get(long timePoint) {
            if (timePoint - delayed.startMillis + durationMillis < 0) {
                return delayed.start;
            } else {
                return delayed.get(timePoint - durationMillis);
            }
        }
    }

    static class Linear extends AnimationPath {

        public Linear(double start, double end, long startMillis, long durationMillis) {
            super(start, end, startMillis, durationMillis);
        }

        @Override
        public double get(long timePoint) {
            if (timePoint <= startMillis)
                return start;
            if (timePoint >= startMillis + durationMillis)
                return end;
            double elapsed = timePoint - startMillis;
            return start + elapsed / durationMillis * (end - start);
        }

    }

}
