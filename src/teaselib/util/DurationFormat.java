package teaselib.util;

import java.util.concurrent.TimeUnit;

public class DurationFormat {
    private final String duration;
    private final long timeSeconds;

    public DurationFormat(long time, TimeUnit unit) {
        this.timeSeconds = TimeUnit.SECONDS.convert(time, unit);
        this.duration = toString(time, unit);
    }

    public static String toString(long time, TimeUnit unit) {
        long timeSeconds = TimeUnit.SECONDS.convert(time, unit);
        if (timeSeconds == Long.MAX_VALUE) {
            return "INF";
        } else if (timeSeconds == Long.MIN_VALUE) {
            return "-INF";
        } else {
            String sign = timeSeconds < 0 ? "-" : "";
            long absoluteTimeSeconds = Math.abs(timeSeconds);
            long h = Math.floorDiv(absoluteTimeSeconds, 60 * 60);
            long m = Math.floorDiv(absoluteTimeSeconds - h * 60 * 60, 60);
            long s = absoluteTimeSeconds - h * 60 * 60 - m * 60;
            return String.format("%s%02d:%02d\"%02d", sign, h, m, s);
        }
    }

    public DurationFormat(String duration) {
        this.duration = duration;
        timeSeconds = toSeconds(duration);
    }

    /**
     * Get the number of seconds of this duration
     * 
     * @return The duration in seconds
     */

    public long toSeconds() {
        return timeSeconds;
    }

    /**
     * Parse a PCM duration argument into seconds starting from 1970 (to be compatible with java.util.Date)
     * 
     * @param timeFrom
     *            milliseconds of the duration
     * @return
     */
    private static long toSeconds(String duration) {
        if ("INF".equalsIgnoreCase(duration)) {
            return teaselib.Duration.INFINITE;
        } else if ("-INF".equalsIgnoreCase(duration)) {
            return -teaselib.Duration.INFINITE;
        } else {
            long sign = 1;
            if (duration.startsWith("-")) {
                sign = -1;
                duration = duration.substring(1);
            } else if (duration.startsWith("+")) {
                duration = duration.substring(1);
            }
            final long hours;
            final long minutes;
            final long seconds;
            int doubleColonPos = duration.indexOf(':', 0);
            int doubleQuotePos = duration.indexOf('"', 0);
            if (doubleQuotePos >= 3) {
                hours = Integer.parseInt(duration.substring(0, doubleColonPos));
                minutes = Integer.parseInt(duration.substring(doubleColonPos + 1, doubleQuotePos));
                seconds = Integer.parseInt(duration.substring(doubleQuotePos + 1));
            } else {
                hours = Integer.parseInt(duration.substring(0, doubleColonPos));
                minutes = Integer.parseInt(duration.substring(doubleColonPos + 1));
                seconds = 0;
            }
            return sign * (3600 * hours + 60 * minutes + seconds);
        }
    }

    @Override
    public String toString() {
        return duration;
    }
}
