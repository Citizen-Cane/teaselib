package teaselib.core;

import java.util.concurrent.TimeUnit;

public class Debugger {
    private final TeaseLib teaseLib;

    public Debugger(TeaseLib teaseLib) {
        this.teaseLib = teaseLib;
    }

    public void freezeTime() {
        teaseLib.freezeTime();
    }

    public void resumeTime() {
        teaseLib.resumeTime();
    }

    public void advanceTime(long duration, TimeUnit unit) {
        teaseLib.advanceTime(duration, unit);
    }
}
