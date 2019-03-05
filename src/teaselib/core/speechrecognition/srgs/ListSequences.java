package teaselib.core.speechrecognition.srgs;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class ListSequences<T> extends ArrayList<ListSequence<T>> {
    private static final long serialVersionUID = 1L;

    public ListSequences() {
        super();
    }

    @SafeVarargs
    public ListSequences(ListSequence<T>... elements) {
        super(Arrays.asList(elements));
    }

    public ListSequences(Collection<? extends ListSequence<T>> elements) {
        super(elements);
    }

    public ListSequences(int initialCapacity) {
        super(initialCapacity);
    }

    public ListSequence<T> commonStart() {
        ListSequence<T> sequence = new ListSequence<>(get(0));
        for (int i = sequence.size(); i >= 1; --i) {
            if (allStartWithSequence(sequence)) {
                return sequence;
            }
            sequence.remove(sequence.size() - 1);
        }

        return new ListSequence<>(Collections.emptyList());
    }

    private boolean allStartWithSequence(ListSequence<T> sequence) {
        for (int j = 1; j < size(); ++j) {
            if (!get(j).startsWith(sequence)) {
                return false;
            }
        }
        return true;
    }

    public ListSequence<T> commonEnd() {
        ListSequence<T> sequence = new ListSequence<>(get(0));
        for (int i = sequence.size(); i >= 1; --i) {
            if (allEndWithSequence(sequence)) {
                return sequence;
            }
            sequence.remove(0);
        }

        return new ListSequence<>(Collections.emptyList());
    }

    private boolean allEndWithSequence(ListSequence<T> sequence) {
        for (int j = 1; j < size(); ++j) {
            if (!get(j).endsWith(sequence)) {
                return false;
            }
        }
        return true;
    }

    public ListSequence<T> commonMiddle() {
        ListSequence<T> sequence = new ListSequence<>(get(0));
        List<ListSequence<T>> candidates = sequence.subLists();

        for (ListSequence<T> candidate : candidates) {
            if (allContainMiddleSequence(candidate)) {
                return new ListSequence<>(candidate);
            }
        }

        return new ListSequence<>(Collections.emptyList());
    }

    private boolean allContainMiddleSequence(ListSequence<T> sequence) {
        for (int j = 1; j < size(); ++j) {
            if (get(j).indexOf(sequence) == -1) {
                return false;
            }
        }
        return true;
    }

    public ListSequences<T> removeFrom(ListSequence<T> match) {
        ListSequences<T> subLists = new ListSequences<>(size());
        for (ListSequence<T> listSequence : this) {
            subLists.add(new ListSequence<>(listSequence.subList(0, listSequence.indexOf(match))));
        }
        return subLists;
    }

    public ListSequences<T> removeExcluding(ListSequence<T> match) {
        ListSequences<T> subLists = new ListSequences<>(size());
        for (ListSequence<T> listSequence : this) {
            subLists.add(new ListSequence<>(listSequence.subList(listSequence.indexOf(match), listSequence.size())));
        }
        return subLists;
    }

    public ListSequences<T> removeIncluding(ListSequence<T> match) {
        ListSequences<T> subLists = new ListSequences<>(size());
        for (ListSequence<T> listSequence : this) {
            subLists.add(new ListSequence<>(
                    listSequence.subList(listSequence.indexOf(match) + match.size(), listSequence.size())));
        }
        return subLists;
    }

}
