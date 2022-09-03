package teaselib.core.sound;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.Test;

import javazoom.spi.mpeg.sampled.file.MpegAudioFileReader;

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
    public void testReadMp3() {
        MpegAudioFileReader reader = new MpegAudioFileReader();
    }

}
