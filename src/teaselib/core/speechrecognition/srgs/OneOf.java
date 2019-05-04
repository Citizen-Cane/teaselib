package teaselib.core.speechrecognition.srgs;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class OneOf extends ArrayList<String> {
    private static final long serialVersionUID = 1L;

    final int choiceIndex;

    public OneOf(int choiceIndex) {
        this.choiceIndex = choiceIndex;
    }

    public OneOf(int choiceIndex, int capacity) {
        super(capacity);
        this.choiceIndex = choiceIndex;
    }

    public OneOf(int choiceIndex, String item) {
        this(choiceIndex, Collections.singletonList(item));
    }

    public OneOf(int choiceIndex, String... items) {
        this(choiceIndex, Arrays.asList(items));
    }

    public OneOf(int choiceIndex, List<String> items) {
        this.choiceIndex = choiceIndex;
        for (String item : items) {
            add(item);
        }
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result + choiceIndex;
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
        OneOf other = (OneOf) obj;
        if (choiceIndex != other.choiceIndex)
            return false;
        return true;
    }

    @Override
    public String toString() {
        return (choiceIndex == Phrases.COMMON_RULE ? "Common" : "choice " + choiceIndex) + " = " + super.toString();
    }

    public boolean hasOptionalParts() {
        return stream().anyMatch(String::isBlank);
    }

}