package teaselib.core;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;

import teaselib.core.util.ExceptionUtil;

class ScriptInteractionCache {
    final Map<String, Map<Class<? extends ScriptInteraction>, ScriptInteraction>> scripts = new HashMap<>();

    // TODO merge duplicated code from ScriptCache

    @SuppressWarnings("unchecked")
    <T extends ScriptInteraction> T get(Script parentScript, Class<T> subScript) {
        return (T) scripts //
                .computeIfAbsent(parentScript.actor.key, key -> new HashMap<>()) //
                .computeIfAbsent(subScript, key -> newScript(parentScript, key));
    }

    static <T extends ScriptInteraction> T newScript(Script parentScript, Class<T> scriptInteraction) {
        try {
            return newInstance(parentScript, scriptInteraction);
        } catch (ReflectiveOperationException e) {
            throw ExceptionUtil.asRuntimeException(e);
        }
    }

    @SuppressWarnings("unchecked")
    static <T extends ScriptInteraction> T newInstance(Script parentScript, Class<T> scriptInteraction)
            throws NoSuchMethodException, InstantiationException, IllegalAccessException, InvocationTargetException {
        Class<? extends Script> callerClass = parentScript.getClass();

        Constructor<T>[] declaredConstructors = (Constructor<T>[]) scriptInteraction.getDeclaredConstructors();
        for (Constructor<T> constructor : declaredConstructors) {
            Class<?>[] parameterTypes = constructor.getParameterTypes();
            if (parameterTypes.length > 0 && parameterTypes[0].isAssignableFrom(callerClass)) {
                if (parameterTypes.length == 1) {
                    if (constructor.canAccess(null)) {
                        return constructor.newInstance(parentScript);
                    } else {
                        throw new IllegalAccessException("Constructor " + scriptInteraction.getName() + "("
                                + callerClass.getName() + ") inaccessible - script classes must be public");
                    }
                } else if (parameterTypes.length == 2 && parameterTypes[1].isAssignableFrom(callerClass)) {
                    if (constructor.canAccess(null)) {
                        return constructor.newInstance(parentScript, parentScript);
                    } else {
                        throw new IllegalAccessException(
                                "Constructor " + scriptInteraction.getName() + "(" + callerClass.getName()
                                        + ") inaccessible - nested script classes must be public & static ");
                    }
                }
            }
        }

        if (declaredConstructors.length == 1) {
            Constructor<T> constructor = declaredConstructors[0];
            throw new NoSuchMethodException(
                    "Constructor " + constructor + " not suitable for " + callerClass.getName());
        } else {
            throw new NoSuchMethodException("Constructor " + scriptInteraction.getName() + "(" + callerClass.getName()
                    + ") missing or inaccessible - nested script classes require the static keyword, or the script has to be a top-level class");
        }
    }
}
