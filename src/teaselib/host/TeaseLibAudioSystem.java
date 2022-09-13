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
import teaselib.core.sound.AudioOutputDevice;
import teaselib.core.sound.AudioOutputLine;
import teaselib.core.sound.AudioSystem;
import teaselib.host.Host.Audio;

/**
 * @author Citizen-Cane
 *
 */
public class TeaseLibAudioSystem implements Host.AudioSystem {

    private static final class AudioImpl implements Audio {

        private final AudioOutputDevice output;
        InputStreamProvider inputStreamProvider;
        InputStream data = null;
        AudioOutputLine line = null;
        Future<Integer> playing = null;

        private AudioImpl(ResourceLoader resources, String path, AudioOutputDevice output) {
            this.output = output;
            this.inputStreamProvider = () -> resources.get(path);
        }

        private AudioImpl(InputStream inputStream, AudioOutputDevice output) {
            this.output = output;
            this.inputStreamProvider = () -> inputStream;
        }

        interface InputStreamProvider {
            InputStream get() throws IOException;
        }

        @Override
        public void load() throws IOException {
            data = inputStreamProvider.get();
            try {
                line = output.addLine(data);
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
            playing = line.start();
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
                data.close();
            } catch (IOException e) {
                // Ignore
            }
            output.remove(line);
        }

    }

    private final AudioSystem audioSystem;

    public TeaseLibAudioSystem() {
        this.audioSystem = new AudioSystem();
    }

    @Override
    public Audio getSound(ResourceLoader resources, String path) {
        return new AudioImpl(resources, path, audioSystem.defaultOutput());

    }

    @Override
    public Audio getSound(InputStream inputStream) {
        return new AudioImpl(inputStream, audioSystem.defaultOutput());
    }

}
