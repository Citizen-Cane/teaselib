package teaselib.core.sound;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.Mixer;
import javax.sound.sampled.UnsupportedAudioFileException;

import teaselib.core.Closeable;
import teaselib.core.concurrency.NamedExecutorService;
import teaselib.host.Host.Audio;

/**
 * @author Citizen-Cane
 *
 */
public class AudioOutputDevice extends AudioDevice implements Closeable {

    NamedExecutorService audioService;
    protected final Map<Audio.Type, AudioOutputLine> lines;

    public AudioOutputDevice(String name, Mixer.Info mixer, Mixer.Info port) {
        super(name, mixer, port);
        this.audioService = NamedExecutorService.newUnlimitedThreadPool("Audio-Service", 1, TimeUnit.HOURS);
        this.lines = new HashMap<>();
    }

    public AudioOutputStream newStream(Audio.Type type, InputStream audio) throws UnsupportedAudioFileException, IOException, LineUnavailableException {
        return newStream(type, javax.sound.sampled.AudioSystem.getAudioInputStream(new BufferedInputStream(audio)));
    }

    public AudioOutputStream newStream(Audio.Type type, javax.sound.sampled.AudioInputStream audio) throws LineUnavailableException {
        var mp3Format = audio.getFormat();
        var pcmFormat = pcmFormat(mp3Format);
        var pcmStream = javax.sound.sampled.AudioSystem.getAudioInputStream(pcmFormat, audio);
        var stereoFormat = stereoFormat(pcmFormat);
        var line = getLine(type, stereoFormat);
        return new AudioOutputStream(pcmStream, line, audioService);
    }

    private static AudioFormat pcmFormat(AudioFormat sourcemp3Format) {
        return new AudioFormat(
                AudioFormat.Encoding.PCM_SIGNED,
                sourcemp3Format.getSampleRate(),
                16,
                sourcemp3Format.getChannels(),
                16 * sourcemp3Format.getChannels() / 8,
                sourcemp3Format.getSampleRate(),
                sourcemp3Format.isBigEndian());
    }

    private static AudioFormat stereoFormat(AudioFormat pcmFormat) {
        AudioFormat stereoFormat;
        if (pcmFormat.getChannels() == 1) {
            stereoFormat = createstereoFormat(pcmFormat);
        } else {
            stereoFormat = pcmFormat;
        }
        return stereoFormat;
    }

    private static AudioFormat createstereoFormat(AudioFormat monoFormat) {
        return new AudioFormat(
                monoFormat.getEncoding(),
                monoFormat.getSampleRate(),
                monoFormat.getSampleSizeInBits(),
                2,
                2 * monoFormat.getFrameSize(),
                monoFormat.getSampleRate(),
                monoFormat.isBigEndian());
    }

    private AudioOutputLine getLine(Audio.Type type, AudioFormat stereoFormat)
            throws LineUnavailableException {
        AudioOutputLine line = lines.get(type);
        if (line == null || !line.matches(stereoFormat)) {
            line = newAudioLine(type, stereoFormat);
        }
        return line;
    }

    private AudioOutputLine newAudioLine(Audio.Type type, AudioFormat stereoFormat) throws LineUnavailableException {
        var line = new AudioOutputLine(this, stereoFormat);
        lines.put(type, line);
        return line;
    }

    public void release(AudioOutputLine line) {
        if (!lines.values().contains(line)) {
            line.close();
        }
    }

    @Override
    public void close() {
        lines.values().stream().forEach(Closeable::close);
    }

}
