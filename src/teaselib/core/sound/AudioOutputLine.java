package teaselib.core.sound;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import javax.sound.sampled.AudioInputStream;
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
        throw new UnsupportedOperationException("TODO");
    }

    public void setBalance(float value) {
        throw new UnsupportedOperationException("TODO");
    }

    public Future<Integer> start() {
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

    private int play() throws IOException, LineUnavailableException {
        byte[] myData = new byte[FrameSize];
        int total = 0;
        line.open();
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
