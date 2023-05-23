package teaselib.core.sound;

import static org.junit.Assume.*;
import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.sound.sampled.AudioFileFormat.Type;
import javax.sound.sampled.BooleanControl;
import javax.sound.sampled.CompoundControl;
import javax.sound.sampled.Control;
import javax.sound.sampled.EnumControl;
import javax.sound.sampled.FloatControl;
import javax.sound.sampled.Line;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.Mixer;
import javax.sound.sampled.Port;
import javax.sound.sampled.UnsupportedAudioFileException;

import org.junit.Ignore;
import org.junit.Test;

import teaselib.host.Host.Audio;
import teaselib.test.TestScript;

public class AudioSystemTest {

    public static List<String> printDevices() {
        List<Line.Info> availablePorts = new ArrayList<>();
        List<Line.Info> availableLines = new ArrayList<>();
        for (Mixer.Info mixerInfo : javax.sound.sampled.AudioSystem.getMixerInfo()) {
            Mixer mixer = javax.sound.sampled.AudioSystem.getMixer(mixerInfo);
            System.out.println("\nFound " + mixer.getClass().getSimpleName() + ": " + "\"" + mixerInfo.getName() + "\""
                    + " (\"" + mixerInfo.getDescription() + "\", " + mixerInfo.getVendor() + ", " + mixerInfo.getVersion() + ")");
            try {
                mixer.open();
                for (Control thisControl : mixer.getControls()) {
                    System.out.println(AnalyzeControl(thisControl));
                }

                for (Line.Info lineInfo : mixer.getSourceLineInfo()) {
                    Line line = mixer.getLine(lineInfo);
                    System.out.println(
                            "    Found source " + line.getClass().getSimpleName() + ": " + lineInfo + " with " + mixer.getMaxLines(lineInfo)
                                    + " max-instances.");
                    try {
                        line.open();
                        for (Control thisControl : line.getControls()) {
                            System.out.println(AnalyzeControl(thisControl));
                        }
                        if (lineInfo.matches(Port.Info.MICROPHONE) || lineInfo.matches(Port.Info.LINE_IN) || lineInfo.matches(Port.Info.COMPACT_DISC)) {
                            availablePorts.add(lineInfo);
                        } else {
                            availableLines.add(lineInfo);
                        }
                    } catch (IllegalArgumentException e) {
                        System.out.println("\t\tLine unavailable: " + e.getMessage());
                    }
                }

                for (Line.Info lineInfo : mixer.getTargetLineInfo()) {
                    Line line = mixer.getLine(lineInfo);
                    System.out.println(
                            "    Found target " + line.getClass().getSimpleName() + ": " + lineInfo + " with " + mixer.getMaxLines(lineInfo)
                                    + " max-instances.");
                    try {
                        line.open();
                        for (Control thisControl : line.getControls()) {
                            System.out.println(AnalyzeControl(thisControl));
                        }
                        if (lineInfo.matches(Port.Info.HEADPHONE) || lineInfo.matches(Port.Info.SPEAKER) || lineInfo.matches(Port.Info.LINE_OUT)) {
                            availablePorts.add(lineInfo);
                        } else {
                            availableLines.add(lineInfo);
                        }
                    } catch (IllegalArgumentException e) {
                        System.out.println("\t\tLine unavailable: " + e.getMessage());
                    }
                }
            } catch (LineUnavailableException e) {
                System.out.println("\t\tLine unavailable." + e.getMessage());
            }
        }

        System.out.println("\nAvailable ports:\n" + availablePorts.stream().map(Line.Info::toString).collect(Collectors.joining("\n")));
        System.out.println("\nAvailable lines:\n" + availableLines.stream().map(Line.Info::toString).collect(Collectors.joining("\n")));
        return availableLines.stream().map(Line.Info::toString).toList();
    }

    public static String AnalyzeControl(Control thisControl) {
        String type = thisControl.getType().toString();
        if (thisControl instanceof BooleanControl) {
            return "\t\t    Control: " + type + " (boolean)";
        }
        if (thisControl instanceof CompoundControl) {
            System.out.println("\t\t    Control: " + type +
                    " (compound - values below)");
            String toReturn = "";
            for (Control children : ((CompoundControl) thisControl).getMemberControls()) {
                toReturn += "  " + AnalyzeControl(children) + "\n";
            }
            return toReturn.substring(0, toReturn.length() - 1);
        }
        if (thisControl instanceof EnumControl) {
            return "\t\t    Control:" + type + " (enum: " + thisControl.toString() + ")";
        }
        if (thisControl instanceof FloatControl) {
            return "\t\t    Control: " + type + " (float: from " +
                    ((FloatControl) thisControl).getMinimum() + " to " +
                    ((FloatControl) thisControl).getMaximum() + ")";
        }
        return "    Control: unknown type";
    }

