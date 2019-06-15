package teaselib.core.speechrecognition.srgs;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

public class OneOf extends ArrayList<String> {
    private static final long serialVersionUID = 1L;

    public final Set<Integer> choices;

    public OneOf(Set<Integer> choices, int capacity) {
        super(capacity);
        this.choices = choices;
    }

    public OneOf(Set<Integer> choices, String item) {
        this(choices, Collections.singletonList(item));
    }

    public OneOf(Set<Integer> choices, List<String> items) {
        this.choices = choices;
        for (String item : items) {
            add(item);
        }
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result + ((choices == null) ? 0 : choices.hashCode());
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
        if (choices == null) {
            if (other.choices != null)
                return false;
        } else if (!choices.equals(other.choices))
            return false;
        return true;
    }

    public boolean isCommon() {
        if (choices.size() == 1 && choices.contains(Phrases.COMMON_RULE)) {
            return true;
        } else {
            return choices.size() > 1;
        }
    }

    public boolean isBlank() {
        return isEmpty() || stream().allMatch(String::isBlank);
    }

    @Override
    public String toString() {
        if (isCommon()) {
            return "Common" + (choices.size() > 1 ? choices.toString() : "") + " = " + super.toString();
        } else {
            return "choice " + choices + " = " + super.toString();
        }
    }

    public boolean hasOptionalParts() {
        return stream().anyMatch(String::isBlank);
    }

}