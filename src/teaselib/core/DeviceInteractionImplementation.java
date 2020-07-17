package teaselib.core;

import static java.util.stream.Collectors.toSet;

import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Deque;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiPredicate;
import java.util.stream.Stream;

import teaselib.Actor;

/**
 * @author Citizen-Cane
 *
 */
public abstract class DeviceInteractionImplementation<K, V> {

    protected class Definitions implements Iterable<Map.Entry<K, V>> {
        final Actor actor;
        private final Map<K, V> elements = new LinkedHashMap<>();
        public final Deque<K> pending = new ArrayDeque<>();

        public Definitions(Actor actor) {
            this.actor = actor;
        }

        public V define(K key, V value) {
            remove(elements, key);
            remove(pending, key);
            elements.put(key, value);
            return value;
        }

        public V get(K key) {
            return elements.get(key);
        }

        private void remove(Collection<K> collection, K key) {
            Set<K> remove = new HashSet<>();
            collection.stream().filter(element -> matcher.test(element, key)).forEach(remove::add);
            remove.stream().forEach(collection::remove);
        }

        private void remove(Map<K, V> map, K key) {
            Set<K> remove = new HashSet<>();
            map.keySet().stream().filter(element -> matcher.test(element, key)).forEach(remove::add);
            remove.stream().forEach(map::remove);
        }

        public Optional<V> findMatching(K key) {
            return elements.entrySet().stream().filter(entry -> matcher.test(entry.getKey(), key))
                    .map(Entry<K, V>::getValue).findFirst();
        }

        public boolean isEmpty() {
            return elements.isEmpty();
        }

        public int size() {
            return elements.size();
        }

        @Override
        public Iterator<Entry<K, V>> iterator() {
            return elements.entrySet().iterator();
        }

        public Stream<Entry<K, V>> stream() {
            return elements.entrySet().stream();
        }

        public void clear() {
            elements.clear();
            pending.clear();
        }

        public boolean clear(K key) {
            Set<K> keys = elements.keySet().stream().filter(element -> matcher.test(key, element)).collect(toSet());
            keys.stream().forEach(elements::remove);
            pending.removeIf(keys::contains);
            return !keys.isEmpty();
        }

    }

    private final Map<Actor, Definitions> definitions = new ConcurrentHashMap<>();
    final BiPredicate<K, K> matcher;

    protected DeviceInteractionImplementation(BiPredicate<K, K> matcher) {
        this.matcher = matcher;
    }

    protected Set<Actor> getActors() {
        return definitions.keySet();
    }

    protected Definitions definitions(Actor actor) {
        return definitions.computeIfAbsent(actor, Definitions::new);
    }

    protected void define(Actor actor, K key, V value) {
        definitions(actor).define(key, value);
    }

}
