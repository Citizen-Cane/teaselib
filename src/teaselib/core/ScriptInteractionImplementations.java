package teaselib.core;

import java.util.function.Supplier;

import teaselib.core.util.TypedObjectMap;

public class ScriptInteractionImplementations {
    private TypedObjectMap objects = new TypedObjectMap();

    public void add(Class<? extends ScriptInteractionImplementation<?, ?>> clazz,
            Supplier<ScriptInteractionImplementation<?, ?>> scriptInteraction) {
        objects.store(clazz, scriptInteraction);
    }

    public <T extends ScriptInteractionImplementation<?, ?>> T get(Class<? extends T> clazz) {
        return objects.get(clazz);
    }

}
