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

    public AudioOutputLine(SourceDataLine line, AudioInputStream pcm, ExecutorService executor) {
        this.line = line;
        this.pcm = pcm;
        this.executor = executor;
    }

    public void setVolume(float value) {
        if (line.isControlSupported(FloatControl.Type.VOLUME)) {
            var volume = (FloatControl) line.getControl(FloatControl.Type.VOLUME);
            volume.setValue(value);
        }
    }

    public void setBalance(float value) {
        if (line.isControlSupported(FloatControl.Type.PAN)) {
            // Does not work for mono lines
            // http://jsresources.sourceforge.net/faq_audio.html#controls_of_mono_line
            var balance = (FloatControl) line.getControl(FloatControl.Type.PAN);
            balance.setValue(value);
        } else if (line.isControlSupported(FloatControl.Type.BALANCE)) {
            // Does not work for mono lines
            // http://jsresources.sourceforge.net/faq_audio.html#balance_vs_pan
            var balance = (FloatControl) line.getControl(FloatControl.Type.BALANCE);
            balance.setValue(value);
        }
    }

    public Future<Integer> start() throws LineUnavailableException {
        line.open();
        return new AbstractFuture<>(executor.submit(this::play)) {
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

    private int play() throws IOException {
        // TODO for mono formats when Control.PAN is missing, convert to stereo stream to allow using Balance Control
        byte[] myData = new byte[FrameSize];
        int total = 0;
        line.start();
        try {
            boolean interrupted = false;
            while (!(interrupted = Thread.currentThread().isInterrupted())) {
                int numBytesRead = pcm.read(myData, 0, myData.length);
                if (numBytesRead <= 0)
                    break;
                total += numBytesRead;
                line.write(myData, 0, numBytesRead);
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
