package teaselib.core.speechrecognition;

import static java.util.stream.Collectors.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public class RuleIndicesList extends ArrayList<Set<Integer>> {
    private static final long serialVersionUID = 1L;

    public static RuleIndicesList singleton(Set<Integer> indices) {
        return new RuleIndicesList(Collections.singletonList(indices));
    }

    public static RuleIndicesList of(Rule rule) {
        return RuleIndicesList.of(rule.children);
    }

    public static RuleIndicesList of(List<Rule> rules) {
        return new RuleIndicesList(rules.stream().map(r -> r.indices).collect(toList()));
    }

    public RuleIndicesList(List<Set<Integer>> indices) {
        super(indices);
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
        return intersection(this);
    }

    public static Set<Integer> intersection(List<Set<Integer>> indices) {
        if (indices.isEmpty()) {
            return Collections.emptySet();
        } else {
            Iterator<Set<Integer>> iterator = indices.iterator();
            Set<Integer> intersection = iterator.next();
            while (iterator.hasNext()) {
                Set<Integer> next = iterator.next();
                // TODO map(Set::retain)
                intersection = intersection.stream().filter(next::contains).collect(toSet());
            }
            return intersection;
        }
    }

    public Set<Integer> union() {
        return stream().flatMap(Set::stream).collect(toSet());
    }

}
