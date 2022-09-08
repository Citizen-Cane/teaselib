package teaselib.core.sound;

import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.TimeUnit;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.Mixer;
import javax.sound.sampled.UnsupportedAudioFileException;

import teaselib.core.concurrency.NamedExecutorService;

/**
 * @author Citizen-Cane
 *
 */
public class AudioOutputDevice extends AudioDevice {

    NamedExecutorService audioService = NamedExecutorService.newUnlimitedThreadPool("Audio-Service", 1, TimeUnit.HOURS);

    public AudioOutputDevice(String name, Mixer.Info mixer, Mixer.Info port) {
        super(name, mixer, port);
    }

    public AudioOutputLine play(InputStream audio) throws LineUnavailableException, UnsupportedAudioFileException, IOException {
        return play(javax.sound.sampled.AudioSystem.getAudioInputStream(audio));
    }

    public AudioOutputLine play(javax.sound.sampled.AudioInputStream audio) throws LineUnavailableException {
        var mp3Format = audio.getFormat();
        var pcmFormat = new AudioFormat(
                AudioFormat.Encoding.PCM_SIGNED,
                mp3Format.getSampleRate(),
                16,
                mp3Format.getChannels(),
                16 * mp3Format.getChannels() / 8,
                mp3Format.getSampleRate(),
                mp3Format.isBigEndian());
        var pcm = javax.sound.sampled.AudioSystem.getAudioInputStream(pcmFormat, audio);
        var line = javax.sound.sampled.AudioSystem.getSourceDataLine(pcmFormat, mixerInfo);
        return new AudioOutputLine(line, pcm, audioService);
        // TODO remember lines until played or remove collection in base class
    }

}
