package teaselib.core;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

import teaselib.core.CodeCoverageDecisionCollector.DecisionList;
import teaselib.core.ui.Prompt;

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
    public static final class DecisionList extends ArrayList<DecisionList.Entry> {
        class Entry {
            final int result;
            final Prompt prompt;

            Entry(int n, Prompt prompt) {
                super();
                this.result = n;
                this.prompt = prompt;
            }

            @Override
            public String toString() {
                return prompt.choices.toText() + "-> result=" + result;
            }

        }

        public DecisionList() {
        }

        public DecisionList(DecisionList decisions, int i, Prompt prompt) {
            super.addAll(decisions);
            super.add(new Entry(i, prompt));
        }

        private static final long serialVersionUID = 1L;

        public void add(int result, Prompt prompt) {
            super.add(new Entry(result, prompt));
        }
    }

    List<DecisionList> coverage = new ArrayList<>();

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
