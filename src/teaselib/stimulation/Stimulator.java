package teaselib.stimulation;

/**
 * Represents a single channel of a stimulation device.
 * 
 * A device usually consists of multiple stimulators (like gamepads with rumble motors), in that case several stimulator
 * instances are created for the device.
 * 
 * @author Citizen-Cane
 *
 */
public interface Stimulator {
    /**
     * The extent to which the channels of a device can be contolled independetly from each other:
     * 
     * XBox gamepad with rumble motors: independent as long as there is enough power
     * 
     * XBox gamepad with estim hack:
     * 
     * independent as long if the voltage is high enough (batteries instead of accumulators, good reed relais, etc.),
     * and the estim device is also independent
     * 
     * Partially independent if reed relays need a somewhat higher trigger voltage, or the estim unit has multiple
     * channels but they are not completely isolated from each other
     * 
     * Dependent: Single channel estim device is used to power both gamepad channels
     * 
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
    public enum Wiring {
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

    public enum Signal {
        Discrete,
        Continous
    }

    public static double SingalLevel_Pace = 0.33;
    public static double SingalLevel_Tease = 0.66;
    public static double SingalLevel_Punish = 1.0;

    /**
     * The name of the device that provides the stimulation
     * 
     * @return
     */
    String getName();

    /**
     * The device that controls the stimulation output
     * 
     * @return
     */
    StimulationDevice getDevice();

    /**
     * Whether the channel is independent from the other channels of the device
     * 
     * @return
     */
    ChannelDependency channelDependency();

    /**
     * The kind of output this stimulator delivers
     * 
     * @return
     */
    Output output();

    Signal signal();

    /**
     * The duration the output value must be set to high in order to receive a noticeable output signal.
     * 
     * @return
     */
    double minimalSignalDuration();

    /**
     * Play the stimulation.
     * 
     * @param waveform
     * @param durationSeconds
     * @param maxstrength
     */
    @Deprecated
    void play(WaveForm waveform, double durationSeconds, double maxstrength);

    @Deprecated
    void extend(double durationSeconds);

    @Deprecated
    void stop();

    @Deprecated
    void complete();
}
