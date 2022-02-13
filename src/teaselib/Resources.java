package teaselib;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

import teaselib.core.Script;

public class Resources implements Iterable<String> {

    // TODO Don't store Script - it's just used to allow SceneBasedImages to change the Take/Pose/Scene after prompt
    // -> call from Script.showPrompt() to change Take/Pose/Scene/Set
    public final Script script;
    public final List<String> elements;
    public final Map<String, String> mapping;

    public Resources(Script script, Collection<String> elements) {
        this(script, elements.isEmpty() ? Collections.emptyList() : new ArrayList<>(elements));
    }

    public Resources(Script script, List<String> elements) {
        this.script = script;
        this.elements = elements;
        this.mapping = new HashMap<>();
        elements.forEach(element -> mapping.put(element, element));
    }

    public Resources(Script script, List<String> elements, Map<String, String> mapping) {
        this.script = script;
        this.elements = elements;
        this.mapping = mapping;
    }

    public boolean isEmpty() {
        return elements.isEmpty();
    }

    public int size() {
        return elements.size();
    }

    public boolean contains(String resource) {
        return mapping.containsKey(resource);
    }

    public void removeIf(Predicate<String> predicate) {
        elements.removeIf(predicate);
        Set<String> related = new HashSet<>();
        mapping.entrySet().removeIf(entry -> {
            if (predicate.test(entry.getKey())) {
                related.add(entry.getValue());
                return true;
            } else if (predicate.test(entry.getValue())) {
                return true;
            } else {
                return false;
            }
        });
        related.forEach(mapping::remove);
    }

    @Override
    public Iterator<String> iterator() {
        return elements.iterator();
    }

    public byte[] getBytes(String resource) throws IOException {
        return script.resources.get(resource).readAllBytes();
    }

}
