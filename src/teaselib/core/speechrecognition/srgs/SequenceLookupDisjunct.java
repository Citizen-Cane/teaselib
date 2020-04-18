package teaselib.core.speechrecognition.srgs;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicInteger;

class SequenceLookupDisjunct<T> {
    private final Sequences<T> sequences;
    final int[] advance;
    private final Map<T, AtomicInteger> startElementIndices;
    private final Map<T, List<Sequence<T>>> startElementSequence;
    private final Map<T, List<Sequence<T>>> laterOccurrences;

    SequenceLookupDisjunct(Sequences<T> sequences) {
        this.sequences = sequences;
        this.advance = new int[sequences.size()];
        this.startElementIndices = new TreeMap<>(sequences.traits.comparator);
        this.startElementSequence = new TreeMap<>(sequences.traits.comparator);
        this.laterOccurrences = new TreeMap<>(sequences.traits.comparator);
    }

    void scan() {
        startElementIndices.clear();
        startElementSequence.clear();
        laterOccurrences.clear();

        for (int k = 0; k < sequences.size(); k++) {
            Sequence<T> sequence = sequences.get(k);
            int a = advance[k];
            if (sequence.size() > a) {
                T key = sequence.get(a);
                startElementIndices.computeIfAbsent(key, t -> new AtomicInteger(0)).incrementAndGet();
                startElementSequence.computeIfAbsent(key, t -> new ArrayList<>()).add(sequence);

                for (int i = 0; i < sequences.size(); i++) {
                    Sequence<T> seq = sequences.get(i);
                    if (sequence != seq && !seq.isEmpty() && seq.size() > a + 1 && seq.indexOf(key, a + 1) > 0) {
                        laterOccurrences.computeIfAbsent(key, t -> new ArrayList<>()).add(seq);
                    }
                }
            }
        }
    }

    void advance(int index) {
        advance[index]++;
        scan();
    }

    boolean othersStartWith(T element) {
        List<Sequence<T>> list = startElementSequence.get(element);
        return list != null && list.size() > 1;
    }

    boolean occursLaterInAnotherSequence(Sequence<T> sequence, T element) {
        List<Sequence<T>> occurrences = laterOccurrences.get(element);
        if (occurrences == null) {
            return false;
        }

        for (int i = 0; i < occurrences.size(); i++) {
            Sequence<T> seq = occurrences.get(i);
            if (seq != sequence && seq.indexOf(element, advance[i]) > 0) {
                return true;
            }
        }

        return false;
    }

    public boolean hasCommonStartElements() {
        return startElementIndices.values().stream().map(AtomicInteger::intValue).reduce(Math::max).orElse(0) > 1;
    }

}