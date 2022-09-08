package teaselib.core.sound;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;

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
        return executor.submit(this::play);
    }

    private int play() throws IOException, LineUnavailableException {
        int frameSize = pcm.getFormat().getFrameSize();

        frameSize = 256;
        if (frameSize <= 0) {
            frameSize = 2048;
        }
        byte[] myData = new byte[frameSize * 16];
        int total = 0;

        line.open();
        line.start();
        try {
            while (!Thread.interrupted()) {
                int numBytesRead = pcm.read(myData, 0, myData.length);
                if (numBytesRead <= 0)
                    break;
                total += numBytesRead;
                line.write(myData, 0, numBytesRead);
            }
            line.drain();
        } finally {
            line.stop();
            line.close();
        }
        return total;
    }

}
