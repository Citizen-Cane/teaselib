package teaselib.stimulation;

import java.util.Arrays;
import java.util.List;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import teaselib.core.configuration.Configuration;
import teaselib.core.configuration.DebugSetup;
import teaselib.core.devices.Device;
import teaselib.core.devices.Devices;
import teaselib.stimulation.pattern.Attention;
import teaselib.stimulation.pattern.Cum;
import teaselib.stimulation.pattern.Punish;
import teaselib.stimulation.pattern.Run;
import teaselib.stimulation.pattern.Tease;
import teaselib.stimulation.pattern.Trot;
import teaselib.stimulation.pattern.Walk;
import teaselib.stimulation.pattern.Whip;

public class StimulationTest {
    private static final Logger logger = LoggerFactory.getLogger(StimulationTest.class);

    private static StimulationDevice device;

    @BeforeClass
    public static void openDefaultDevice() {
        Configuration config = DebugSetup.getConfiguration();
        Devices devices = new Devices(config);

        device = devices.get(StimulationDevice.class).getDefaultDevice();

        logger.info("Battery-Level is " + device.batteryLevel().toString());
    }

    @AfterClass
    public static void closeDefaultDevice() {
        for (Stimulator stimulator : device.stimulators()) {
            stimulator.stop();
        }
        device.close();
    }

    public static Stimulator getLeftStimulator() {
        return device.stimulators().get(0);
    }

    public static Stimulator getRightStimulator() {
        return device.stimulators().get(1);
    }

    @Test
    public void testEnumerateStimulators() {
        System.out.println(device.getDevicePath() + (device.connected() ? "" : ":" + Device.WaitingForConnection));
        for (Stimulator stimulator : device.stimulators()) {
            System.out.println(stimulator.getName());
        }
    }

    @Test
    public void testWalk() throws InterruptedException {
        testStimulation(getRightStimulator(), new Walk());
    }

    @Test
    public void testTrot() throws InterruptedException {
        testStimulation(getRightStimulator(), new Trot());
    }

    @Test
    public void testRun() throws InterruptedException {
        testStimulation(getRightStimulator(), new Run());
    }

    @Test
    public void testTease() throws InterruptedException {
        testStimulation(getRightStimulator(), new Tease());
    }

    @Test
    public void testAttention() throws InterruptedException {
        Stimulator stimulator = getRightStimulator();
        Stimulation stimulation = new Attention();
        for (int i = Stimulation.MinIntensity; i <= Stimulation.MaxIntensity; i++) {
            stimulator.play(stimulation.waveform(stimulator, i), 0, i);
            stimulator.complete();
            Thread.sleep(5000);
        }
    }

    @Test
    public void testPunish() throws InterruptedException {
        Stimulator stimulator = getLeftStimulator();
        Stimulation stimulation = new Punish();
        for (int i = Stimulation.MinIntensity; i <= Stimulation.MaxIntensity; i++) {
            stimulator.play(stimulation.waveform(stimulator, i), 0, i);
            stimulator.complete();
            Thread.sleep(5000);
        }
    }

    @Test
    public void testWhip() throws InterruptedException {
        Stimulator stimulator = getLeftStimulator();
        Stimulation stimulation = new Whip();
        for (int i = Stimulation.MinIntensity; i <= Stimulation.MaxIntensity; i++) {
            stimulator.play(stimulation.waveform(stimulator, i), 0, i);
            stimulator.complete();
            Thread.sleep(5000);
        }
    }

    @Test
    public void testBallMassage() throws InterruptedException {
        Stimulator stimulator = getLeftStimulator();
        Stimulation stimulation = new Cum();
        for (int i = Stimulation.MinIntensity; i <= Stimulation.MaxIntensity; i++) {
            stimulator.play(stimulation.waveform(stimulator, i), 0, i);
            stimulator.complete();
            Thread.sleep(5000);
        }
    }

    @Test
    public void testCum() throws InterruptedException {
        testStimulation(getRightStimulator(), new Cum());
    }

    static private void testStimulation(Stimulator stimulator, Stimulation stimulation) throws InterruptedException {
        double durationSeconds = 30.0;
        for (int i = Stimulation.MinIntensity; i <= Stimulation.MaxIntensity; i++) {
            stimulator.play(stimulation.waveform(stimulator, i), durationSeconds, i);
            stimulator.complete();
            Thread.sleep(5000);
        }
    }

    @Test
    public void testAll() throws InterruptedException {
        Stimulator a = getLeftStimulator();
        Stimulator c = getRightStimulator();
        final double durationSeconds = 10.0;
        List<Stimulation> r = Arrays.asList(new Walk(), new Trot(), new Walk(), new Run(), new Run(), new Run(),
                new Run());
        List<Stimulation> l = Arrays.asList(new Tease(), new Cum(), new Tease(), new Cum(), new Cum(), new Cum(),
                new Cum());
        for (int j = 0; j < r.size(); j++) {
            for (int i = Stimulation.MinIntensity; i <= Stimulation.MaxIntensity; i++) {
                Stimulation left = l.get(j);
                Stimulation right = r.get(j);
                System.out.println(left.toString() + " " + right.toString());

                a.play(right.waveform(a, i), durationSeconds, i);
                c.play(left.waveform(c, i), durationSeconds, i);

                a.complete();
                c.complete();

                Thread.sleep(5000);
            }
        }
    }
}
