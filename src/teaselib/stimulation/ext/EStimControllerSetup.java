package teaselib.stimulation.ext;

import java.util.ArrayList;
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
        StimulationDevice device = getDevice();
        device.setMode(Output.EStim, Wiring.INFERENCE_CHANNEL);
        EStimController stim = getController();
        EStimController.init(stim, device);
        adjustLevels(stim, attachElectrodes(device));
        return stim;
    }

    private StimulationDevice getDevice() {
        Chooser chooser = new Chooser();
        chooser.tryConnect();
        if (chooser.device.connected()) {
            return chooser.device;
        } else {
            say("Get your EStim device and turn on the controller!");
            String manualDevice = "I only have a manual device";
            String notAvailable = "I'm sorry, #title, but I don't have any";
            // String result = reply(chooser::supplyDebugInstance, manualDevice, notAvailable);
            String result = reply(chooser::connect, manualDevice, notAvailable);
            if (result == Chooser.CONNECTED) {
                return chooser.device;
            } else {
                return StimulationDevice.MANUAL;
            }
        }
    }

    class Chooser {
        static final String CONNECTED = "Device Connected";
        static final String CANCELLED = "Cancelled";
        StimulationDevice device;

        String supplyDebugInstance() {
            say("Supplying debug instance.");
            device = teaseLib.devices.get(StimulationDevice.class).getDefaultDevice();
            return CONNECTED;
        }

        String connectDevice() {
            do {
                connect();
            } while (!device.connected());
            return device.connected() ? CONNECTED : CANCELLED;
        }

        void tryConnect() {
            device = teaseLib.devices.get(StimulationDevice.class).getDefaultDevice();
            DeviceCache.connect(device, 0);
            if (device.connected()) {
                checkBatteries();
            }
        }

        void connect() {
            device = teaseLib.devices.get(StimulationDevice.class).getDefaultDevice();
            DeviceCache.connect(device);
            checkBatteries();
        }

        private void checkBatteries() {
            if (device.batteryLevel().needsCharging()) {
                say("Looks like the batteries need recharging.", "Go replace them, #slave!");
                device.close(); // disconnect
            }
        }
    }

    private StimulationDevice attachElectrodes(StimulationDevice device) {
        // TODO Can't check channel count since the device isn't wired up -> enum physical channels or reset wiring
        // TODO Mixed mode (EStim/Vibrator) is possible, so don't check too much here - handle in instructions instead
        device.setMode(device.output, Wiring.Independent);

        List<Stimulator> estims = device.stimulators().stream().filter(EstimOutput).collect(Collectors.toList());
        List<Stimulator> vibrators = device.stimulators().stream().filter(VibrationOutput).collect(Collectors.toList());

        if (estims.isEmpty() && vibrators.isEmpty()) {
            return handleManualDevice(device);
        }

        // TODO Handle vibrators first -> vib at penis tip or in cunt, then estim punishment
        if (estims.size() != 2 || estims.stream().filter(ContinousOutput).count() > 0
                || estims.stream().filter(VibrationOutput).count() > 0) {
            return handleMoreThanTwoPhysicalChannels();
        } else {
            return wireUpDualPhysicalChannelDevice(device);
        }
    }

    private StimulationDevice handleManualDevice(StimulationDevice device) {
        append("Tell me when you've wired yourself up and are ready to test, #slave.");
        agree("I'm wired up, #title");
        return device;
    }

    private static StimulationDevice handleMoreThanTwoPhysicalChannels() {
        throw new UnsupportedOperationException("Only devices with 2 physical estim-channels are supported for now");
    }

    private StimulationDevice wireUpDualPhysicalChannelDevice(StimulationDevice device) {
        // TODO instructions that work for mixed devices - mixed estim/vibration setups work
        // - just test channels for output and adjust instructions
        // TODO add device selector and signal type selector (estim/vibration)

        wireUpDualPhysicalChannelDeviceInstructions();
        device.play(constantSignal(device, 1, TimeUnit.MINUTES));

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

    private void wireUpDualPhysicalChannelDeviceInstructions() {
        show("You may now apply the electrodes and connect them to your stimulation device.");
        append(Message.Bullet,
                "Use a separate set of electrodes for each channel to provide me with two independent channels to guide you through the session.");
        append(Message.Bullet,
                "Share one electrode between channels to provide me with an additional inference channel to punish you properly.");
        append(Message.Bullet,
                "Share electrodes between both channels for three levels of signal strength to guide and punish you.");

        append("I've set all channels to pass-through to allow you to play with the signal strength.");
        append(Message.Bullet, "The left channel will set your pace.");
        append(Message.Bullet, "The right channel will be used to tease you.");
        append(Message.Bullet, "The inference channel on the shared electrodes will be used to discipline you.");

        append("A shared electrode might inflict sharp pain on the body part it's applied to,",
                "but this is just what you deserve.");

        append("Tell me when you're ready, #slave.", "How did you wire yourself up?");
    }

    private static StimulationDevice mapPhysicalDualDiscreteChannelsToSingleContinuous(StimulationDevice device) {
        // WIth two channels the same setup as one shared electrode
        // - two physical channels or pace and tease, one inference channel for punishment
        return device;
        // TODO implement continuous signal device proxy or make xinput device behave as such
        // -> stimulator proxy separates code better - it's a stimulator attribute
        // -> setup as single continuous channel device
    }

    private static StimulationTargets constantSignal(StimulationDevice device, long duration, TimeUnit timeUnit) {
        WaveForm waveForm = new ConstantWave(timeUnit.toMillis(duration));
        StimulationTargets targets = new StimulationTargets(device);
        for (Stimulator stimulator : device.stimulators()) {
            targets.set(stimulator, waveForm, 0, timeUnit);
        }
        return targets;
    }

    private void adjustLevels(EStimController stim, StimulationDevice device) {
        if (!device.stimulators().isEmpty()) {
            adjustlevelsInstructions(device);
            append("Let's try it:");
            testIntentions(stim);
        }
    }

    private void adjustlevelsInstructions(StimulationDevice device) {
        say("Now adjust the levels acording to your pain tolerance for proper feedback during your training:");
        append(Message.Bullet, "Left channel: pace");
        append(Message.Bullet, "Right channel: tease");
        if (device.wiring == Wiring.INFERENCE_CHANNEL) {
            append(Message.Bullet, "Inference channel: discipline");
        }
    }

    private boolean testIntentions(EStimController stim) {
        // TODO What is perceived stronger on pain or tease level:
        // constant input or burst pulse?
        // return new ConstantWave(2.0);
        Stimulation stimulation = (stimulator, intensity) -> {
            double mimimalSignalDuration = stimulator.minimalSignalDuration();
            return new BurstSquareWave(2, mimimalSignalDuration, 1.0 - mimimalSignalDuration);
        };

        List<String> answers = new ArrayList<>();
        for (Intention intention : Intention.values()) {
            answers.add(intention.name());
        }
        String allAdjusted = "All channels adjusted, #title";
        answers.add(allAdjusted);

        while (true) {
            String answer = reply(answers);
            if (answer == allAdjusted) {
                break;
            }

            if (answer.equals(Intention.Pace.name())) {
                replace("Pace!");
            } else if (answer.equals(Intention.Tease.name())) {
                replace("Tease!");
            } else if (answer.equals(Intention.Pain.name())) {
                replace("Pain!");
            }

            Intention intention = Intention.values()[answers.indexOf(answer)];
            stim.play(intention, stimulation);
            stim.complete(intention);
        }

        return true;
    }
}
