package teaselib.core.sound;

import javax.sound.sampled.Mixer;

import teaselib.core.Closeable;

/**
 * @author Citizen-Cane
 *
 */
public class AudioInputDevice extends AudioDevice implements Closeable {

    public AudioInputDevice(String name, Mixer.Info mixer, Mixer.Info port) {
        super(name, mixer, port);
    }

    @Override
    public void close() { /* Ignore */ }

}
