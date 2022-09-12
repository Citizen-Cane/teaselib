package teaselib.core.sound;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.TimeUnit;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.Mixer;
import javax.sound.sampled.SourceDataLine;
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

    public AudioOutputLine addLine(InputStream audio) throws LineUnavailableException, UnsupportedAudioFileException, IOException {
        return addLine(javax.sound.sampled.AudioSystem.getAudioInputStream(new BufferedInputStream(audio)));
    }

    public AudioOutputLine addLine(javax.sound.sampled.AudioInputStream audio) throws LineUnavailableException {
        var mp3Format = audio.getFormat();
        var pcmFormat = new AudioFormat(
                AudioFormat.Encoding.PCM_SIGNED,
                mp3Format.getSampleRate(),
                16,
                mp3Format.getChannels(),
                2 * mp3Format.getChannels(),
                mp3Format.getSampleRate(),
                mp3Format.isBigEndian());
        AudioFormat stereoFormat = pcmFormat.getChannels() == 1 // && EnableStereoUpmix
                ? new AudioFormat(
                        AudioFormat.Encoding.PCM_SIGNED,
                        mp3Format.getSampleRate(),
                        16,
                        2,
                        4,
                        mp3Format.getSampleRate(),
                        mp3Format.isBigEndian())
                : pcmFormat;
        AudioInputStream pcmStream = javax.sound.sampled.AudioSystem.getAudioInputStream(pcmFormat, audio);
        SourceDataLine stereoLine = javax.sound.sampled.AudioSystem.getSourceDataLine(stereoFormat, mixerInfo);
        AudioOutputLine line = new AudioOutputLine(pcmStream, stereoLine, audioService);
        lines.add(line);
        return line;
    }

    public void remove(AudioOutputLine line) {
        lines.remove(line);
    }

}
