package teaselib.stimulation;

/**
 * Represents a single channel of a stimulation device.
 * <p>
 * A device usually consists of multiple stimulators (like gamepads with rumble motors), in that case several stimulator
 * instances are created for the device.
 * 
 * @author Citizen-Cane
 *
 */
public interface Stimulator {
    /**
     * The extent to which the channels of a device can be controlled independently of each other.
     * <p>
     * XBox gamepad with rumble motors are independent as long as there is enough power.
     * <p>
     * XBox gamepad with estim hack:
     * <p>
     * independent: as long if the voltage is high enough (batteries instead of accumulators, good reed relais, etc.),
     * and the estim device is also independent.
     * <p>
     * Partially independent: if reed relays need a somewhat higher trigger voltage, or the estim unit has multiple
     * channels, but they are not completely isolated from each other.
     * <p>
     * Dependent: Single channel estim device is used to power both gamepad channels.
     */
    enum ChannelDependency {
        Independent,
        PartiallyDependent,
        Dependent
    }

    /**
     * The physical result of the stimulation
     * 
     */
    enum Output {
        Vibration,
        EStim
    }

    /**
     * Electrode wiring
     *
     */
    enum Wiring {
        /**
         * Independent channels.
         */
        Independent,

        /**
         * When both channel signals add up to induce a stronger signal on the body part that's wired up with a common
         * electrode, the device provides an additional channel, however all three channels will be dependent.
         */
        INFERENCE_CHANNEL
    }

    enum Signal {
        Discrete,
        Continuous
    }

    double SignalLevel_Pace = 0.33;
    double SignalLevel_Tease = 0.66;
    double SignalLevel_Punish = 1.0;

    /**
     * The name of the device that provides the stimulation
     * 
     */
    String getName();

    /**
     * The device that controls the stimulation output
     * 
     */
    StimulationDevice getDevice();

    /**
     * Whether the channel is independent of the other channels of the device
     * 
     */
    ChannelDependency channelDependency();

    /**
     * The kind of output this stimulator delivers
     * 
     */
    Output output();

    Signal signal();

    /**
     * The duration the output value must be set to high in order to receive a noticeable output signal.
     * 
     */
    double minimalSignalDuration();

    /**
     * Play the stimulation.
     * 
     */
    void play(WaveForm waveform, double durationSeconds, double maxStrength);

    void extend(double durationSeconds);

    void stop();

    void complete();
}
