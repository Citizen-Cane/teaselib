package teaselib.host;

import static teaselib.core.util.ExceptionUtil.*;

import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.UnsupportedAudioFileException;

import teaselib.core.ResourceLoader;
import teaselib.core.sound.AudioOutputLine;
import teaselib.core.sound.AudioSystem;
import teaselib.host.Host.Audio;

/**
 * @author Citizen-Cane
 *
 */
public class TeaseLibAudioSystem implements Host.AudioSystem {

    AudioSystem audioSystem;

    public TeaseLibAudioSystem() {
        this.audioSystem = new AudioSystem();
    }

    @Override
    public Audio getSound(ResourceLoader resources, String path) {
        var output = audioSystem.defaultOutput();

        return new Audio() {

            InputStream audio;
            AudioOutputLine line = null;
            Future<Integer> playing = null;

            @Override
            public void load() throws IOException {
                audio = resources.get(path);
                try {
                    line = output.play(audio);
                } catch (LineUnavailableException | UnsupportedAudioFileException e) {
                    throw asRuntimeException(e);
                }
            }

            @Override
            public void set(Control control, float value) {
                switch (control) {
                case Balance:
                    line.setBalance(value);
                    break;
                case Fade:
                    throw new UnsupportedOperationException(control.name());
                case Volume:
                    line.setBalance(value);
                    break;
                default:
                    break;
                }
            }

            @Override
            public void play() throws InterruptedException {
                try {
                    playing = line.start();
                } catch (LineUnavailableException e) {
                    throw asRuntimeException(e);
                }
                try {
                    playing.get();
                } catch (CancellationException e) {
                    // Ignore
                } catch (ExecutionException e) {
                    throw asRuntimeException(e);
                }
            }

            @Override
            public void stop() {
                playing.cancel(true);
            }

            @Override
            public void close() {
                try {
                    audio.close();
                } catch (IOException e) {
                    // Ignore
                }
            }

        };
    }

}
