package teaselib.core.speechrecognition.srgs;

import static java.util.stream.Collectors.*;

import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.Map.Entry;
import java.util.function.Predicate;

class SymbolDistances<T> {
    final Sequences<T> sequences;
    final Map<T, Integer> distances;

    public SymbolDistances(Sequences<T> sequences) {
        this.sequences = sequences;
        this.distances = new TreeMap<>(sequences.traits.comparator);

        Set<T> symbols = sequences.stream().filter(Predicate.not(Sequence::isEmpty)).map(s -> s.get(0))
                .collect(toCollection(() -> new TreeSet<>(sequences.traits.comparator)));

        for (int i = 0; i < sequences.size(); i++) {
            Sequence<T> sequence = sequences.get(i);
            if (sequence != null && sequence.size() > 1) {
                for (int k = 1; k < sequence.size(); k++) {
                    T symbol = sequence.get(k);
                    if (symbols.contains(symbol)) {
                        Integer distance = Integer.valueOf(k);
                        distances.compute(symbol,
                                (key, value) -> value != null ? Math.min(value, distance) : distance);
                    }
                }
            }
        }

    }

    Map<Integer, Set<T>> groups() {
        return distances.entrySet().stream()
                .collect(groupingBy(Entry<T, Integer>::getValue, mapping(Entry<T, Integer>::getKey,
                        toCollection(() -> new TreeSet<>(sequences.traits.comparator)))));
    }

    @Override
    public String toString() {
        return distances.toString();
    }

}