package teaselib.host;

import static teaselib.core.util.ExceptionUtil.*;

import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.UnsupportedAudioFileException;

import teaselib.core.concurrency.NoFuture;
import teaselib.core.sound.AudioOutputDevice;
import teaselib.core.sound.AudioOutputLine;
import teaselib.core.sound.AudioSystem;
import teaselib.host.Host.Audio;
import teaselib.host.Host.Audio.Type;

/**
 * @author Citizen-Cane
 *
 */
public class TeaseLibAudioSystem implements Host.AudioSystem {

    private static final class AudioImpl implements Audio {

        InputStream audio = null;
        AudioOutputLine line = null;
        Future<Integer> playing = NoFuture.Integer;

        private AudioImpl(Audio.Type type, InputStream audio, AudioOutputDevice output) throws IOException {
            this.audio = audio;
            try {
                line = output.getLine(type, audio);
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
                // Excepted & ignored
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

    }

    private final AudioSystem audioSystem;

    public TeaseLibAudioSystem() {
        this.audioSystem = new AudioSystem();
    }

    @Override
    public Audio getSound(Type type, InputStream inputStream) throws IOException {
        return new AudioImpl(type, inputStream, audioSystem.defaultOutput());
    }

    @Override
    public void close() {
        audioSystem.defaultOutput().close();
    }

}
