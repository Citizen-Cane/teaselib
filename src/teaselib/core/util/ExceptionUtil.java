package teaselib.core.util;

import java.lang.reflect.InvocationTargetException;
import java.util.concurrent.ExecutionException;

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

    public static RuntimeException asRuntimeException(Exception e) {
        if (e instanceof RuntimeException) {
            return (RuntimeException) e;
        } else {
            return new RuntimeException(e.getClass().getSimpleName() + ": " + e.getMessage(), e);
        }
    }
}
