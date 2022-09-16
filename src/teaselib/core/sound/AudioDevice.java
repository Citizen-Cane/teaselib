package teaselib.core.sound;

import javax.sound.sampled.Line;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.Mixer;
import javax.sound.sampled.Mixer.Info;

/**
 * @author Citizen-Cane
 *
 */
public class AudioDevice {

    protected final String name;
    protected final Info mixerInfo;
    protected final Info port;

    protected Mixer mixer;

    public AudioDevice(String name, Info mixer, Info port) {
        this.name = name;
        this.mixerInfo = mixer;
        this.port = port;
    }

    Line mixer() throws LineUnavailableException {
        if (mixer == null) {
            this.mixer = javax.sound.sampled.AudioSystem.getMixer(mixerInfo);
            this.mixer.open();
        }
        return mixer;
    }

    @Override
    public String toString() {
        return name;
    }

}
