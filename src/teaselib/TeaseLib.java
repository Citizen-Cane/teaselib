package teaselib;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.concurrent.TimeUnit;

/**
 * @author someone
 *
 *         Teaselib can either be used via the shared global instance, or by
 *         creating a private instance.
 * 
 *         A private TeaseLib instance might be useful when using more than one
 *         TTS voice in a scenario with multiple actors
 * 
 *         All static methods refer to the shared instance
 */
public class TeaseLib {

    private static TeaseLib instance;

    public final Host host;
    public final ResourceLoader resources;
    public final Persistence persistence;

    public final static boolean logDetails = false;

    private static BufferedWriter log = null;
    private final static File logFile = new File("./TeaseLib.log");

    public TeaseLib(TeaseLib teaseLib, String assetRoot) {
        this(teaseLib.host, teaseLib.persistence, teaseLib.resources.basePath,
                assetRoot);
    }

    public TeaseLib(Host host, Persistence persistence, String basePath,
            String assetRoot) {
        if (host == null || persistence == null || basePath == null
                || assetRoot == null) {
            throw new IllegalArgumentException();
        }
        this.host = host;
        this.persistence = persistence;
        TeaseLib.instance = this;
        // Init log
        try {
            log = new BufferedWriter(new FileWriter(logFile));
        } catch (IOException e) {
            host.show(null, "Cannot open log file " + logFile.getAbsolutePath());
            host.reply(Arrays.asList("Oh dear"));
        }
        // Now we can log errors
        try {
            this.resources = new ResourceLoader(basePath, assetRoot);
        } catch (Throwable t) {
            log(this, t);
            throw new RuntimeException(t);
        }
    }

    public static TeaseLib instance() {
        if (instance == null) {
            throw new IllegalStateException(
                    "Please create a TeaseLib instance first");
        }
        return instance;
    }

    public static Host host() {
        return instance().host;
    }

    public static ResourceLoader resources() {
        return instance().resources;
    }

    public static Persistence persistence() {
        return instance().persistence;
    }

    public static synchronized void log(String text) {
        Date now = new Date(System.currentTimeMillis());
        SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm:ss.SSS");
        String line = timeFormat.format(now) + ": " + text;

        try {
            log.write(line + "\n");
            // todo move flush and close into jvm shutdown handler
            log.flush();
        } catch (IOException e) {
            host().show(null,
                    "Cannot write to log file " + logFile.getAbsolutePath());
        }
        System.out.println(line);
    }

    /**
     * Log details interesting for teaselib devs
     * 
     * @param line
     *            Log output
     */
    public static void logDetail(String line) {
        if (logDetails) {
            log(line);
        }
    }

    public static void logDetail(Object instance, Throwable e) {
        if (logDetails) {
            log(instance, e);
        }
    }

    public static void log(Object instance, Throwable e) {
        String message = e.getMessage();
        log(e.getClass().getSimpleName() + ": "
                + (message != null ? message : ""));
        log(instance.getClass().getName() + ":" + instance.toString());
        logStackTrace(e);
        Throwable cause = e.getCause();
        if (cause != null) {
            log("Caused by");
            logStackTrace(cause);
        }
    }

    private static void logStackTrace(Throwable e) {
        for (StackTraceElement ste : e.getStackTrace()) {
            log("\t" + ste.toString());
        }
    }

    public void addAssets(String... assets) {
        resources.addAssets(assets);
    }

    /**
     * Preferred method to wait, since it allows us to overwrite this method
     * with automated input.
     * 
     * If interrupted, must throw a ScriptInterruptedException. It's a runtime
     * exception so it doesn't have to be declared. This way simple scripts are
     * safe, but script closures can be cancelled.
     * 
     * @param milliseconds
     *            The time to sleep.
     */
    public void sleep(long x, TimeUnit timeUnit) {
        try {
            if (x > 0) {
                timeUnit.sleep(x);
            }
        } catch (InterruptedException e) {
            throw new ScriptInterruptedException();
        }
    }

    /**
     * @return Seconds since midnight 1.1.1970 UTC
     */
    public long getTime() {
        // todo change to millis
        return System.currentTimeMillis() / 1000;
    }

    /**
     * Return a random number
     * 
     * @param min
     *            minimum value
     * @param max
     *            maximum value
     * @return A value in the interval [min, max]
     */
    public int random(int min, int max) {
        double r = Math.random();
        return min + (int) (r * (max - min + 1));
    }
}
