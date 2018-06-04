package teaselib.stimulation;

import java.util.concurrent.TimeUnit;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import teaselib.core.Configuration;
import teaselib.core.devices.Devices;
import teaselib.core.devices.xinput.stimulation.XInputStimulationDevice;
import teaselib.stimulation.Stimulator.Output;
import teaselib.stimulation.Stimulator.Wiring;
import teaselib.stimulation.ext.EStimController;
import teaselib.stimulation.ext.Intention;
import teaselib.stimulation.ext.StimulationTarget;
import teaselib.stimulation.ext.StimulationTargets;
import teaselib.stimulation.pattern.Attention;
import teaselib.stimulation.pattern.Walk;
import teaselib.stimulation.pattern.Whip;
import teaselib.test.DebugSetup;

public class XInputStimulationControllerTest {
    private static XInputStimulationDevice device;

    @BeforeClass
    public static void openDefaultDevice() {
        Configuration config = DebugSetup.getConfiguration();
        Devices devices = new Devices(config);
        device = (XInputStimulationDevice) devices.get(StimulationDevice.class).getDefaultDevice();
        device.setMode(Output.EStim, Wiring.INFERENCE_CHANNEL);
    }

    @AfterClass
    public static void closeDefaultDevice() {
        device.stop();
        device.close();
    }

    @Test
    public void test() throws InterruptedException {
        EStimController stim = new EStimController();
        EStimController.init(stim, device);

        device.play(constantSignal(device, 1, TimeUnit.MINUTES));
        device.stop();

        Stimulation stimulation = (stimulator, intensity) -> {
            double mimimalSignalDuration = stimulator.minimalSignalDuration();
            return new BurstSquareWave(2, mimimalSignalDuration, 1.0 - mimimalSignalDuration);
        };

        device.play(constantSignal(device, 1, TimeUnit.MINUTES));

        for (Intention intention : Intention.values()) {
            stim.play(intention, stimulation);
            stim.complete(intention);
        }

        // TODO overload method with duration parameter for each channel
        stim.play(Intention.Pain, new Whip(), Intention.Tease, new Attention(), Intention.Pace,
                new Walk().over(5, TimeUnit.SECONDS));
        Thread.sleep(1000);
        stim.play(Intention.Pain, new Whip());

        // TODO makes absolutely sense, since it allows to act in the the middle of a stimulation
        stim.complete(Intention.Pain);

        // TODO Can stop whole device only, and it probably doesn't make sense to stop just one stimulator
        stim.play(Intention.Pace, new Walk().over(5, TimeUnit.SECONDS));
        stim.complete();
        // TODO add append() to stimulation controller
        stim.play(Intention.Pace, new Walk(), 5.0);
        stim.stop(Intention.Pace);

    }

    private static StimulationTargets constantSignal(StimulationDevice device, long duration, TimeUnit timeUnit) {
        WaveForm waveForm = new ConstantWave(timeUnit.toMillis(duration));
        StimulationTargets targets = new StimulationTargets(device);
        for (Stimulator stimulator : device.stimulators()) {
            targets.set(new StimulationTarget(stimulator, waveForm, 0));
        }
        return targets;
    }
}
