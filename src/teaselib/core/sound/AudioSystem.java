package teaselib.core.sound;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

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
        Objects.requireNonNull(com.tagtraum.ffsampledsp.FFNativeLibraryLoader.class);

        this.input = new Devices<>();
        this.output = new Devices<>();
        enumerateDevices();
    }

    private void enumerateDevices() {
        input.devices = new ArrayList<>();
        output.devices = new ArrayList<>();

        var inputPorts = AudioMixer.getInputPorts();
        var outputPorts = AudioMixer.getOutputPorts();
        var inputDevices = AudioMixer.getInputDevices();
        var outputDevices = AudioMixer.getOutputDevices();

        for (var mixer : inputDevices) {
            if (mixer.isDefaultDevice(inputPorts)) {
                if (input.primary == null) {
                    input.primary = new AudioInputDevice(mixer.name, mixer.info, null);
                } else {
                    throw new IllegalStateException("Multiple default devices");
                }
            } else {
                AudioMixer port = findMatchingPort(mixer, inputPorts);
                input.devices.add(new AudioInputDevice(mixer.name, mixer.info, port.info));
            }
        }

        for (var mixer : outputDevices) {
            if (mixer.isDefaultDevice(outputPorts)) {
                if (output.primary == null) {
                    output.primary = new AudioOutputDevice(mixer.name, mixer.info, null);
                } else {
                    throw new IllegalStateException("Multiple default devices");
                }
            } else {
                AudioMixer port = findMatchingPort(mixer, outputPorts);
                output.devices.add(new AudioOutputDevice(mixer.name, mixer.info, port.info));
            }
        }
    }

    private static AudioMixer findMatchingPort(AudioMixer mixer, List<AudioMixer> ports) {
        return ports.stream().filter(port -> mixer.name.startsWith(port.name)).findFirst().orElseThrow();
    }

    public AudioInputDevice defaultInput() {
        return input.primary;
    }

    public AudioOutputDevice defaultOutput() {
        return output.primary;
    }

}
