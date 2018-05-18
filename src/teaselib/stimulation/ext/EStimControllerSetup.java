package teaselib.stimulation.ext;

import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import teaselib.Actor;
import teaselib.Message;
import teaselib.TeaseScript;
import teaselib.core.Script;
import teaselib.core.devices.DeviceCache;
import teaselib.stimulation.BurstSquareWave;
import teaselib.stimulation.ConstantWave;
import teaselib.stimulation.Stimulation;
import teaselib.stimulation.StimulationDevice;
import teaselib.stimulation.Stimulator;
import teaselib.stimulation.Stimulator.Output;
import teaselib.stimulation.Stimulator.Signal;
import teaselib.stimulation.Stimulator.Wiring;
import teaselib.stimulation.WaveForm;

/**
 * Performs setup of the global stimulation controller.
 * 
 * @author Citizen-Cane
 *
 */
public class EStimControllerSetup extends TeaseScript {
    public static final Predicate<Stimulator> EstimOutput = (
            Stimulator stimulator) -> stimulator.output() == Output.EStim;
    public static final Predicate<Stimulator> VibrationOutput = (
            Stimulator stimulator) -> stimulator.output() == Output.Vibration;

    public static final Predicate<Stimulator> ContinousOutput = (
            Stimulator stimulator) -> stimulator.signal() == Signal.Continous;

    public EStimControllerSetup(TeaseScript script) {
        super(script, getOrDefault(script, Locale.ENGLISH));
    }

    private static Actor getOrDefault(Script script, Locale locale) {
        if (script.actor.locale().getLanguage().equalsIgnoreCase(locale.getLanguage())) {
            return script.actor;
        } else {
            Actor defaultDominant = script.teaseLib.getDominant(script.actor.gender, locale);
            defaultDominant.images = script.actor.images;
            return defaultDominant;
        }
    }

    public EStimController getController() {
        return teaseLib.globals.getOrDefault(EStimController.class, EStimController::new);
    }

    public EStimController run() {
        EStimController stim = getController();
        return EStimController.init(stim, adjustLevels(stim, attachElectrodes(getDevice())));
    }

    private StimulationDevice getDevice() {
        say("Get your EStim device and turn on the teaseLib controller!");
        String manualDevice = "I only have a manual device";
        String notAvailable = "I'm sorry, #title, but I don't have any";
        Chooser chooser = new Chooser();
        String result = reply(chooser::connectDevice, manualDevice, notAvailable);
        if (result == Chooser.CONNECTED) {
            return chooser.device;
        } else {
            // TODO remove test code
            return chooser.device;
            // return StimulationDevice.MANUAL;
        }
    }

    class Chooser {
        static final String CONNECTED = "Device Connected";
        static final String CANCELLED = "Cancelled";
        StimulationDevice device;

        String connectDevice() {
            do {
                device = teaseLib.devices.get(StimulationDevice.class).getDefaultDevice();
                DeviceCache.connect(device);
                if (device.batteryLevel().needsCharging()) {
                    say("Looks like the batteries need recharging.", "Go replace them, #slave!");
                    device.close(); // disconnect
                }
            } while (!device.connected());
            return device.connected() ? CONNECTED : CANCELLED;
        }
    }

    private StimulationDevice attachElectrodes(StimulationDevice device) {
        // TODO Can't check channel count since the device isn't wired up -> enum physical channels or reset wiring
        // TODO Mixed mode (EStim/Vivrator) is possible, so don't check too much here
        device.setMode(device.output, Wiring.Independent);
        List<Stimulator> stimulators = device.stimulators().stream().filter(EstimOutput).collect(Collectors.toList());

        if (stimulators.isEmpty()) {
            return handleManualDevice(device);
        }

        if (stimulators.size() != 2 || stimulators.stream().filter(ContinousOutput).count() > 0
                || stimulators.stream().filter(VibrationOutput).count() > 0) {
            return handleMoreThanTwoPhysicalChannels();
        } else {
            return wireUpDualPhysicalChannelDevice(device, stimulators);
        }
    }

    private StimulationDevice handleMoreThanTwoPhysicalChannels() {
        throw new UnsupportedOperationException("Only devices with 2 physical estim-channels are supported for now");
    }

    private StimulationDevice handleManualDevice(StimulationDevice device) {
        append("Tell me when you're ready, #slave");
        agree("I'm wired up, #title");
        return device;
    }

