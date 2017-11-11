package teaselib.core;

import java.io.IOException;

/**
 * @author Citizen-Cane
 *
 */
public interface Audio {
    enum Mode {
        Synchronous,
        Background
    }

    void load() throws IOException;

    void play() throws InterruptedException;

    void stop();
}
