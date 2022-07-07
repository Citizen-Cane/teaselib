package teaselib.core.ui;

/**
 * @author Citizen-Cane
 *
 */
public abstract class AnimationPath {

    final static AnimationPath NONE = new AnimationPath(0, 0, 0, 0) {
        @Override
        public double get(long timePoint) {
            return 0;
        }
    };

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
