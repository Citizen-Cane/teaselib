package teaselib.core.sound;

import static org.junit.Assume.*;
import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.io.InputStream;
import java.util.stream.Stream;

import javax.sound.sampled.AudioFileFormat.Type;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;

import org.junit.Test;

public class AudioDevicesTest {

    @Test
    public void testAudioSystem() {
        var formats = Stream.of(AudioSystem.getAudioFileTypes()).map(Type::toString).toList();
        assertNotNull(formats);
        assertEquals(3, formats.size());
        assertTrue(formats.contains("WAVE"));
        assertTrue(formats.contains("AU"));
        assertTrue(formats.contains("AIFF"));

        var lines = AudioDevices.inputs();
        assumeTrue(lines.size() > 0);
    }

    // Audio-Service-Provider:
    // https://www.tagtraum.com/ffsampledsp/
    // https://www.tagtraum.com/ffmpeg/

    @Test
    public void testReadMp3SpeechShort() throws IOException, UnsupportedAudioFileException {
        testMp3("0.mp3");
    }

    @Test
    public void testReadMp3SpeechLong() throws IOException, UnsupportedAudioFileException {
        testMp3("1.mp3");
    }

    private void testMp3(String resource) throws UnsupportedAudioFileException, IOException {
        InputStream mp3 = getClass().getResource(resource).openStream();
        assertNotNull(mp3);
        AudioInputStream in = AudioSystem.getAudioInputStream(mp3);
        AudioFormat format = in.getFormat();
        assertNotNull(format);
        assertEquals(1, format.getChannels());
        assertEquals(32000, format.getSampleRate());
    }

}
