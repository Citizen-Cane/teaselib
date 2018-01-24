package teaselib.core.debug;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import teaselib.core.Debugger.Response;
import teaselib.core.Debugger.ResponseAction;
import teaselib.core.ui.Choice;
import teaselib.core.util.ExceptionUtil;
import teaselib.core.util.WildcardPattern;

/**
 * @author Citizen-Cane
 *
 */
public class DebugResponses {
    public static final long IMMEDIATELY = 0;
    public static final long NEVER = Long.MAX_VALUE;

    private List<ResponseAction> responses = new ArrayList<>();

    public static class Result {
        public final String match;
        public final int index;
        public final Response response;

        public Result(String match, int index, Response response) {
            super();
            this.match = match;
            this.index = index;
            this.response = response;
        }

        @Override
        public String toString() {
            return match + " -> choice " + index + " response=" + response;
        }
    }

    public void add(ResponseAction responseAction) {
        responses.add(responseAction);
    }

    public void replace(ResponseAction responseAction) {
        Pattern choice = WildcardPattern.compile(responseAction.match);
        List<ResponseAction> remove = new ArrayList<>();
        for (ResponseAction entry : responses) {
            if (choice.matcher(entry.match).matches()) {
                remove.add(entry);
            }
        }

        for (ResponseAction item : remove) {
            responses.remove(item);
        }

        responses.add(responseAction);
    }

    public Result getResponse(List<Choice> choices) {
        Result bestResult = null;
        for (ResponseAction entry : responses) {
            bestResult = getResponse(choices, entry, bestResult);
        }

        if (bestResult != null) {
            return bestResult;
        } else {
            return defaultResponse(choices);
        }
    }

    public static Result getResponse(List<Choice> choices, ResponseAction entry, Result bestResult) {
        Pattern choice = WildcardPattern.compile(entry.match);
        for (int i = 0; i < choices.size(); i++) {
            if (choice.matcher(choices.get(i).text).matches()
                    && (bestResult == null || bestResult.response == Response.Ignore)) {
                try {
                    bestResult = new Result(entry.match, i, entry.getResponse().call());
                } catch (Exception e) {
                    throw ExceptionUtil.asRuntimeException(ExceptionUtil.reduce(e));
                }
            }
        }
        return bestResult;
    }

    public Result defaultResponse(List<Choice> choices) {
        if (choices.size() == 1) {
            return new Result("*", 0, Response.Choose);
        } else {
            throw new IllegalStateException("No response rule defined for " + choices);
        }
    }

    @Override
    public String toString() {
        return responses.toString();
    }

}
