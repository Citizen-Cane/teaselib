package teaselib.util.math;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Vector;

public class Partition<K> {
    public static interface Members<K> {
        boolean similar(K item1, K item2);
    }

    public static interface Order<K> {
        K group(K item1, K item2);
    }

    public final List<Group> groups;
    final Members<K> measure;
    final Order<K> order;

    public class Group {
        K orderingElement;
        public final List<K> items = new Vector<K>();

        Group(K item) {
            items.add(item);
            orderingElement = item;
        }

        void join(Group group) {
            orderingElement = order.group(orderingElement,
                    group.orderingElement);
            items.addAll(group.items);
        }
    }

    public Partition(List<K> items, Members<K> measure, Order<K> order,
            final Comparator<K> comperator) {
        this.measure = measure;
        this.order = order;
        groups = new Vector<Group>();
        // First iteration - create initial groups
        for (K item : items) {
            boolean grouped = false;
            for (Group group : groups) {
                if (measure.similar(item, group.orderingElement)) {
                    group.join(new Group(item));
                    grouped = true;
                    break;
                }
            }
            if (!grouped) {
                groups.add(new Group(item));
            }
        }
        // Additional iterations - join groups
        boolean groupsJoined;
        do {
            groupsJoined = false;
            for (int i = groups.size() - 1; i >= 0; i--) {
                for (int j = 0; j < groups.size(); j++) {
                    if (i != j) {
                        Group groupI = groups.get(i);
                        Group groupJ = groups.get(j);
                        if (similar(groupI, groupJ)) {
                            groupJ.join(groupI);
                            groups.remove(i);
                            groupsJoined = true;
                            break;
                        }
                    }
                }
            }
        } while (groupsJoined && groups.size() > 1);
        // Sort groups, with largest ordering element first
        Comparator<Group> groupComperator = new Comparator<Group>() {
            @Override
            public int compare(Group g1, Group g2) {
                return -comperator.compare(g1.orderingElement,
                        g2.orderingElement);
            }
        };
        Collections.sort(groups, groupComperator);
    }

    private boolean similar(Group group1, Group group2) {
        for (K k : group1.items) {
            if (similarToGroup(k, group2)) {
                return true;
            }
        }
        return false;
    }

    private boolean similarToGroup(K item, Group group) {
        for (K k : group.items) {
            if (measure.similar(k, item)) {
                return true;
            }
        }
        return false;
    }
}
