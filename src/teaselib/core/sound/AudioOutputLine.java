package teaselib.core.sound;

import java.io.IOException;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.FloatControl;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.Mixer.Info;
import javax.sound.sampled.SourceDataLine;

import teaselib.core.Closeable;
import teaselib.core.concurrency.AbstractFuture;
import teaselib.core.concurrency.NoFuture;
import teaselib.core.util.ExceptionUtil;

/**
 * @author Citizen-Cane
 *
 */
public class AudioOutputLine implements Closeable {

    private final SourceDataLine line;
    private final ExecutorService executor;

    private AudioInputStream stream;
    private boolean stereoUpmix;
    private Future<Integer> future = NoFuture.Integer;

    /**
     * @param stereoFormat
     *            The audio format for the source data line.
     * @param mixer
     *            The mixer for this audio line.
     * @param streamingExecutor
     *            Executor for streaming the audio data to the source line
     * @throws LineUnavailableException
     */
    public AudioOutputLine(AudioFormat format, Info mixer, ExecutorService streamingExecutor)
            throws LineUnavailableException {
        this.line = javax.sound.sampled.AudioSystem.getSourceDataLine(format, mixer);
        line.open();
        this.executor = streamingExecutor;
    }

    /**
     * @param format
     *            Indicates whether this AudioLine's format matches the one specified.
     * @return {@code true} if this format matches the one specified, {@code false} otherwise
     * @see {@link AudioFormat#matches}
     */
    public boolean matches(AudioFormat format) {
        return format.matches(line.getFormat());
    }

    /**
     * @param newStream
     *            A pcm mono or stereo stream. If the line has been used before, check if the stream {@link #matches}
     *            the audio line format.
     */
    public void load(AudioInputStream newStream) {
        if (this.stream != null) {
            try {
                stream.close();
            } catch (IOException e) {
                // Ignore
            }
        }
        this.stream = newStream;
        this.stereoUpmix = line.getFormat().getChannels() == 2 && this.stream.getFormat().getChannels() == 1;
    }

    public void setVolume(float value) {
        if (line.isControlSupported(FloatControl.Type.VOLUME)) {
            var volume = (FloatControl) line.getControl(FloatControl.Type.VOLUME);
            volume.setValue(value);
        }
    }

    public void setBalance(float value) {
        if (line.isControlSupported(FloatControl.Type.BALANCE)) {
            // Does not work for mono lines
            // http://jsresources.sourceforge.net/faq_audio.html#balance_vs_pan
            var balance = (FloatControl) line.getControl(FloatControl.Type.BALANCE);
            balance.setValue(value);
        } else if (line.isControlSupported(FloatControl.Type.PAN)) {
            // Does not work for mono lines
            // http://jsresources.sourceforge.net/faq_audio.html#controls_of_mono_line
            var balance = (FloatControl) line.getControl(FloatControl.Type.PAN);
            balance.setValue(value);
        }
    }

    public Future<Integer> start() {
        line.start();
        future = new AbstractFuture<>(executor.submit(this::stream)) {
            @Override
            public boolean cancel(boolean interrupt) {
                boolean result = future.cancel(interrupt);
                line.stop();
                line.flush();
                return result;
            }
        };
        return future;
    }

    boolean isActive() {
        return line.isActive();
    }

    void awaitCompletion() throws InterruptedException {
        try {
            future.get();
        } catch (CancellationException e) {
            // Ignore
        } catch (ExecutionException e) {
            throw ExceptionUtil.asRuntimeException(e);
        }

    }

    private static final int FrameSize = 256;

    @SuppressWarnings("null")
    private int stream() throws IOException {
        byte[] samples = new byte[FrameSize];
        byte[] processed = stereoUpmix ? new byte[FrameSize * 2] : null;
        int total = 0;
        try {
            boolean interrupted = false;
            while (!(interrupted = Thread.currentThread().isInterrupted())) {
                int numBytesRead = stream.read(samples, 0, samples.length);
                if (numBytesRead <= 0)
                    break;
                total += numBytesRead;
                if (stereoUpmix) {
                    int i = 0, j = 0;
                    while (i < samples.length) {
                        byte b1 = samples[i++];
                        byte b2 = samples[i++];
                        processed[j++] = b1;
                        processed[j++] = b2;
                        processed[j++] = b1;
                        processed[j++] = b2;
                    }
                    line.write(processed, 0, numBytesRead * 2);
                } else {
                    line.write(samples, 0, numBytesRead);
                }
            }
            if (!interrupted) {
                line.drain();
            }
        } finally {
            line.stop();
            line.flush();
        }
        return total;
    }

    @Override
    public void close() {
        line.close();
    }

}
