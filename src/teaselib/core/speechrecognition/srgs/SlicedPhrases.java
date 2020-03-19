package teaselib.core.speechrecognition.srgs;

import java.util.ArrayList;
import java.util.List;

public class SlicedPhrases<T> {
    final SequencesSymbolCount<T> symbols;
    final Sequences<T> sequences;

    public SlicedPhrases(Sequences<T> phrases) {
        this.symbols = new SequencesSymbolCount<>(phrases);
        this.sequences = joinDistinctElements(phrases);
    }

    private Sequences<T> joinDistinctElements(Sequences<T> phrases) {
        for (Sequence<T> sequence : phrases) {
            for (int i = 0; i < sequence.size(); i++) {
                T t = sequence.get(i);
                if (symbols.get(t) == 1) {
                    joinDistinctElements(sequence, i, t);
                }
            }
        }
        return phrases;
    }

    private void joinDistinctElements(Sequence<T> sequence, int i, T t) {
        int j = i + 1;
        Sequence<T> distinctSymbols = null;
        while (j < sequence.size()) {
            T u = sequence.get(j);
            if (symbols.get(u) == 1) {
                if (distinctSymbols == null) {
                    distinctSymbols = new Sequence<>(t, sequence.traits);
                }
                sequence.remove(j);
                distinctSymbols.add(u);
            } else {
                break;
            }
        }
        if (distinctSymbols != null) {
            sequence.set(i, distinctSymbols.joined());
        }
    }

    public List<Sequences<T>> result() {
        List<List<Sequences<T>>> candidates = new ArrayList<>();
        candidates.add(Sequences.slice(candidates, sequences));
        return Sequences.reduce(candidates);
    }

}
