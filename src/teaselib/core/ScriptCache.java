package teaselib.core;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;

import teaselib.core.util.ExceptionUtil;

class ScriptCache {
    final Map<String, Map<Class<? extends Script>, Script>> scripts = new HashMap<>();

    <T extends Script> T get(Script parentScript, Class<T> subScriptClass) {
        Map<Class<? extends Script>, Script> map = computeIfAbsentNestedInstanceAware(parentScript);
        T script = computeIfAbsentNestedInstancenAware(parentScript, subScriptClass, map);
        return script;
    }

    private static <T extends Script> T computeIfAbsentNestedInstancenAware(Script parentScript,
            Class<T> subScriptClass, Map<Class<? extends Script>, Script> map) {
        @SuppressWarnings("unchecked")
        T script = (T) map.get(subScriptClass);
        if (script == null) {
            script = newScript(parentScript, subScriptClass);
            map.put(subScriptClass, script);
        }
        return script;
    }

    private Map<Class<? extends Script>, Script> computeIfAbsentNestedInstanceAware(Script parentScript) {
        String key = parentScript.actor.key;
        Map<Class<? extends Script>, Script> map = scripts.get(key);
        if (map == null) {
            map = new HashMap<>();
            scripts.put(key, map);
        }
        return map;
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
            throws NoSuchMethodException, InstantiationException, IllegalAccessException, InvocationTargetException {
        Class<? extends Script> callerClass = parentScript.getClass();

        Constructor<T>[] declaredConstructors = (Constructor<T>[]) subScript.getDeclaredConstructors();
        for (Constructor<T> constructor : declaredConstructors) {
            Class<?>[] parameterTypes = constructor.getParameterTypes();
            if (parameterTypes.length > 0 && parameterTypes[0].isAssignableFrom(callerClass)) {
                if (parameterTypes.length == 1) {
                    if (constructor.canAccess(null)) {
                        return constructor.newInstance(parentScript);
                    } else {
                        throw new IllegalAccessException("Constructor " + subScript.getName() + "("
                                + callerClass.getName() + ") inaccessible - script classes must be public");
                    }
                } else if (parameterTypes.length == 2 && parameterTypes[1].isAssignableFrom(callerClass)) {
                    if (constructor.canAccess(null)) {
                        return constructor.newInstance(parentScript, parentScript);
                    } else {
                        throw new IllegalAccessException(
                                "Constructor " + subScript.getName() + "(" + callerClass.getName()
                                        + ") inaccessible - nested script classes must be public & static ");
                    }
                }
            }
        }

        if (declaredConstructors.length == 1) {
            Constructor<T> constructor = declaredConstructors[0];
            throw new NoSuchMethodException(
                    "Constructor " + constructor + " not suitable for argument " + callerClass.getName());
        } else {
            throw new NoSuchMethodException("Constructor " + subScript.getName() + "(" + callerClass.getName()
                    + ") missing or inaccessible - nested script classes require the static keyword, or the script has to be a top-level class");
        }
    }

}