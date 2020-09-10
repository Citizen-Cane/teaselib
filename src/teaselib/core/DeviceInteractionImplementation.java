package teaselib.core;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiPredicate;

import teaselib.Actor;

/**
 * @author Citizen-Cane
 *
 */
public abstract class DeviceInteractionImplementation<K, V> {

    private final Map<Actor, DeviceInteractionDefinitions<K, V>> definitions = new ConcurrentHashMap<>();
    final BiPredicate<K, K> matcher;

    protected DeviceInteractionImplementation(BiPredicate<K, K> matcher) {
        this.matcher = matcher;
    }

    public boolean contains(Actor actor) {
        return definitions.containsKey(actor);
    }

    protected Set<Actor> getActors() {
        return definitions.keySet();
    }

    protected DeviceInteractionDefinitions<K, V> definitions(Actor actor) {
        return definitions.computeIfAbsent(actor, key -> new DeviceInteractionDefinitions<>(key, matcher));
    }

    protected void define(Actor actor, K key, V value) {
        definitions(actor).define(key, value);
    }

}
