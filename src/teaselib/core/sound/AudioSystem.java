package teaselib.core.sound;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.sound.sampled.Mixer;
import javax.sound.sampled.Mixer.Info;

/**
 * @author Citizen-Cane
 *
 */
public class AudioSystem {

    public static class Devices<T> {

        T primary;
        List<T> devices = Collections.emptyList();

        public List<T> devices() {
            return devices;
        }

    }

    public final Devices<AudioInputDevice> input;
    public final Devices<AudioOutputDevice> output;

    public AudioSystem() {
        this.input = new Devices<>();
        this.output = new Devices<>();
        enumerateDevices();
    }

    private void enumerateDevices() {
        input.devices = new ArrayList<>();
        output.devices = new ArrayList<>();
        var ports = ports(AudioSystem::isPort);
        var mixers = mixers(Predicate.not(AudioSystem::isPort));
        for (var mixer : mixers.entrySet()) {
            String name = mixer.getKey();
            if (isDefaultDevice(mixer.getValue())) {
                if (isInput(mixer.getValue())) {
                    if (input.primary == null) {
                        input.primary = new AudioInputDevice(name, mixer.getValue(), null);
                    } else {
                        throw new IllegalStateException("Multiple default devices");
                    }
                } else if (isOutput(mixer.getValue())) {
                    if (output.primary == null) {
                        output.primary = new AudioOutputDevice(name, mixer.getValue(), null);
                    } else {
                        throw new IllegalStateException("Multiple default devices");
                    }
                }
            } else {
                if (isInput(mixer.getValue())) {
                    for (var port : ports.entrySet()) {
                        String portName = port.getKey();
                        if (name.startsWith(portName)) {
                            input.devices.add(new AudioInputDevice(name, mixer.getValue(), port.getValue()));
                            break;
                        }
                    }
                } else if (isOutput(mixer.getValue())) {
                    for (var port : ports.entrySet()) {
                        String portName = port.getKey();
                        if (name.startsWith(portName)) {
                            output.devices.add(new AudioOutputDevice(name, mixer.getValue(), port.getValue()));
                            break;
                        }
                    }
                }
            }
        }

    }

    private static boolean isInput(Mixer.Info info) {
        var mixer = javax.sound.sampled.AudioSystem.getMixer(info);
        return mixer.getSourceLineInfo().length == 0 && mixer.getTargetLineInfo().length > 0;
    }

    private static boolean isOutput(Mixer.Info info) {
        var mixer = javax.sound.sampled.AudioSystem.getMixer(info);
        return mixer.getSourceLineInfo().length > 0 && mixer.getTargetLineInfo().length == 0;
    }

    private static Map<String, Mixer.Info> ports(Predicate<? super Info> predicate) {
        return Stream.of(javax.sound.sampled.AudioSystem.getMixerInfo()).filter(predicate).collect(
                Collectors.toMap(mixer -> mixer.getName().substring(5), Function.identity()));
    }

    private static Map<String, Mixer.Info> mixers(Predicate<? super Info> predicate) {
        return Stream.of(javax.sound.sampled.AudioSystem.getMixerInfo()).filter(predicate).collect(
                Collectors.toMap(mixer -> mixer.getName(), Function.identity()));
    }

    private static boolean isDefaultDevice(Mixer.Info info) {
        String name = info.getName();
        // TODO All fucked up, since the primary sound device names are localized
        // -> possibly derive the name from the two devices with the same name - more precisely the same first word
        return name.toLowerCase().startsWith("primary") || !name.contains("(");
    }

    private static boolean isPort(Mixer.Info info) {
        return info.getName().toLowerCase().startsWith("port");
    }

    public AudioDevice defaultInput() {
        return input.primary;
    }

    public AudioOutputDevice defaultOutput() {
        return output.primary;
    }

}
