package teaselib.core.sound;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import teaselib.core.Closeable;

/**
 * @author Citizen-Cane
 *
 */
public class AudioSystem implements Closeable {

    public static class Devices<T> {

        T primary;
        private List<T> devices = Collections.emptyList();

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

        var inputPorts = MixerInfo.getInputPorts();
        var outputPorts = MixerInfo.getOutputPorts();
        var inputDevices = MixerInfo.getInputDevices();
        var outputDevices = MixerInfo.getOutputDevices();

        for (var mixer : inputDevices) {
            if (mixer.isDefaultDevice(inputPorts)) {
                if (input.primary == null) {
                    input.primary = new AudioInputDevice(mixer.name, mixer.info, null);
                } else {
                    throw new IllegalStateException("Multiple default devices");
                }
            } else {
                MixerInfo port = findMatchingPort(mixer, inputPorts);
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
                MixerInfo port = findMatchingPort(mixer, outputPorts);
                output.devices.add(new AudioOutputDevice(mixer.name, mixer.info, port.info));
            }
        }
    }

    private static MixerInfo findMatchingPort(MixerInfo mixer, List<MixerInfo> ports) {
        return ports.stream().filter(port -> mixer.name.startsWith(port.name)).findFirst().orElseThrow();
    }

    public AudioInputDevice defaultInput() {
        return input.primary;
    }

    public AudioOutputDevice defaultOutput() {
        return output.primary;
    }

    @Override
    public void close() {
        input.devices().stream().forEach(AudioInputDevice::close);
        input.primary.close();
        output.devices().stream().forEach(AudioOutputDevice::close);
        output.primary.close();
    }

}
