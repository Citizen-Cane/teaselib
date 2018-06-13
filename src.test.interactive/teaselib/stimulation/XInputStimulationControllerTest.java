package teaselib.stimulation;

import java.util.concurrent.TimeUnit;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
import teaselib.stimulation.pattern.Repeat;
import teaselib.stimulation.pattern.Walk;
import teaselib.stimulation.pattern.Whip;
import teaselib.test.DebugSetup;

public class XInputStimulationControllerTest {
    private static final Logger logger = LoggerFactory.getLogger(XInputStimulationControllerTest.class);

    private static XInputStimulationDevice device;

    @BeforeClass
    public static void openDefaultDevice() {
        Configuration config = DebugSetup.getConfiguration();
        Devices devices = new Devices(config);
        device = (XInputStimulationDevice) devices.get(StimulationDevice.class).getDefaultDevice();
        // TODO Too late, since stims are enumerated in constructor
        // TODO Name of inference channel wrong
        // TODO Inference channel is likely the wrong stimulator - but it's just a place holder since vibration is now
        // set in device
        // TODO stimulation targets start with 0ms time stamps or duration - no noticeable signal - debug all targets
        // sample streams
        device.setMode(Output.EStim, Wiring.INFERENCE_CHANNEL);
    }

    @AfterClass
    public static void closeDefaultDevice() {
        device.stop();
        device.close();
    }

    @Test
    public void testStimulationSampling() throws InterruptedException {
        logger.info("Connected to {}", device);
        device.setMode(Output.Vibration, Wiring.INFERENCE_CHANNEL);
        EStimController stim = new EStimController();
        EStimController.init(stim, device);

        StimulationTargets constantSignal = constantSignal(device, 1, TimeUnit.MINUTES);
        logger.info("Playing {}", constantSignal);
        device.play(constantSignal);
        sleep(1000);
        logger.info("Stop");
        device.stop();

        Stimulation stimulation = (stimulator, intensity) -> {
            double mimimalSignalDuration = stimulator.minimalSignalDuration();
            return new BurstSquareWave(2, mimimalSignalDuration, 1.0 - mimimalSignalDuration);
        };

        logger.info("Playing {}", constantSignal);
        device.play(constantSignal);

        logger.info("Replacing stimulation targets:");
        for (Intention intention : Intention.values()) {
            logger.info("Playing {} {}", intention, stimulation);
            stim.play(intention, stimulation);
            logger.info("Completing");
            // TODO completing single intention doesn't work yet - blocks 1 minute to complete all
            // stim.complete(intention);
        }
        stim.stop();

        logger.info("Playing multiple patterns");
        // TODO overload method with duration parameter for each channel
        Whip whip = new Whip();
        Walk walkRepeated = new Walk();
        Repeat walkRendered = walkRepeated.over(2, TimeUnit.SECONDS);
        stim.play(Intention.Pain, whip, Intention.Tease, new Attention(), Intention.Pace, walkRendered);
        logger.info("Waiting");
        sleep(2000);

        logger.info("While teasing and setting pace: Playing {}", whip);
        stim.play(Intention.Pain, whip);

        // TODO makes absolutely sense, since it allows to act in the the middle of a stimulation
        logger.info("Completing whip");
        stim.complete(Intention.Pain);

        // TODO add append() methods to stimulation controller
        logger.info("Playing Walk rendered {}", walkRendered);
        stim.play(Intention.Pace, walkRendered);
        logger.info("Completing");
        stim.complete();

        logger.info("Playing Walk repeated {}", walkRepeated);
        stim.play(Intention.Pace, walkRepeated, 2.0);
        logger.info("Completing");
        stim.complete();

        logger.info("Playing Walk repeated {}", walkRepeated);
        stim.play(Intention.Pace, walkRepeated, 2.0);
        logger.info("Stopping");
        stim.stop(Intention.Pace);
    }

    private static void sleep(long millis) throws InterruptedException {
        Thread.sleep(millis);
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
