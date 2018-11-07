package teaselib.core;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

import teaselib.core.CodeCoverageDecisionCollector.DecisionList;

/**
 *
 * How does total code coverage work?
 * <p>
 * Start with empty decision list
 * <li>when the decision list ends choose 0
 * <li>Add each choice to the decision list
 * <li>For each decision, add a clone of the current decision list to the end of the work list
 * <p>
 * <li>for each decision to make, use the next higher choice
 * <li>if not available,
 * <p>
 *
 * @author Citizen-Cane
 */

public class CodeCoverageDecisionCollector implements Iterable<DecisionList> {
    public static final class DecisionList extends ArrayList<Integer> {
        public DecisionList() {
        }

        public DecisionList(DecisionList decisions, int i) {
            addAll(decisions);
            add(i);
        }

        private static final long serialVersionUID = 1L;
    }

    private List<DecisionList> coverage = new ArrayList<>();

    public CodeCoverageDecisionCollector() {
        coverage.add(new DecisionList());
    }

    @Override
    public Iterator<DecisionList> iterator() {
        return new Iterator<DecisionList>() {
            int index = 0;

            @Override
            public DecisionList next() {
                if (index >= coverage.size()) {
                    throw new NoSuchElementException();
                } else {
                    return coverage.get(index++);
                }
            }

            @Override
            public boolean hasNext() {
                return index < coverage.size();
            }

            @Override
            public String toString() {
                return String.valueOf(index);
            }

        };
    }

    public void add(DecisionList list) {
        coverage.add(list);
    }
}
