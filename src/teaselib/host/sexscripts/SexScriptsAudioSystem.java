package teaselib.host.sexscripts;

import java.io.InputStream;

import ss.IScript;
import teaselib.host.Host;
import teaselib.host.Host.Audio;
import teaselib.host.Host.Audio.Type;

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
    public Audio getSound(Type type, InputStream inputStream) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void close() { /* Ignore */ }

}
