package teaselib.core.sound;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
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

    public AudioOutputLine getLine(Audio.Type type, InputStream audio) throws LineUnavailableException, UnsupportedAudioFileException, IOException {
        return getLine(type, javax.sound.sampled.AudioSystem.getAudioInputStream(new BufferedInputStream(audio)));
    }

    public AudioOutputLine getLine(Audio.Type type, javax.sound.sampled.AudioInputStream audio) throws LineUnavailableException {
        var mp3Format = audio.getFormat();
        var pcmFormat = pcmFormat(mp3Format);
        AudioInputStream pcmStream = javax.sound.sampled.AudioSystem.getAudioInputStream(pcmFormat, audio);
        AudioFormat stereoFormat = stereoFormat(pcmFormat);
        return getLine(type, pcmStream, stereoFormat);
    }

    private static AudioFormat pcmFormat(AudioFormat sourcemp3Format) {
        var pcmFormat = new AudioFormat(
                AudioFormat.Encoding.PCM_SIGNED,
                sourcemp3Format.getSampleRate(),
                16,
                sourcemp3Format.getChannels(),
                16 * sourcemp3Format.getChannels() / 8,
                sourcemp3Format.getSampleRate(),
                sourcemp3Format.isBigEndian());
        return pcmFormat;
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
        AudioFormat stereoFormat;
        stereoFormat = new AudioFormat(
                monoFormat.getEncoding(),
                monoFormat.getSampleRate(),
                monoFormat.getSampleSizeInBits(),
                2,
                2 * monoFormat.getFrameSize(),
                monoFormat.getSampleRate(),
                monoFormat.isBigEndian());
        return stereoFormat;
    }

    private AudioOutputLine getLine(Audio.Type type, AudioInputStream pcmStream, AudioFormat stereoFormat)
            throws LineUnavailableException {
        AudioOutputLine line = lines.get(type);
        if (line == null) {
            line = newAudioLine(type, stereoFormat);
        } else if (!line.matches(stereoFormat)) {
            line.close();
            line = newAudioLine(type, stereoFormat);
        } else if (line.isActive()) {
            try {
                line.awaitCompletion();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        line.load(pcmStream);
        return line;
    }

    private AudioOutputLine newAudioLine(Audio.Type type, AudioFormat stereoFormat) throws LineUnavailableException {
        var line = new AudioOutputLine(stereoFormat, mixerInfo, audioService);
        lines.put(type, line);
        return line;
    }

    @Override
    public void close() {
        lines.values().stream().forEach(Closeable::close);
    }

}
