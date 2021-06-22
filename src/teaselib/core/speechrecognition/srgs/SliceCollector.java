package teaselib.core.speechrecognition.srgs;

import static java.util.Arrays.asList;
import static java.util.stream.Collectors.toList;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.Supplier;
import java.util.stream.Stream;

class SliceCollector<T> {
    final Sequences<T> sequences;
    boolean modified = false;
    boolean isEmpty = true;

    public SliceCollector(int size, Sequence.Traits<T> traits) {
        this.sequences = new Sequences<>(traits);
        for (int i = 0; i < size; i++) {
            sequences.add(null);
        }
    }

    public SliceCollector(int size, Sequence.Traits<T> traits, Supplier<Sequence<T>> supplier) {
        this.sequences = new Sequences<>(traits);
        for (int i = 0; i < size; i++) {
            sequences.add(supplier.get());
        }
    }

    public SliceCollector(Sequences<T> sequences) {
        this.sequences = sequences;
    }

    public int size() {
        return sequences.size();
    }

    public void add(T element) {
        throw new UnsupportedOperationException("Add " + element);
    }

    public void add(int index, T element) {
        sequences.get(index, () -> new Sequence<>(sequences.traits)).add(element);
        modified = true;
        isEmpty = sequences.isEmpty();
    }

    public Sequence<T> get(int index) {
        return sequences.get(index);
    }

    public void set(int index, Sequence<T> sequence) {
        sequences.set(index, sequence);
        modified = true;
        isEmpty = sequences.isEmpty();
    }

    public Stream<Sequence<T>> stream() {
        return sequences.stream();
    }

    Sequences<T> slice() {
        List<Sequence<T>> elements = sequences.stream().filter(Objects::nonNull).filter(Sequence::nonEmpty)
                .map(element -> new Sequence<>(element, sequences.traits)).collect(toList());
        return new Sequences<>(elements, sequences.traits);
    }

    Sequences<T> gather() {
        return new Sequences<>(distinct().values(), sequences.traits);
    }

    private Map<Sequence<T>, Sequence<T>> distinct() {
        Map<Sequence<T>, Sequence<T>> reduced = new TreeMap<>(sequences.traits.listComparator);
        int size = sequences.size();
        for (int i = 0; i < size; i++) {
            Sequence<T> elements = sequences.get(i);
            if (!elements.isEmpty()) {
                Sequence<T> key = elements;
                List<T> existing = reduced.get(key);
                if (existing != null) {
                    Sequence<T> joinedElements = new Sequence<>(sequences.traits);
                    int existingSize = existing.size();
                    for (int j = 0; j < existingSize; j++) {
                        joinedElements.add(
                                sequences.traits.joinCommonOperator.apply(asList(elements.get(j), existing.get(j))));
                    }
                    reduced.put(key, joinedElements);
                } else {
                    reduced.put(key, elements);
                }
            }
        }
        return reduced;
    }

    SliceCollector<T> without(Set<T> symbols) {
        int size = sequences.size();
        var disjunctWithoutLaterOccurrences = new SliceCollector<>(size, sequences.traits);
        for (int i = 0; i < size; i++) {
            Sequence<T> sequence = sequences.get(i);
            if (sequence != null) {
                var disjunctElement = sequence.get(0);
                if (!symbols.contains(disjunctElement)) {
                    disjunctWithoutLaterOccurrences.add(i, disjunctElement);
                }
            }
        }
        return disjunctWithoutLaterOccurrences;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + (isEmpty ? 1231 : 1237);
        result = prime * result + (modified ? 1231 : 1237);
        result = prime * result + ((sequences == null) ? 0 : sequences.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        @SuppressWarnings("unchecked")
        var other = (SliceCollector<T>) obj;
        if (isEmpty != other.isEmpty)
            return false;
        if (sequences == null) {
            if (other.sequences != null)
                return false;
        } else if (!sequences.equals(other.sequences))
            return false;
        return true;
    }

    @Override
    public String toString() {
        return sequences.toString();
    }

}