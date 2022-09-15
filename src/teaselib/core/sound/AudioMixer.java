package teaselib.core.sound;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;

import javax.sound.sampled.Line;
import javax.sound.sampled.Line.Info;
import javax.sound.sampled.Mixer;
import javax.sound.sampled.Port;

/**
 * @author Citizen-Cane
 *
 */
class AudioMixer {

    final String name;
    final Mixer.Info info;

    AudioMixer(String name, Mixer.Info info) {
        this.name = name;
        this.info = info;

    }

    static final Function<String, String> PortNamingRule = name -> name.substring(5);

    static List<AudioMixer> getInputPorts() {
        return AudioMixer.get(AudioMixer::isInputPort, PortNamingRule);
    }

    static List<AudioMixer> getOutputPorts() {
        return AudioMixer.get(AudioMixer::isOutputPort, PortNamingRule);
    }

    static final Function<String, String> DeviceNamingRule = name -> name;

    static List<AudioMixer> getInputDevices() {
        return AudioMixer.get(AudioMixer::isInputDevice, DeviceNamingRule);
    }

    private static boolean isInputDevice(Mixer.Info info) {
        if (isPort(info)) {
            return false;
        } else {
            var mixer = javax.sound.sampled.AudioSystem.getMixer(info);
            return mixer.getSourceLineInfo().length == 0 && mixer.getTargetLineInfo().length > 0;
        }
    }

    static List<AudioMixer> getOutputDevices() {
        return AudioMixer.get(AudioMixer::isOutputDevice, DeviceNamingRule);
    }

    private static boolean isOutputDevice(Mixer.Info info) {
        if (isPort(info)) {
            return false;
        } else {
            var mixer = javax.sound.sampled.AudioSystem.getMixer(info);
            return mixer.getSourceLineInfo().length > 0 && mixer.getTargetLineInfo().length == 0;
        }
    }

    private static boolean isInputPort(Mixer.Info info) {
        Mixer mixer = javax.sound.sampled.AudioSystem.getMixer(info);
        Info[] lineInfo = mixer.getSourceLineInfo();
        return isPort(info, lineInfo, AudioMixer::isInputPort);
    }

    private static boolean isOutputPort(Mixer.Info info) {
        Mixer mixer = javax.sound.sampled.AudioSystem.getMixer(info);
        Info[] lineInfo = mixer.getTargetLineInfo();
        return isPort(info, lineInfo, AudioMixer::isOutputPort);
    }

    private static boolean isInputPort(Line.Info info) {
        return info.matches(Port.Info.MICROPHONE) || info.matches(Port.Info.LINE_IN) || info.matches(Port.Info.COMPACT_DISC);
    }

    private static boolean isOutputPort(Line.Info info) {
        return info.matches(Port.Info.HEADPHONE) || info.matches(Port.Info.LINE_OUT) || info.matches(Port.Info.SPEAKER);
    }

    private static boolean isPort(Mixer.Info info, Info[] lineInfo, Predicate<? super javax.sound.sampled.Line.Info> predicate) {
        if (isPort(info)) {
            return Stream.of(lineInfo).anyMatch(predicate);
        }
        return false;
    }

    private static boolean isPort(Mixer.Info info) {
        return info.getClass().getSimpleName().contains("Port");
    }

    private static List<AudioMixer> get(Predicate<Mixer.Info> matching, Function<String, String> namingRule) {
        List<AudioMixer> ports = new ArrayList<>();
        for (Mixer.Info mixer : javax.sound.sampled.AudioSystem.getMixerInfo()) {
            if (matching.test(mixer)) {
                ports.add(new AudioMixer(namingRule.apply(mixer.getName()), mixer));
            }
        }
        return ports;
    }

    boolean isDefaultDevice(List<AudioMixer> ports) {
        return !isPort(this.info) && ports.stream().noneMatch(port -> this.name.startsWith(port.name));
    }

}
