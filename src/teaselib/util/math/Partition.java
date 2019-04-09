package teaselib.util.math;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Spliterator;
import java.util.function.Consumer;

public class Partition<K> {
    @FunctionalInterface
    public static interface Siblings<K> {
        boolean similar(K item1, K item2);
    }

    @FunctionalInterface
    public static interface Join<K> {
        K groups(K item1, K item2);
    }

    public final List<Group> groups;
    final Siblings<K> siblings;
    final Join<K> join;

    public class Group implements Iterable<K> {
        K orderingElement;
        private final List<K> items = new ArrayList<>();

        Group(K item) {
            items.add(item);
            orderingElement = item;
        }

        void join(Group group) {
            orderingElement = join.groups(orderingElement, group.orderingElement);
            items.addAll(group.items);
        }

        @Override
        public void forEach(Consumer<? super K> action) {
            items.forEach(action);
        }

        @Override
        public Spliterator<K> spliterator() {
            return items.spliterator();
        }

        @Override
        public Iterator<K> iterator() {
            return items.iterator();
        }

        @Override
        public String toString() {
            return orderingElement.toString() + "->" + items;
        }

        public K get(int index) {
            return items.get(index);
        }

        public int size() {
            return items.size();
        }
    }

    public Partition(List<K> items, Siblings<K> measure) {
        this(items, measure, (a, b) -> a, null);
    }

    public Partition(List<K> items, Siblings<K> measure, Join<K> order) {
        this(items, measure, order, null);
    }

    public Partition(List<K> items, Siblings<K> siblings, Join<K> joiner, final Comparator<K> comperator) {
        this.siblings = siblings;
        this.join = joiner;
        this.groups = new ArrayList<>();

        createInitialGroups(items);
        joinGroups();

        if (comperator != null) {
            sortGroupsByOrderingElementLargestFirst(comperator);
        }
    }

    private void createInitialGroups(List<K> items) {
        for (K item : items) {
            boolean grouped = false;
            for (Group group : groups) {
                if (siblings.similar(item, group.orderingElement)) {
                    group.join(new Group(item));
                    grouped = true;
                    break;
                }
            }
            if (!grouped) {
                groups.add(new Group(item));
            }
        }
    }

    private void joinGroups() {
        boolean groupsJoined;
        do {
            groupsJoined = false;
            for (int i = groups.size() - 1; i >= 0; i--) {
                groupsJoined = joinGroup(i);
            }
        } while (groupsJoined && groups.size() > 1);
    }

    private boolean joinGroup(int i) {
        Group groupI = groups.get(i);
        boolean groupsJoined = false;
        for (int j = 0; j < groups.size(); j++) {
            if (i != j) {
                Group groupJ = groups.get(j);
                if (similar(groupI, groupJ)) {
                    groupJ.join(groupI);
                    groups.remove(i);
                    groupsJoined = true;
                    break;
                }
            }
        }
        return groupsJoined;
    }

    private void sortGroupsByOrderingElementLargestFirst(final Comparator<K> comperator) {
        Comparator<Group> c = (Group g1, Group g2) -> -comperator.compare(g1.orderingElement, g2.orderingElement);
        Collections.sort(groups, c);
    }

    private boolean similar(Group group1, Group group2) {
        for (K k : group1) {
            if (similarToGroup(k, group2)) {
                return true;
            }
        }
        return false;
    }

    private boolean similarToGroup(K item, Group group) {
        for (K k : group) {
            if (siblings.similar(k, item)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public String toString() {
        return groups.toString();
    }
}
