package teaselib.core.speechrecognition.srgs;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

class SequenceLookup<T> {
    final Map<String, AtomicInteger> indices;
    final Map<String, List<Sequence<T>>> lookup;

    SequenceLookup(int size) {
        indices = new HashMap<>(size);
        lookup = new HashMap<>(size);
    }

    void scan(Sequences<T> sequences, int length) {
        sequences.stream().filter(seq -> seq.size() > 0).forEach(sequence -> {
            String key = new Sequence<>(
                    sequences.joinSequenceOperator.apply(sequence.subList(0, Math.min(length, sequence.size()))))
                            .toString().toLowerCase();
            indices.computeIfAbsent(key, t -> new AtomicInteger(0)).incrementAndGet();
            lookup.computeIfAbsent(key, t -> new ArrayList<>()).add(sequence);
        });
    }

    boolean occursInAnotherDistinctSequence(T element) {
        String key = element.toString().toLowerCase();
        AtomicInteger n = indices.get(key);
        return n != null && n.intValue() == 1
                && indices.entrySet().stream().filter(entry -> entry.getValue().intValue() > 1).anyMatch(entry -> {
                    return lookup.get(entry.getKey()).stream().anyMatch(seq -> {
                        return seq.toString().toLowerCase().contains(key);
                    });
                });
    }

    boolean occursLaterInAnotherSequence(List<T> elements) {
        String key = elements.stream().map(Objects::toString).map(String::toLowerCase).collect(Collectors.joining(" "));
        AtomicInteger n = indices.get(key);
        return n != null && n.intValue() > 1
                && indices.entrySet().stream().filter(entry -> entry.getValue().intValue() > 1).anyMatch(entry -> {
                    return lookup.get(entry.getKey()).stream().anyMatch(seq -> {
                        String lowerCase = seq.toString().toLowerCase();
                        return lowerCase.contains(key) && !lowerCase.startsWith(key);
                    });
                });
    }

}