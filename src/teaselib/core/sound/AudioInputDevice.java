package teaselib.core.sound;

import javax.sound.sampled.Mixer;

/**
 * @author Citizen-Cane
 *
 */
public class AudioInputDevice extends AudioDevice<AudioInputLine> {

    public AudioInputDevice(String name, Mixer.Info mixer, Mixer.Info port) {
        super(name, mixer, port);
    }

}
