package teaselib.core.speechrecognition.srgs;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicInteger;

class SequenceLookupCommon<T> {
    final Sequences<T> sequences;
    final Map<List<T>, AtomicInteger> startElementIndices;
    final Map<List<T>, List<Sequence<T>>> startElementSequences;
    final Map<List<T>, List<Sequence<T>>> laterOccurrences;

    SequenceLookupCommon(Sequences<T> sequences, Comparator<List<T>> comparator) {
        this.sequences = sequences;
        this.startElementIndices = new TreeMap<>(comparator);
        this.startElementSequences = new TreeMap<>(comparator);
        this.laterOccurrences = new TreeMap<>(comparator);
    }

    void scan(int length) {
        startElementIndices.clear();
        startElementSequences.clear();
        laterOccurrences.clear();

        sequences.stream().filter(Sequence::nonEmpty).forEach(sequence -> {
            List<T> key = sequence.subList(0, Math.min(length, sequence.size()));
            startElementIndices.computeIfAbsent(key, t -> new AtomicInteger(0)).incrementAndGet();
            startElementSequences.computeIfAbsent(key, t -> new ArrayList<>()).add(sequence);

            for (Sequence<T> seq : sequences) {
                if (sequence != seq && !seq.isEmpty() && seq.size() > 1 && seq.indexOf(key, 1) > 0) {
                    laterOccurrences.computeIfAbsent(key, t -> new ArrayList<>()).add(seq);
                }
            }
        });
    }

    boolean othersStartWith(List<T> elements) {
        List<Sequence<T>> list = startElementSequences.get(elements);
        return list != null && list.size() > 1;
    }

    // Seems to be the correct solution but is slow and doesn't provide any better results (yet)
    // - candidates produced by this condition are likely also produced by the preceding disjunct slice
    boolean occursLaterInAnotherSequence_slow_but_correct(List<T> commonPhrase) {
        List<T> key = commonPhrase;
        AtomicInteger n = startElementIndices.get(key);
        if (n == null) {
            throw new NoSuchElementException(commonPhrase.toString());
        }

        List<Sequence<T>> occurrences = laterOccurrences.get(key);
        if (occurrences == null) {
            return false;
        }

        for (int i = 0; i < occurrences.size(); i++) {
            Sequence<T> sequence = occurrences.get(i);
            if (!sequence.startsWith(key) && sequence.indexOf(key, 1) > 0) {
                return true;
            }
        }

        return false;
    }

    // just searches those sequences for later occurrences of "elements"
    // whose start elements are common
    // - does the trick (so far) and results in 3 times faster slicing (for the unit tests)
    boolean occursLaterInAnotherSequence(List<T> commonPhrase) {
        List<T> key = commonPhrase;
        AtomicInteger n = startElementIndices.get(key);
        if (n == null) {
            throw new NoSuchElementException(commonPhrase.toString());
        }

        // filtering disjunct elements makes slicing quick
        // -> consider only sequences that have common start elements, ignore disjunct
        return startElementIndices.entrySet().stream().filter(entry -> entry.getValue().intValue() > 1)
                .anyMatch(entry -> {
                    List<Sequence<T>> startElementSequencesForElements = startElementSequences.get(entry.getKey());
                    return startElementSequencesForElements.stream().anyMatch(sequence -> sequence.indexOf(key, 1) > 0);
                });
    }

}
