package teaselib.core.speechrecognition.srgs;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

class SequenceLookup<T> {
    final Sequences<T> sequences;
    final Map<String, AtomicInteger> startElementIndicess;
    final Map<String, List<Sequence<T>>> startElementSequences;
    final Map<String, List<Sequence<T>>> laterOccurrences;

    SequenceLookup(Sequences<T> sequences) {
        this.sequences = sequences;
        int size = sequences.size();
        this.startElementIndicess = new HashMap<>(size);
        this.startElementSequences = new HashMap<>(size);
        this.laterOccurrences = new HashMap<>(size);
    }

    void scan(int length) {
        startElementIndicess.clear();
        startElementSequences.clear();
        laterOccurrences.clear();

        sequences.stream().filter(Sequence::nonEmpty).forEach(sequence -> {
            String key = key(sequence.subList(0, Math.min(length, sequence.size())));
            startElementIndicess.computeIfAbsent(key, t -> new AtomicInteger(0)).incrementAndGet();
            startElementSequences.computeIfAbsent(key, t -> new ArrayList<>()).add(sequence);

            for (Sequence<T> seq : sequences) {
                if (sequence != seq && !seq.isEmpty() && sequenceContains(seq.subList(1), key)) {
                    laterOccurrences.computeIfAbsent(key, t -> new ArrayList<>()).add(seq);
                }
            }
        });
    }

    public void removeAndRescan(Sequence<T> sequence, T element, int length) {
        sequence.remove(element);
        scan(length);
    }

    boolean sequenceContains(List<T> sequence, String key) {
        return sequence.toString().toLowerCase().contains(key);
    }

    boolean othersStartWith(T element) {
        List<Sequence<T>> list = startElementSequences.get(key(element));
        return list != null && list.size() > 1;
    }

    boolean othersStartWith(List<T> elements) {
        List<Sequence<T>> list = startElementSequences.get(key(elements));
        return list != null && list.size() > 1;
    }

    String key(List<T> elements) {
        return elements.stream().map(this::key).collect(Collectors.joining(" "));
    }

    boolean occursInAnotherDistinctSequence(T element) {
        return laterOccurrences.containsKey(key(element));
    }

    private String key(T element) {
        return element.toString().toLowerCase();
    }

    boolean occursLaterInAnotherSequence(List<T> elements) {
        String key = key(elements);
        AtomicInteger n = startElementIndicess.get(key);
        return n != null && n.intValue() > 1 && startElementIndicess.entrySet().stream().filter(entry -> {
            return entry.getValue().intValue() > 1;
        }).anyMatch(entry -> {
            List<Sequence<T>> startElementSequencesForElements = startElementSequences.get(entry.getKey());
            return startElementSequencesForElements.stream().anyMatch(sequence -> {
                String phrase = key(sequence.subList(1));
                return phrase.contains(key);
            });
        });
    }

    public boolean hasCommonStartElements() {
        return startElementIndicess.values().stream().map(AtomicInteger::intValue).reduce(Math::max).orElse(0) > 1;
    }

}