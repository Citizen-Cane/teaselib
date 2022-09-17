package teaselib.core.sound;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;

import teaselib.core.Closeable;

/**
 * @author Citizen-Cane
 *
 */
class AudioOutputLine implements Closeable {

    final AudioOutputDevice device;
    final SourceDataLine dataLine;

    /**
     * @param stereoFormat
     *            The audio format for the source data line.
     * @param mixer
     *            The mixer for this audio line.
     * @param streamingExecutor
     *            Executor for streaming the audio data to the source line
     * @throws LineUnavailableException
     */
    public AudioOutputLine(AudioOutputDevice device, AudioFormat format)
            throws LineUnavailableException {
        this.device = device;
        this.dataLine = javax.sound.sampled.AudioSystem.getSourceDataLine(format, device.mixerInfo);
        dataLine.open();
    }

    /**
     * @param format
     *            Indicates whether this AudioLine's format matches the one specified.
     * @return {@code true} if this format matches the one specified, {@code false} otherwise
     * @see {@link AudioFormat#matches}
     */
    public boolean matches(AudioFormat format) {
        return format.matches(dataLine.getFormat());
    }

    public void release() {
        device.release(this);
    }

    @Override
    public void close() {
        dataLine.close();
    }

}
