package teaselib.core.util;

import java.io.IOException;
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
    private final Map<String, Future<T>> scheduled = new HashMap<>();
    private final Map<String, T> fetched = new HashMap<>();

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
                scheduled.put(key, executorService.submit(() -> fetcher.apply(key)));
            }
        }
    }

    public T get(String key) throws IOException, InterruptedException {
        synchronized (this) {
            if (fetched.containsKey(key)) {
                return fetched.get(key);
            } else if (scheduled.containsKey(key)) {
                Future<T> future = scheduled.get(key);
                scheduled.remove(key);
                T t = get(future);
                fetched.put(key, t);
                return t;
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
            if (e.getCause() instanceof IOException) {
                throw (IOException) e.getCause();
            } else {
                throw new IOException(e.getMessage(), e);
            }
        }
    }

    public void remove(String key) {
        synchronized (this) {
            scheduled.remove(key);
        }
    }

}
