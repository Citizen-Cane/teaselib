package teaselib.core.sound;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.TimeUnit;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.Mixer;
import javax.sound.sampled.UnsupportedAudioFileException;

import teaselib.core.concurrency.NamedExecutorService;

/**
 * @author Citizen-Cane
 *
 */
public class AudioOutputDevice extends AudioDevice<AudioOutputLine> {

    NamedExecutorService audioService = NamedExecutorService.newUnlimitedThreadPool("Audio-Service", 1, TimeUnit.HOURS);

    public AudioOutputDevice(String name, Mixer.Info mixer, Mixer.Info port) {
        super(name, mixer, port);
    }

    public AudioOutputLine addLine(InputStream audio) throws LineUnavailableException, UnsupportedAudioFileException, IOException {
        return addLine(javax.sound.sampled.AudioSystem.getAudioInputStream(new BufferedInputStream(audio)));
    }

    public AudioOutputLine addLine(javax.sound.sampled.AudioInputStream audio) throws LineUnavailableException {
        var mp3Format = audio.getFormat();
        var pcmFormat = pcmFormat(mp3Format);
        AudioInputStream pcmStream = javax.sound.sampled.AudioSystem.getAudioInputStream(pcmFormat, audio);

        AudioFormat stereoFormat;
        if (pcmFormat.getChannels() == 1 /* && EnableStereoUpmix */) {
            stereoFormat = stereoFormat(pcmFormat);
        } else {
            stereoFormat = pcmFormat;
        }
        AudioOutputLine line = new AudioOutputLine(pcmStream, stereoFormat, mixerInfo, audioService);
        lines.add(line);
        return line;
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

    private static AudioFormat stereoFormat(AudioFormat monoFormat) {
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

    public void remove(AudioOutputLine line) {
        lines.remove(line);
        line.close();
    }

}
