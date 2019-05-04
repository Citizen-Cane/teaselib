package teaselib.core.speechrecognition.srgs;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

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
            add(new OneOf(choiceIndex++, item));
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
        Optional<OneOf> reduce = stream().reduce((a, b) -> a.choiceIndex > b.choiceIndex ? a : b);
        if (reduce.isPresent()) {
            int choiceIndex = reduce.get().choiceIndex;
            return choiceIndex == Phrases.COMMON_RULE ? 1 : choiceIndex + 1;
        } else {
            return 1;
        }
    }

    public boolean containOptionalChoices() {
        return stream().anyMatch(
                items -> items.choiceIndex != Phrases.COMMON_RULE && items.stream().anyMatch(String::isBlank));
    }

    public boolean isCommon() {
        return stream().reduce((a, b) -> {
            return a.choiceIndex > b.choiceIndex ? a : b;
        }).orElseGet(() -> new OneOf(Phrases.COMMON_RULE)).choiceIndex == Phrases.COMMON_RULE;
    }

    @Override
    public String toString() {
        return "rule group=" + group + " index=" + index + " = " + super.toString();
    }

}