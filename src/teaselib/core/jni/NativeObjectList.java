package teaselib.core.jni;

import java.util.ArrayList;

/**
 * @author Citizen-Cane
 *
 */
public class NativeObjectList<T extends NativeObject.Disposible> extends ArrayList<T>
        implements teaselib.core.Closeable {

    private static final long serialVersionUID = 1L;

    public NativeObjectList(int initialCapacity) {
        super(initialCapacity);
    }

    @Override
    public void close() {
        stream().forEach(T::close);
    }

}
