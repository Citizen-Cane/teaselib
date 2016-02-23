/**
 * 
 */
package teaselib.util;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * @author someone
 *
 */
public class Logger {
    private final BufferedWriter log;
    private final Level level;

    private boolean showTime = false;
    private boolean showThread = false;
    private boolean logToConsole = false;

    private final static SimpleDateFormat timeFormat = new SimpleDateFormat(
            "HH:mm:ss.SSS");

    public enum Level {
        None,
        Info,
        Debug
    }

    public static Logger getConsoleLogger() {
        return new Logger(Level.Info).logToConsole(true);
    }

    public static Logger getDummyLogger() {
        return new Logger(Level.None);
    }

    public Logger(Level level) {
        this.log = null;
        this.level = level;
    }

    public Logger(File logFile) throws IOException {
        this(logFile, Level.Info);
    }

    public Logger(File logFile, Level level) throws IOException {
        super();
        this.log = new BufferedWriter(new FileWriter(logFile));
        this.level = level;
        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                BufferedWriter log = Logger.this.log;
                if (log != null) {
                    try {
                        log.flush();
                        log.close();
                    } catch (IOException ignored) {
                    }
                }
            }
        });
    }

    public Logger showTime(boolean enable) {
        showTime = enable;
        return this;
    }

    public Logger showThread(boolean enable) {
        showThread = enable;
        return this;
    }

    public Logger logToConsole(boolean enable) {
        logToConsole = enable;
        return this;
    }

    private void log(String text) {
        if (log != null) {
            synchronized (log) {
                try {
                    log.write(format(text));
                    log.flush();
                } catch (IOException e) {
                    // Ignore
                }
            }
        }
        if (logToConsole) {
            System.out.print(format(text));
        }
    }

    public void info(String text) {
        log(text);
    }

    private String format(String text) {
        Date now = new Date(System.currentTimeMillis());
        String line = (showTime ? (timeFormat.format(now) + " [") : "")
                + (showThread ? (Thread.currentThread().getName() + "] ") : "")
                + text + "\n";
        return line;
    }

    public boolean logDetails() {
        return level == Level.Debug;
    }

    /**
     * Log details that might be interesting for developers
     * 
     * @param line
     *            Log output
     */
    public void debug(String line) {
        if (logDetails()) {
            info(line);
        }
    }

    public void debug(Object instance, Throwable e) {
        if (logDetails()) {
            error(instance, e);
        }
    }

    public void error(Object instance, Throwable e) {
        String message = e.getMessage();
        info(e.getClass().getSimpleName() + ": "
                + (message != null ? message : ""));
        info(instance.getClass().getName() + ":" + instance.toString());
        stackTrace(e);
        Throwable cause = e.getCause();
        if (cause != null) {
            info("Caused by");
            stackTrace(cause);
        }
    }

    private void stackTrace(Throwable e) {
        for (StackTraceElement ste : e.getStackTrace()) {
            info("\t" + ste.toString());
        }
    }
}
