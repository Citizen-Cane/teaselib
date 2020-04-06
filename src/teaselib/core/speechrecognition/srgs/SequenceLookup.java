package teaselib.core.speechrecognition.srgs;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicInteger;

class SequenceLookup<T> {
    final Sequences<T> sequences;
    final Map<List<T>, AtomicInteger> startElementIndicess;
    final Map<List<T>, List<Sequence<T>>> startElementSequences;
    final Map<List<T>, List<Sequence<T>>> laterOccurrences;

    SequenceLookup(Sequences<T> sequences, Comparator<List<T>> comparator) {
        this.sequences = sequences;
        this.startElementIndicess = new TreeMap<>(comparator);
        this.startElementSequences = new TreeMap<>(comparator);
        this.laterOccurrences = new TreeMap<>(comparator);
    }

    void scan(int length) {
        startElementIndicess.clear();
        startElementSequences.clear();
        laterOccurrences.clear();

        sequences.stream().filter(Sequence::nonEmpty).forEach(sequence -> {
            List<T> key = sequence.subList(0, Math.min(length, sequence.size()));
            startElementIndicess.computeIfAbsent(key, t -> new AtomicInteger(0)).incrementAndGet();
            startElementSequences.computeIfAbsent(key, t -> new ArrayList<>()).add(sequence);

            for (Sequence<T> seq : sequences) {
                if (sequence != seq && !seq.isEmpty() && seq.indexOf(key, 1) > 0) {
                    laterOccurrences.computeIfAbsent(key, t -> new ArrayList<>()).add(seq);
                }
            }
        });
    }

    public void removeAndRescan(Sequence<T> sequence, T element, int length) {
        sequence.remove(element);
        scan(length);
    }

    boolean othersStartWith(T element) {
        List<Sequence<T>> list = startElementSequences.get(Collections.singletonList(element));
        return list != null && list.size() > 1;
    }

    boolean othersStartWith(List<T> elements) {
        List<Sequence<T>> list = startElementSequences.get(elements);
        return list != null && list.size() > 1;
    }

    boolean occursInAnotherDistinctSequence(T element) {
        return laterOccurrences.containsKey(Collections.singletonList(element));
    }

    boolean occursLaterInAnotherSequence(List<T> elements) {
        List<T> key = elements;
        AtomicInteger n = startElementIndicess.get(key);
        return n != null && n.intValue() > 1 && startElementIndicess.entrySet().stream().filter(entry -> {
            return entry.getValue().intValue() > 1;
        }).anyMatch(entry -> {
            List<Sequence<T>> startElementSequencesForElements = startElementSequences.get(entry.getKey());
            return startElementSequencesForElements.stream().anyMatch(sequence -> {
                return sequence.indexOf(key, 1) > 0;
            });
        });
    }

    public boolean hasCommonStartElements() {
        return startElementIndicess.values().stream().map(AtomicInteger::intValue).reduce(Math::max).orElse(0) > 1;
    }

}