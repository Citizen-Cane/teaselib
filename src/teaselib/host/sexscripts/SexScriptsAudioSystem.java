package teaselib.host.sexscripts;

import ss.IScript;
import teaselib.core.ResourceLoader;
import teaselib.host.Host;
import teaselib.host.Host.Audio;

/**
 * @author Citizen-Cane
 *
 */
public class SexScriptsAudioSystem implements Host.AudioSystem {

    final IScript ss;

    public SexScriptsAudioSystem(IScript ss) {
        this.ss = ss;
    }

    @Override
    public Audio getSound(ResourceLoader resources, String path) {
        return new SexScriptsAudio(ss, resources, path);
    }

}
