package teaselib.core.speechrecognition.srgs;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class Sequences<T> extends ArrayList<Sequence<T>> {
    private static final long serialVersionUID = 1L;

    public Sequences() {
        super();
    }

    @SafeVarargs
    public Sequences(Sequence<T>... elements) {
        super(Arrays.asList(elements));
    }

    public Sequences(Collection<? extends Sequence<T>> elements) {
        super(elements);
    }

    public Sequences(int initialCapacity) {
        super(initialCapacity);
    }

    public Sequence<T> commonStart() {
        Sequence<T> sequence = new Sequence<>(get(0));
        for (int i = sequence.size(); i >= 1; --i) {
            if (allStartWithSequence(sequence)) {
                return sequence;
            }
            sequence.remove(sequence.size() - 1);
        }

        return new Sequence<>(Collections.emptyList());
    }

    private boolean allStartWithSequence(Sequence<T> sequence) {
        for (int j = 1; j < size(); ++j) {
            if (!get(j).startsWith(sequence)) {
                return false;
            }
        }
        return true;
    }

    public Sequence<T> commonEnd() {
        Sequence<T> sequence = new Sequence<>(get(0));
        for (int i = sequence.size(); i >= 1; --i) {
            if (allEndWithSequence(sequence)) {
                return sequence;
            }
            sequence.remove(0);
        }

        return new Sequence<>(Collections.emptyList());
    }

    private boolean allEndWithSequence(Sequence<T> sequence) {
        for (int j = 1; j < size(); ++j) {
            if (!get(j).endsWith(sequence)) {
                return false;
            }
        }
        return true;
    }

    public Sequence<T> commonMiddle() {
        Sequence<T> sequence = new Sequence<>(get(0));
        List<Sequence<T>> candidates = sequence.subLists();

        for (Sequence<T> candidate : candidates) {
            if (allContainMiddleSequence(candidate)) {
                return new Sequence<>(candidate);
            }
        }

        return new Sequence<>(Collections.emptyList());
    }

    private boolean allContainMiddleSequence(Sequence<T> sequence) {
        for (int j = 1; j < size(); ++j) {
            if (get(j).indexOf(sequence) == -1) {
                return false;
            }
        }
        return true;
    }

    public Sequences<T> removeFrom(Sequence<T> match) {
        Sequences<T> subLists = new Sequences<>(size());
        for (Sequence<T> listSequence : this) {
            subLists.add(new Sequence<>(listSequence.subList(0, listSequence.indexOf(match))));
        }
        return subLists;
    }

    public Sequences<T> removeExcluding(Sequence<T> match) {
        Sequences<T> subLists = new Sequences<>(size());
        for (Sequence<T> listSequence : this) {
            subLists.add(new Sequence<>(listSequence.subList(listSequence.indexOf(match), listSequence.size())));
        }
        return subLists;
    }

    public Sequences<T> removeIncluding(Sequence<T> match) {
        Sequences<T> subLists = new Sequences<>(size());
        for (Sequence<T> listSequence : this) {
            subLists.add(new Sequence<>(
                    listSequence.subList(listSequence.indexOf(match) + match.size(), listSequence.size())));
        }
        return subLists;
    }

    public int maxLength() {
        Optional<? extends Sequence<T>> reduced = this.stream().reduce((a, b) -> a.size() > b.size() ? a : b);
        return reduced.isPresent() ? reduced.get().size() : 0;
    }

    public List<String> toStrings() {
        return stream().map(Sequence::toString).collect(Collectors.toList());
    }

    public static <T> Sequences<T> flatten(List<List<Sequence<T>>> sequences) {
        int size = SequenceUtil.max(sequences);
        Sequences<T> flattened = new Sequences<>(size);
        for (int i = 0; i < size; i++) {
            Sequence<T> flat = new Sequence<>();

            for (List<Sequence<T>> elements : sequences) {
                if (elements.size() == 1) {
                    flat.addAll(elements.get(0));
                } else {
                    flat.addAll(elements.get(i));
                }
            }

            flattened.add(flat);
        }
        return flattened;
    }

}
