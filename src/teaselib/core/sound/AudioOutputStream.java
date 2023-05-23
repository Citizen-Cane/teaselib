package teaselib.core.sound;

import java.io.IOException;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.FloatControl;

import teaselib.core.Closeable;
import teaselib.core.concurrency.AbstractFuture;
import teaselib.core.concurrency.NoFuture;
import teaselib.core.util.ExceptionUtil;

/**
 * @author Citizen-Cane
 *
 */
public class AudioOutputStream implements Closeable {

    private static final int FrameSize = 256;

    private final AudioInputStream audio;
    private final AudioOutputLine line;
    private final javax.sound.sampled.SourceDataLine dataLine;
    private final ExecutorService executor;
    private final boolean stereoUpmix;
    private final byte[] samples;
    private final byte[] processed;

    Future<Integer> future = NoFuture.Integer;

    /**
     * @param audio
     *            A pcm mono or stereo stream. If the line has been used before, check if the stream {@link #matches}
     *            the audio line format.
     * @param line
     * @param executor
     */
    public AudioOutputStream(AudioInputStream audio, AudioOutputLine line, ExecutorService executor) {
        this.audio = audio;
        this.line = line;
        this.dataLine = line.dataLine;
        this.executor = executor;
        this.stereoUpmix = dataLine.getFormat().getChannels() == 2 && audio.getFormat().getChannels() == 1;
        this.samples = new byte[FrameSize];
        this.processed = stereoUpmix ? new byte[FrameSize * 2] : null;
    }

    public Future<Integer> start() {
        dataLine.start();
        future = new AbstractFuture<>(executor.submit(this::run)) {
            @Override
            public boolean cancel(boolean interrupt) {
                boolean result = future.cancel(interrupt);
                dataLine.stop();
                dataLine.flush();
                return result;
            }
        };
        return future;
    }

    public void setVolume(float value) {
        if (dataLine.isControlSupported(FloatControl.Type.VOLUME)) {
            var volume = (FloatControl) dataLine.getControl(FloatControl.Type.VOLUME);
            volume.setValue(value);
        }
    }

    public void setBalance(float value) {
        if (dataLine.isControlSupported(FloatControl.Type.BALANCE)) {
            // Does not work for mono lines
            // http://jsresources.sourceforge.net/faq_audio.html#balance_vs_pan
            var balance = (FloatControl) dataLine.getControl(FloatControl.Type.BALANCE);
            balance.setValue(value);
        } else if (dataLine.isControlSupported(FloatControl.Type.PAN)) {
            // Does not work for mono lines
            // http://jsresources.sourceforge.net/faq_audio.html#controls_of_mono_line
            var balance = (FloatControl) dataLine.getControl(FloatControl.Type.PAN);
            balance.setValue(value);
        }
    }

    int run() throws IOException {
        int total = 0;
        try {
            boolean interrupted = false;
            while (!(interrupted = Thread.currentThread().isInterrupted())) {
                int numBytesRead = audio.read(samples, 0, samples.length);
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
                    dataLine.write(processed, 0, numBytesRead * 2);
                } else {
                    dataLine.write(samples, 0, numBytesRead);
                }
            }
            if (!interrupted) {
                dataLine.drain();
            }
        } finally {
            dataLine.stop();
            dataLine.flush();
        }
        return total;
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

    @Override
    public void close() {
        try {
            if (!future.isDone() && !future.isCancelled()) {
                future.cancel(true);
            }
            audio.close();
            line.release();
        } catch (IOException e) {
            // Ignore
        }
    }

}
