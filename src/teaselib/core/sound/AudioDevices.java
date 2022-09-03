package teaselib.core.sound;

import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

import javax.sound.sampled.AudioFileFormat.Type;
import javax.sound.sampled.AudioSystem;

/**
 * @author Citizen-Cane
 *
 */
public class AudioDevices {

    public static List<String> inputs() {
        return Collections.emptyList();
    }

    public static List<String> outputs() {
        return Collections.emptyList();
    }

    public static List<String> formats() {
        return Stream.of(AudioSystem.getAudioFileTypes()).map(Type::toString).toList();
    }

}
