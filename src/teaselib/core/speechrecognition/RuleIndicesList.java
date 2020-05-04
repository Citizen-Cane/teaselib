package teaselib.core.speechrecognition;

import static java.util.stream.Collectors.toSet;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import teaselib.core.ui.Prompt;

public class RuleIndicesList extends ArrayList<Set<Integer>> {
    private static final long serialVersionUID = 1L;

    public RuleIndicesList(Rule rule) {
        gather(rule);
    }

    public RuleIndicesList(List<Set<Integer>> values) {
        super(values);
    }

    private void gather(Rule rule) {
        if (!rule.indices.isEmpty() && rule.indices.stream()
                // TODO looks redundant - remove when tests succeed again
                .allMatch(choiceIndex -> choiceIndex > Prompt.Result.DISMISSED.elements.get(0))) {
            add(rule.indices);
        }
        rule.children.stream().forEach(child -> addAll(child.indices()));
    }

    public Optional<Integer> singleResult() {
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

    public Set<Integer> intersection() {
        Iterator<Set<Integer>> iterator = this.iterator();
        Set<Integer> intersection = iterator.next();
        while (iterator.hasNext()) {
            Set<Integer> next = iterator.next();
            intersection = intersection.stream().filter(next::contains).collect(toSet());
        }
        return intersection;
    }
}
