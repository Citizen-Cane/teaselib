/**
 * 
 */
package teaselib.core;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Pattern;

import teaselib.core.util.WildcardPattern;

/**
 * @author Citizen-Cane
 *
 */
public class DebugResponses {
    public static final long IMMEDIATELY = 0;
    public static final long NEVER = Long.MAX_VALUE;

    private Map<String, Long> responses = new HashMap<String, Long>();

    public static class Result {
        String match;
        int index;
        long delay;

        public Result(String match, int index, long delay) {
            super();
            this.match = match;
            this.index = index;
            this.delay = delay;
        }

        @Override
        public String toString() {
            return match + " -> clicking choice " + index + " after " + delay + "ms";
        }
    }

    public void add(String match, long delaySeconds) {
        responses.put(match, delaySeconds);
    }

    Result getResponse(List<String> choices) {
        for (Entry<String, Long> entry : responses.entrySet()) {
            Pattern choice = WildcardPattern.compile(entry.getKey());
            for (int i = 0; i < choices.size(); i++) {
                if (choice.matcher(choices.get(i)).matches()) {
                    return new Result(entry.getKey(), i, entry.getValue());
                }
            }
        }

        if (choices.size() == 1) {
            return new Result("*", 0, DebugResponses.IMMEDIATELY);
        } else {
            throw new IllegalStateException("No response rule defined for " + choices);
        }
    }

}
