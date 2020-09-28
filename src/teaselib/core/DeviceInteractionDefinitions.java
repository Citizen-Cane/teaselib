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
import java.util.function.BiPredicate;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;

import teaselib.Actor;

public class DeviceInteractionDefinitions<K, V> implements Iterable<Map.Entry<K, V>> {
    final Actor actor;
    private final Map<K, V> elements = new LinkedHashMap<>();
    public final Deque<K> pending = new ArrayDeque<>();
    private final BiPredicate<K, K> matcher;

    public DeviceInteractionDefinitions(Actor actor, BiPredicate<K, K> matcher) {
        this.actor = actor;
        this.matcher = matcher;
    }

    public DeviceInteractionDefinitions(DeviceInteractionDefinitions<K, V> all, Predicate<Map.Entry<K, V>> filter) {
        this.actor = all.actor;
        this.matcher = all.matcher;
        all.stream().filter(filter).forEach(this::put);
    }

    private void put(Map.Entry<K, V> entry) {
        elements.put(entry.getKey(), entry.getValue());
    }

    public V define(K key, V value) {
        remove(elements, key);
        remove(pending, key);
        elements.put(key, value);
        return value;
    }

    public boolean contains(K key) {
        return elements.containsKey(key);
    }

    public V get(K key) {
        return elements.get(key);
    }

    public V get(K key, Function<K, V> value) {
        return elements.computeIfAbsent(key, value::apply);
    }

    public void remove(K key) {
        remove(elements, key);
        remove(pending, key);
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

    public Set<K> keySet() {
        return elements.entrySet().stream().map(Map.Entry::getKey).collect(toSet());
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

    @Override
    public String toString() {
        return "[pending=" + elements + ", pending=" + pending + "]";
    }

}