package teaselib.core.speechrecognition;

import static java.util.stream.Collectors.toSet;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public class RuleIndicesList extends ArrayList<Set<Integer>> {
    private static final long serialVersionUID = 1L;

    public RuleIndicesList(Rule rule) {
        if (rule.children.isEmpty()) {
            add(rule.indices);
        } else if (Rule.MAIN_RULE_NAME.equals(rule.name)) {
            gatherChildren(rule);
        } else {
            throw new IllegalArgumentException("Not a main rule: " + rule);
        }
    }

    public RuleIndicesList(List<Set<Integer>> values) {
        super(values);
    }

    private void gatherChildren(Rule rule) {
        rule.children.stream().forEach(child -> add(child.indices));
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
        if (isEmpty()) {
            return Collections.emptySet();
        } else {
            Iterator<Set<Integer>> iterator = this.iterator();
            Set<Integer> intersection = iterator.next();
            while (iterator.hasNext()) {
                Set<Integer> next = iterator.next();
                intersection = intersection.stream().filter(next::contains).collect(toSet());
            }
            return intersection;
        }
    }
}
