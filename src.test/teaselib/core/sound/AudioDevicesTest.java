package teaselib.core.sound;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.io.InputStream;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;

import org.junit.Test;

public class AudioDevicesTest {

    @Test
    public void testFormats() {

        var formats = AudioDevices.formats();
        assertNotNull(formats);
        assertNotEquals(0, formats.size());

        assertTrue(formats.contains("WAVE"));
        assertTrue(formats.contains("AU"));
        assertTrue(formats.contains("AIFF"));

        assertTrue(formats.contains("AIFF-C"), "part of mp3spi");

        // TODO with mp3spi there's no mp3 file type
        assertTrue(formats.contains("MP3"), "MP3 spi");
        assertTrue(formats.contains("OGG"), "Ogg spi");
    }

    @Test
    public void testReadMp3SpeechShort() throws IOException, UnsupportedAudioFileException {
        InputStream mp3 = getClass().getResource("0.mp3").openStream();
        assertNotNull(mp3);
        AudioInputStream in = AudioSystem.getAudioInputStream(mp3);
        assertNotNull(in.getFormat());
    }

    @Test
    public void testReadMp3SpeechLong() throws IOException, UnsupportedAudioFileException {
        InputStream mp3 = getClass().getResource("1.mp3").openStream();
        assertNotNull(mp3);
        AudioInputStream in = AudioSystem.getAudioInputStream(mp3);
        assertNotNull(in.getFormat());
    }

}
