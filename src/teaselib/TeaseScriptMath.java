package teaselib;

import java.util.List;
import java.util.concurrent.TimeUnit;

import teaselib.core.ResourceLoader;
import teaselib.core.Script;
import teaselib.core.ScriptInterruptedException;
import teaselib.core.TeaseLib;
import teaselib.core.TimeOfDay;
import teaselib.util.Daytime;

/**
 * @author Citizen-Cane
 *
 */
public abstract class TeaseScriptMath extends TeaseScriptPersistence {

    protected TeaseScriptMath(TeaseLib teaseLib, ResourceLoader resources, Actor actor, String namespace) {
        super(teaseLib, resources, actor, namespace);
    }

    protected TeaseScriptMath(Script script, Actor actor) {
        super(script, actor);
    }

    public Message message(String... text) {
        if (text == null)
            return null;
        if (text.length == 0)
            return new Message(actor);
        return new Message(actor, text);
    }

    public Message message(List<String> text) {
        if (text == null)
            return null;
        return new Message(actor, text);
    }

    public void sleep(long duration, TimeUnit timeUnit) {
        try {
            teaseLib.sleep(duration, timeUnit);
        } catch (InterruptedException e) {
            throw new ScriptInterruptedException(e);
        }
    }

    public Duration duration() {
        return teaseLib.duration();
    }

    public Duration duration(long limit, TimeUnit unit) {
        return teaseLib.duration(limit, unit);
    }

    public Duration duration(Daytime dayTime) {
        return teaseLib.duration(dayTime);
    }

    public Duration duration(Daytime dayTime, long daysInTheFuture) {
        return teaseLib.duration(dayTime, daysInTheFuture);
    }

    public TimeOfDay timeOfDay() {
        return teaseLib.timeOfDay();
    }

}
