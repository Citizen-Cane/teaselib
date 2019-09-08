package teaselib.core.speechrecognition.srgs;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public class Rule extends ArrayList<OneOf> {
    private static final long serialVersionUID = 1L;

    public final int group;
    public final int index;

    static Rule placeholderOf(Rule rule) {
        return new Rule(rule.group, rule.index);
    }

    public Rule(int group, int index) {
        this.group = group;
        this.index = index;
    }

    public Rule(int group, int index, String... items) {
        this.group = group;
        this.index = index;
        int choiceIndex = 0;
        for (String item : items) {
            add(new OneOf(Collections.singleton(choiceIndex++), item));
        }
    }

    public Rule(int group, int index, OneOf... items) {
        this(group, index, Arrays.asList(items));
    }

    public Rule(int group, int index, List<OneOf> items) {
        this.group = group;
        this.index = index;
        for (OneOf item : items) {
            add(item);
        }
    }

    @Override
    public boolean add(OneOf items) {
        Optional<OneOf> existing = stream().filter(oneOf -> items.choices.equals(oneOf.choices)).findFirst();
        if (existing.isPresent()) {
            return existing.get()
                    .addAll(items.stream().filter(item -> !existing.get().contains(item)).collect(Collectors.toList()));
        } else {
            return super.add(items);
        }
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result + index;
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (!super.equals(obj))
            return false;
        if (getClass() != obj.getClass())
            return false;
        Rule other = (Rule) obj;
        if (index != other.index)
            return false;
        return true;
    }

    int choices() {
        long count = stream().map(i -> i.choices).flatMap(Set::stream).filter(choice -> choice != Phrases.COMMON_RULE)
                .distinct().count();
        if (count > 0) {
            return (int) count;
        } else {
            return 1;
        }
    }

    public boolean isBlank() {
        return isEmpty() || stream().allMatch(OneOf::isBlank);
    }

    @Override
    public String toString() {
        return "rule group=" + group + " index=" + index + " = " + super.toString();
    }

}