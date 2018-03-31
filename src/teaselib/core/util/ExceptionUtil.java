package teaselib.core.util;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.concurrent.ExecutionException;

import org.slf4j.Logger;

import teaselib.Config;
import teaselib.core.Configuration;

public class ExceptionUtil {

    private ExceptionUtil() {
    }

    public static Exception reduce(Exception e) {
        if (canReduce(e)) {
            Throwable cause = e.getCause();
            if (cause instanceof Exception) {
                return reduce((Exception) cause);
            } else {
                return new RuntimeException(cause);
            }
        }
        return e;
    }

    private static boolean canReduce(Exception e) {
        return e instanceof ExecutionException || e instanceof InvocationTargetException;
    }

    public static RuntimeException asRuntimeException(Throwable t) {
        if (t instanceof RuntimeException) {
            return (RuntimeException) t;
        } else {
            return new RuntimeException(t.getClass().getSimpleName() + ": " + t.getMessage(), t);
        }
    }

    public static RuntimeException asRuntimeException(Exception e) {
        if (e instanceof RuntimeException) {
            return (RuntimeException) e;
        } else {
            return new RuntimeException(e.getClass().getSimpleName() + ": " + e.getMessage(), e);
        }
    }

    public static RuntimeException asRuntimeException(Exception e, String message) {
        if (e instanceof RuntimeException) {
            return (RuntimeException) e;
        } else {
            return new RuntimeException(message, e);
        }
    }

    public static void handleIOException(IOException e, Configuration config, Logger logger) throws IOException {
        if (e instanceof IOException) {
            boolean stopOnAssetNotFound = Boolean.parseBoolean(config.get(Config.Debug.StopOnAssetNotFound));
            if (stopOnAssetNotFound) {
                logger.error(e.getMessage(), e);
                throw (IOException) e;
            } else {
                logger.warn(e.getMessage(), e);
            }
        }
    }

}
