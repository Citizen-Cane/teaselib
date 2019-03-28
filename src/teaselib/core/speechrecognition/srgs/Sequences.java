package teaselib.core.speechrecognition.srgs;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

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

}
