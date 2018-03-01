package teaselib.core.util;

import java.io.IOException;
import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import teaselib.core.concurrency.NamedExecutorService;

public class Prefetcher<T> {
    private static final ExecutorService fetcher = NamedExecutorService.singleThreadedQueue("Resource Prefetcher");

    private final Queue<String> resources = new ArrayDeque<>();
    private final Map<String, Callable<T>> toFetch = new HashMap<>();
    private final Map<String, Future<T>> fetched = new HashMap<>();

    public Prefetcher() {
    }

    public void add(String key, Callable<T> prefetcher) {
        synchronized (this) {
            resources.add(key);
            toFetch.put(key, prefetcher);
        }
    }

    public boolean isEmpty() {
        return resources.isEmpty();
    }

    public void fetch() {
        synchronized (this) {
            String key = resources.poll();
            if (!isEmpty() && !fetched.containsKey(key)) {
                fetch(key);
            }
        }
    }

    public void fetch(String key) {
        synchronized (this) {
            Callable<T> callable = toFetch.remove(key);
            fetched.put(key, fetcher.submit(callable));
        }
    }

    public T get(String key) throws IOException, InterruptedException {
        synchronized (this) {
            if (fetched.containsKey(key)) {
                try {
                    return fetched.get(key).get();
                } catch (ExecutionException e) {
                    if (e.getCause() instanceof IOException) {
                        throw (IOException) e.getCause();
                    } else {
                        throw new IOException(e.getMessage(), e);
                    }
                }
            } else {
                fetch(key);
                return get(key);
            }
        }
    }

    public void remove(String key) {
        synchronized (this) {
            toFetch.remove(key);
            fetched.remove(key);
        }
    }
}
