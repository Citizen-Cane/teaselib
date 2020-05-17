package teaselib.core;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;

import teaselib.core.util.ExceptionUtil;

class ScriptCache {
    final Map<String, Map<Class<? extends Script>, Script>> scripts = new HashMap<>();

    @SuppressWarnings("unchecked")
    <T extends Script> T get(Script parentScript, Class<T> subScript) {
        return (T) scripts //
                .computeIfAbsent(parentScript.actor.key, key -> new HashMap<>()) //
                .computeIfAbsent(subScript, key -> newScript(parentScript, key));
    }

    static <T extends Script> T newScript(Script parentScript, Class<T> subScript) {
        try {
            return newInstance(parentScript, subScript);
        } catch (ReflectiveOperationException e) {
            throw ExceptionUtil.asRuntimeException(e);
        }
    }

    @SuppressWarnings("unchecked")
    static <T extends Script> T newInstance(Script parentScript, Class<T> subScript)
            throws InstantiationException, IllegalAccessException, InvocationTargetException {
        Class<? extends Script> callerClass = parentScript.getClass();
        Class<?> type = callerClass;

        while (type != null) {
            for (Constructor<T> constructor : (Constructor<T>[]) subScript.getDeclaredConstructors()) {
                Class<?>[] parameterTypes = constructor.getParameterTypes();
                if (parameterTypes.length > 0 && parameterTypes[0].isAssignableFrom(callerClass)) {
                    if (parameterTypes.length == 1) {
                        return constructor.newInstance(parentScript);
                    } else if (parameterTypes.length == 2 && parameterTypes[1].isAssignableFrom(callerClass)) {
                        return constructor.newInstance(parentScript, parentScript);
                    }
                }
            }
            type = type.getSuperclass();
        }

        throw new NoSuchMethodError("Constructor " + subScript.getName() + "(" + callerClass.getName() + ")");
    }

}