package teaselib.core.util;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

public class Prefetcher<T> {

    private final ExecutorService executorService;
    private final Function<T> fetcher;

    private final Queue<String> queued = new ArrayDeque<>();
    private final Map<String, WeakReference<Future<T>>> scheduled = new HashMap<>();
    private final Map<String, WeakReference<T>> fetched = new HashMap<>();

    public interface Function<T> {
        T apply(String key) throws IOException;
    }

    public Prefetcher(ExecutorService executorService, Function<T> fetcher) {
        this.executorService = executorService;
        this.fetcher = fetcher;
    }

    public void add(String key) {
        synchronized (this) {
            queued.add(key);
        }
    }

    public boolean isEmpty() {
        synchronized (this) {
            return queued.isEmpty();
        }
    }

    public void fetch() {
        synchronized (this) {
            if (!queued.isEmpty()) {
                String key = queued.poll();
                fetch(key);
            }
        }
    }

    public void fetch(String key) {
        synchronized (this) {
            if (!fetched.containsKey(key) && !scheduled.containsKey(key)) {
                scheduled.put(key, new WeakReference<>(executorService.submit(() -> fetcher.apply(key))));
            }
        }
    }

    public T get(String key) throws IOException, InterruptedException {
        synchronized (this) {
            if (fetched.containsKey(key)) {
                T t = fetched.get(key).get();
                if (t == null) {
                    fetched.remove(key);
                    return get(key);
                } else {
                    return t;
                }
            } else if (scheduled.containsKey(key)) {
                Future<T> future = scheduled.get(key).get();
                scheduled.remove(key);
                if (future == null) {
                    return get(key);
                } else {
                    T t = get(future);
                    fetched.put(key, new WeakReference<>(t));
                    return t;
                }
            } else {
                fetch(key);
                return get(key);
            }
        }
    }

    private T get(Future<T> future) throws InterruptedException, IOException {
        try {
            return future.get();
        } catch (ExecutionException e) {
            Throwable cause = e.getCause();
            if (cause instanceof InterruptedException interruptedException) {
                throw interruptedException;
            } else if (cause instanceof IOException ioException) {
                throw ioException;
            } else {
                throw ExceptionUtil.asRuntimeException(e);
            }
        }
    }

    public void remove(String key) {
        synchronized (this) {
            scheduled.remove(key);
        }
    }

}
