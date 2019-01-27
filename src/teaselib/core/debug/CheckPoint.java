package teaselib.core.debug;

public interface CheckPoint {
    enum ScriptFunction implements CheckPoint {
        Started,
        Finished
    }

    enum Script implements CheckPoint {
        NewMessage
    }
}
