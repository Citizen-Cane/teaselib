package teaselib.core.util;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.concurrent.ExecutionException;

import org.slf4j.Logger;

import teaselib.Config;
import teaselib.core.configuration.Configuration;

public class ExceptionUtil {

    private ExceptionUtil() {
    }

    /**
     * Tries to reduce the exception to its cause. Allows to reduce exceptions in a typed way with a single statement.
     * 
     * @param e
     *            The exception to reduce.
     * @return The reduced runtime-exception.
     * @throws E
     *             if e cannot be reduced.
     */
    public static <E extends Exception> RuntimeException reduced(E e) throws E {
        Exception reduced = ExceptionUtil.reduce(e);
        if (reduced == e) {
            throw e;
        } else {
            return ExceptionUtil.asRuntimeException(reduced);
        }
    }

    /**
     * Tries to remove the exception wrappers like ExecutionException or InvocationTargetException
     * 
     * @param e
     *            The exception to reduce to its cause.
     * @return The original exception, or its cause.
     */
    public static Exception reduce(Exception e) {
        if (canReduce(e)) {
            Throwable cause = e.getCause();
            if (cause instanceof Exception) {
                return reduce((Exception) cause);
            } else if (cause instanceof Error) {
                throw (Error) cause;
            } else if (cause == null) {
                return e;
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
        if (t instanceof Error e) {
            throw e;
        } else if (t instanceof RuntimeException r) {
            return r;
        } else if (t instanceof Exception) {
            Exception e = reduce((Exception) t);
            if (e instanceof RuntimeException r) {
                throw r;
            } else if (e != t) {
                return asRuntimeException(e);
            } else {
                return wrapped(t);
            }
        } else {
            return wrapped(t);
        }
    }

    private static RuntimeException wrapped(Throwable t) {
        String name = t.getClass().getSimpleName();
        String message = t.getMessage();
        return new RuntimeException(name + (message != null ? ": " + message : ""), t);
    }

    public static RuntimeException asRuntimeException(Exception e, String message) {
        if (e instanceof RuntimeException) {
            return (RuntimeException) e;
        } else {
            return new RuntimeException(message, e);
        }
    }

    public static void handleIOException(IOException e, Configuration config, Logger logger) throws IOException {
        boolean stopOnAssetNotFound = Boolean.parseBoolean(config.get(Config.Debug.StopOnAssetNotFound));
        if (stopOnAssetNotFound) {
            logger.error(e.getMessage(), e);
            throw e;
        } else {
            logger.warn(e.getMessage());
        }
    }

    public static void handleAssetNotFound(IOException e, Configuration config, Logger logger) {
        if (e instanceof IOException) {
            try {
                ExceptionUtil.handleIOException((IOException) e, config, logger);
            } catch (IOException e1) {
                throw ExceptionUtil.asRuntimeException(e1);
            }
        } else {
            if (Boolean.parseBoolean(config.get(Config.Debug.StopOnAssetNotFound))) {
                throw ExceptionUtil.asRuntimeException(e);
            } else {
                logger.warn(e.getMessage());
            }
        }
    }

}
