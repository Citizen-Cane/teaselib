package teaselib.core.util;

import java.util.concurrent.ExecutionException;

public class ExceptionUtil {

    public static Exception reduce(Exception e) {
        if (canReduce(e)) {
            Throwable cause = e.getCause();
            if (cause instanceof Exception) {
                return reduce((Exception) cause);
            }
        }
        return e;
    }

    private static boolean canReduce(Exception e) {
        return e instanceof ExecutionException || e instanceof RuntimeException;
    }

    public static RuntimeException asRuntimeException(Exception e) {
        if (e instanceof RuntimeException) {
            return (RuntimeException) e;
        } else {
            return new RuntimeException(e.getClass().getSimpleName() + ": " + e.getMessage(), e);
        }
    }
}
