package teaselib.core.sound;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.FloatControl;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;

import teaselib.core.concurrency.AbstractFuture;

/**
 * @author Citizen-Cane
 *
 */
public class AudioOutputLine {

    private final SourceDataLine line;
    private final AudioInputStream pcm;
    private final ExecutorService executor;
    private final boolean stereoUpmix;

    public AudioOutputLine(AudioInputStream pcm, SourceDataLine line, ExecutorService executor) {
        this.line = line;
        this.pcm = pcm;
        this.executor = executor;
        this.stereoUpmix = line.getFormat().getChannels() == 2 && pcm.getFormat().getChannels() == 1;
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

    public Future<Integer> start() throws LineUnavailableException {
        line.open();
        return new AbstractFuture<>(executor.submit(this::stream)) {
            @Override
            public boolean cancel(boolean interrupt) {
                boolean result = future.cancel(interrupt);
                line.flush();
                line.stop();
                line.close();
                return result;
            }
        };
    }

    private static final int FrameSize = 256;

    @SuppressWarnings("null")
    private int stream() throws IOException {
        byte[] samples = new byte[FrameSize];
        byte[] processed = stereoUpmix ? new byte[FrameSize * 2] : null;
        int total = 0;
        line.start();
        try {
            boolean interrupted = false;
            while (!(interrupted = Thread.currentThread().isInterrupted())) {
                int numBytesRead = pcm.read(samples, 0, samples.length);
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
            line.close();
        }
        return total;
    }

}
