package teaselib.core;

import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiPredicate;

import teaselib.Actor;

/**
 * @author Citizen-Cane
 *
 */
public abstract class ScriptInteractionImplementation<K, V> {

    protected class Definitions {
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

        public int size() {
            return elements.size();
        }
    }

    private final Map<Actor, Definitions> definitions = new HashMap<>();
    final BiPredicate<K, K> matcher;

    protected ScriptInteractionImplementation(BiPredicate<K, K> matcher) {
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