    @Test
    public void printAudioDevices() {
        var lines = printDevices();
        assertNotNull(lines);
        assumeTrue(lines.size() > 0);
    }

    // Audio-Service-Provider:
    // https://www.tagtraum.com/ffsampledsp/
    // https://www.tagtraum.com/ffmpeg/

    @Test
    public void testAudioAudioFormats() {
        var formats = Stream.of(javax.sound.sampled.AudioSystem.getAudioFileTypes()).map(Type::toString).toList();
        assertNotNull(formats);
        assertEquals(3, formats.size());
        assertTrue(formats.contains("WAVE"));
        assertTrue(formats.contains("AU"));
        assertTrue(formats.contains("AIFF"));
        // ffsampledsp does not add explicit audio formats
    }

    @Test
    public void testAudioOutputDevices() {
        try (AudioSystem audioSystem = new AudioSystem()) {

            var speakers = audioSystem.output.devices();
            assertNotNull(speakers);
            assumeTrue(speakers.size() > 0);
            assertNotNull(audioSystem.defaultOutput());
            assertFalse(speakers.contains(audioSystem.defaultOutput()));

            var mics = audioSystem.input.devices();
            assertNotNull(mics);
            assumeTrue(mics.size() > 0);
            assertNotNull(audioSystem.defaultInput());
            assertFalse(mics.contains(audioSystem.defaultInput()));
        }
    }

    @Test
    public void testAudioSystemReadMp3SpeechShort()
            throws IOException, UnsupportedAudioFileException, LineUnavailableException, InterruptedException, ExecutionException {
        testMP3("0.mp3", 34654);
    }

    @Test
    @Ignore
    public void testAudioSystemReadMp3SpeechLong()
            throws IOException, UnsupportedAudioFileException, LineUnavailableException, InterruptedException, ExecutionException {
        testMP3("1.mp3", 758110);
    }

    @Test
    public void testInterruptAudioStream()
            throws IOException, UnsupportedAudioFileException, LineUnavailableException, InterruptedException {
        try (AudioSystem audioSystem = new AudioSystem()) {
            var device = audioSystem.output.primary;
            InputStream mp3 = getClass().getResource("1.mp3").openStream();
            var audio = device.newStream(Audio.Type.Speech, mp3);
            var playing = audio.start();
            Thread.sleep(1000);
            playing.cancel(true);
            assertThrows(CancellationException.class, () -> playing.get());
        }
    }

    @Test
    public void testBalance()
            throws IOException, UnsupportedAudioFileException, LineUnavailableException, InterruptedException {
        try (AudioSystem audioSystem = new AudioSystem()) {
            var device = audioSystem.output.primary;
            InputStream mp3 = getClass().getResource("1.mp3").openStream();
            var audio = device.newStream(Audio.Type.Speech, mp3);
            audio.setVolume(1.0f);
            audio.setBalance(-1.0f);
            var playing = audio.start();
            Thread.sleep(1000);
            audio.setBalance(1.0f);
            Thread.sleep(1000);
            playing.cancel(true);
            assertThrows(CancellationException.class, () -> playing.get());
        }
    }

    @Test
    public void testZippeAudioResource()
            throws IOException, UnsupportedAudioFileException, LineUnavailableException, InterruptedException, ExecutionException {
        try (TestScript script = new TestScript(getClass())) {
            script.resources.addAssets("teaselib/core/sound/audio.zip");
            InputStream mp3 = script.resources.get("/2.mp3");
            assertNotNull(mp3);
            testMP3(mp3, 34654);
        }
    }

    private void testMP3(String name, int s)
            throws IOException, LineUnavailableException, UnsupportedAudioFileException, InterruptedException, ExecutionException {
        InputStream mp3 = getClass().getResource(name).openStream();
        testMP3(mp3, s);
    }

    private static void testMP3(InputStream mp3, int s)
            throws LineUnavailableException, UnsupportedAudioFileException, IOException, InterruptedException, ExecutionException {
        assertNotNull(mp3);
        try (AudioSystem audioSystem = new AudioSystem()) {
            var device = audioSystem.output.primary;
            var audio = device.newStream(Audio.Type.Speech, mp3);
            var playing = audio.start();
            int samples = playing.get();
            assertEquals(s, samples);
        }
    }

}
