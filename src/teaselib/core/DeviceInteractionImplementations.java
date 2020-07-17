package teaselib.core;

import java.util.function.Supplier;

import teaselib.core.util.TypedObjectMap;

public class DeviceInteractionImplementations {
    private TypedObjectMap objects = new TypedObjectMap();

    public void add(Class<? extends DeviceInteractionImplementation<?, ?>> clazz,
            Supplier<DeviceInteractionImplementation<?, ?>> scriptInteraction) {
        objects.store(clazz, scriptInteraction);
    }

    public <T extends DeviceInteractionImplementation<?, ?>> T get(Class<? extends T> clazz) {
        return objects.get(clazz);
    }

}