    private StimulationDevice wireUpDualPhysicalChannelDevice(StimulationDevice device, List<Stimulator> stimulators) {
        showLengthyInstructions();

        device.play(constantSignal(stimulators, 1, TimeUnit.MINUTES), 60);

        String separate = "Two separate sets of electrodes, #title";
        String oneShared = "One shared electrode, #title";
        String bothShared = "Both electrodes shared, #title";
        String answer = reply(separate, oneShared, bothShared);
        device.stop();
        device.setMode(Output.EStim, answer == separate ? Wiring.Independent : Wiring.INFERENCE_CHANNEL);

        if (answer == bothShared) {
            return mapPhysicalDualDiscreteChannelsToSingleContinuous(device);
        } else {
            return device;
        }
    }

    private void showLengthyInstructions() {
        say("You may now apply the electrodes and connect them to your stimulation device.");
        append(Message.Bullet,
                "Use a separate set of electrodes for each channel to provide me with two independent channels to guide you through the session.");
        append(Message.Bullet,
                "Share one electrode between channels to provide me with an additional inference channel to punish you properly.");
        append(Message.Bullet,
                "Share electrodes between both channels for three levels of signal strength to guide and punish you.");

        append("I've turned on all channels of the controller on pass-through to allow you to play with the signal strength.");
        append(Message.Bullet, "The left channel will set your pace.");
        append(Message.Bullet, "The right channel will be used to tease you.");
        append(Message.Bullet, "The inference channel will be used to discipline you.");

        append("A shared electrode might inflict sharp pain on the body part it's applied to,",
                "but this is just what you deserve.");

        append("Tell me when you're ready, #slave.", "How did you wire yourself up?");
    }

    private StimulationDevice mapPhysicalDualDiscreteChannelsToSingleContinuous(StimulationDevice device) {
        // WIth two channels the same setup as one shared electrode
        // - two physical channels or pace and tease, one inference channel for punishment
        return device;
        // TODO implement continuous signal device proxy or make xinput device behave as such
        // -> stimulator proxy separates code better - it's a stimulator attribute
        // -> setup as single continuous channel device
    }

    private StimulationChannels constantSignal(List<Stimulator> stimulators, long duration, TimeUnit timeUnit) {
        WaveForm waveForm = new ConstantWave(timeUnit.toMillis(duration));
        StimulationChannels channels = new StimulationChannels();
        for (Stimulator stimulator : stimulators) {
            channels.add(new Channel(stimulator, waveForm));
        }
        return channels;
    }

    private StimulationDevice adjustLevels(EStimController stim, StimulationDevice device) {
        EStimController.init(stim, device);

        say("Now adjust the levels acording to your pain tolerance for proper feedback during your training");
        append(Message.Bullet, "Left channel: pace");
        append(Message.Bullet, "Right channel: tease");
        if (device.wiring == Wiring.INFERENCE_CHANNEL) {
            append(Message.Bullet, "Inference channel: discipline");
        }
        append("Let's try it:");

        reply(() -> {
            while (true)
                iterateIntentions(stim);
        }, "All channels adjusted, #title");

        return device;
    }

    private void iterateIntentions(EStimController stim) {
        // TODO What is perceived stronger on pain or tease level:
        // constant input or burst pulse
        // return new ConstantWave(2.0);
        Stimulation stimulation = (stimulator, intensity) -> {
            double mimimalSignalDuration = stimulator.minimalSignalDuration();
            return new BurstSquareWave(2, mimimalSignalDuration, 1.0 - mimimalSignalDuration);
        };

        for (Intention intention : Intention.values()) {
            if (intention == Intention.Pace) {
                replace("Pace!");
            } else if (intention == Intention.Tease) {
                replace("Tease!");
            } else if (intention == Intention.Pain) {
                replace("Pain!");
            }

            stim.play(intention, stimulation);
            stim.complete(intention);

            String nextPlease = "Next please, #title";
            // listen to "next please" input and advance, but keep buttons displayed
            // TODO Include answer from script function -> nested function
            String todoNestedAnswer = "All channels adjusted, #title";
            String answer = reply(nextPlease, todoNestedAnswer);
            if (answer == nextPlease) {
                continue;
            } else {
                Thread.currentThread().interrupt();
            }
        }
    }
}
