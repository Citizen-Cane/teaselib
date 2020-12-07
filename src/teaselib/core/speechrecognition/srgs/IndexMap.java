package teaselib.core.speechrecognition.srgs;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class IndexMap<T> {
    private final List<T> values;

    public IndexMap() {
        values = new ArrayList<>();
    }

    public int add(T value) {
        values.add(value);
        return values.size() - 1;
    }

    public T get(int index) {
        return values.get(index);
    }

    public Set<T> get(Set<Integer> indices) {
        return indices.stream().map(this::get).collect(Collectors.toSet());
    }

    @Override
    public String toString() {
        return values.toString();
    }

}
