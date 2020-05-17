package teaselib.core;

import java.util.function.Supplier;

import teaselib.core.util.TypedObjectMap;

public class ScriptInteractions {
    private TypedObjectMap objects = new TypedObjectMap();

    public void add(Class<? extends ScriptInteraction> clazz, Supplier<ScriptInteraction> scriptInteraction) {
        objects.store(clazz, scriptInteraction);
    }

    public <T extends ScriptInteraction> T get(Class<? extends T> clazz) {
        return objects.get(clazz);
    }

}
