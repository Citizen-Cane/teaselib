package teaselib.core.speechrecognition;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import teaselib.core.ui.Prompt;

public class RuleIndicesList extends ArrayList<Set<Integer>> {
    private static final long serialVersionUID = 1L;

    public RuleIndicesList(Rule rule) {
        gather(rule);
    }

    public RuleIndicesList(List<HashSet<Integer>> values) {
        super(values);
    }

    private void gather(Rule rule) {
        if (!rule.choiceIndices.isEmpty() && rule.choiceIndices.stream()
                // TODO looks redundant - remove when tests succeed again
                .allMatch(choiceIndex -> choiceIndex > Prompt.Result.DISMISSED.elements.get(0))) {
            add(rule.choiceIndices);
        }
        rule.children.stream().forEach(child -> addAll(child.gather()));
    }

    public Optional<Integer> getCommonDistinctValue() {
        if (isEmpty())
            return Optional.empty();

        Set<Integer> candidates = new HashSet<>(get(0));
        for (Integer candidate : new ArrayList<>(candidates)) {
            for (int i = 1; i < size(); i++) {
                if (!get(i).contains(candidate)) {
                    candidates.remove(candidate);
                    if (candidates.isEmpty())
                        return Optional.empty();
                    else
                        break;
                }
            }
        }

        return candidates.size() == 1 ? Optional.of(candidates.iterator().next()) : Optional.empty();
    }

}
