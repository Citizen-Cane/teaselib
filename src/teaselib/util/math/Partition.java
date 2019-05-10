package teaselib.util.math;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Spliterator;
import java.util.function.Consumer;

public class Partition<K> implements Iterable<Partition<K>.Group> {
    @FunctionalInterface
    public static interface Similarity<K> {
        boolean similar(K item1, K item2);
    }

    @FunctionalInterface
    public static interface Join<K> {
        K groups(K item1, K item2);
    }

    public final List<Group> groups;
    final Similarity<K> similarity;
    final Join<K> join;

    public class Group implements Iterable<K> {
        public final List<K> items;
        private final List<K> writableItems;
        private K orderingElement;

        Group(K item) {
            this.writableItems = new ArrayList<>();
            this.items = Collections.unmodifiableList(writableItems);
            writableItems.add(item);
            orderingElement = item;
        }

        void join(Group group) {
            orderingElement = join.groups(orderingElement, group.orderingElement);
            writableItems.addAll(group.items);
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

    public Partition(List<K> items, Similarity<K> similarity) {
        this(items, similarity, (a, b) -> a, null);
    }

    public Partition(List<K> items, Similarity<K> similarity, Join<K> joiner) {
        this(items, similarity, joiner, null);
    }

    public Partition(List<K> items, Similarity<K> similarity, Join<K> joiner, Comparator<K> comperator) {
        this.similarity = similarity;
        this.join = joiner;
        this.groups = Collections.unmodifiableList(partitionOf(items, comperator));
    }

    private List<Group> partitionOf(List<K> items, Comparator<K> comperator) {
        List<Group> partitions = createInitialGroups(items);
        if (comperator != null) {
            return sortByOrderingElementLargestFirst(join(partitions), comperator);
        } else {
            return partitions;
        }
    }

    private List<Group> createInitialGroups(List<K> items) {
        List<Group> initialGroups = new ArrayList<>();
        for (K item : items) {
            boolean grouped = false;
            for (Group group : initialGroups) {
                if (similarity.similar(item, group.orderingElement)) {
                    group.join(new Group(item));
                    grouped = true;
                    break;
                }
            }
            if (!grouped) {
                initialGroups.add(new Group(item));
            }
        }
        return initialGroups;
    }

    private List<Group> join(List<Group> groups) {
        boolean groupsJoined;
        do {
            groupsJoined = false;
            for (int i = groups.size() - 1; i >= 0; i--) {
                groupsJoined = join(groups, i);
            }
        } while (groupsJoined && groups.size() > 1);
        return groups;
    }

    private boolean join(List<Group> groups, int i) {
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

    private List<Group> sortByOrderingElementLargestFirst(List<Group> groups, Comparator<K> comperator) {
        Comparator<Group> c = (Group g1, Group g2) -> -comperator.compare(g1.orderingElement, g2.orderingElement);
        Collections.sort(groups, c);
        return groups;
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
            if (similarity.similar(k, item)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public Iterator<Group> iterator() {
        return groups.iterator();
    }

    @Override
    public String toString() {
        return groups.toString();
    }
}
