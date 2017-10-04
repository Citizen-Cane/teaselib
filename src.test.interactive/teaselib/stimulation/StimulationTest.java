package teaselib.stimulation;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import teaselib.core.Configuration;
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
import teaselib.test.DebugSetup;

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
            System.out.println(stimulator.getDeviceName());
        }
    }

    @Test
    public void testWalk() throws InterruptedException {
        Stimulator stimulator = getRightStimulator();
        testStimulation(new Walk(stimulator));
    }

    @Test
    public void testTrot() throws InterruptedException {
        Stimulator stimulator = getRightStimulator();
        testStimulation(new Trot(stimulator));
    }

    @Test
    public void testRun() throws InterruptedException {
        Stimulator stimulator = getRightStimulator();
        testStimulation(new Run(stimulator));
    }

    @Test
    public void testTease() throws InterruptedException {
        Stimulator stimulator = getRightStimulator();
        testStimulation(new Tease(stimulator));
    }

    @Test
    public void testAttention() throws InterruptedException {
        Stimulator stimulator = getRightStimulator();
        Stimulation stimulation = new Attention(stimulator);
        for (int i = Stimulation.MinIntensity; i <= Stimulation.MaxIntensity; i++) {
            stimulation.play(0, i);
            stimulation.complete();
            Thread.sleep(5000);
        }
    }

    @Test
    public void testPunish() throws InterruptedException {
        Stimulator stimulator = getLeftStimulator();
        Stimulation stimulation = new Punish(stimulator);
        for (int i = Stimulation.MinIntensity; i <= Stimulation.MaxIntensity; i++) {
            stimulation.play(0, i);
            stimulation.complete();
            Thread.sleep(5000);
        }
    }

    @Test
    public void testWhip() throws InterruptedException {
        Stimulator stimulator = getLeftStimulator();
        Stimulation stimulation = new Whip(stimulator);
        for (int i = Stimulation.MinIntensity; i <= Stimulation.MaxIntensity; i++) {
            stimulation.play(0, i);
            stimulation.complete();
            Thread.sleep(5000);
        }
    }

    @Test
    public void testBallMassage() throws InterruptedException {
        Stimulator stimulator = getLeftStimulator();
        Stimulation stimulation = new Cum(stimulator);
        for (int i = Stimulation.MinIntensity; i <= Stimulation.MaxIntensity; i++) {
            stimulation.play(0, i);
            stimulation.complete();
            Thread.sleep(5000);
        }
    }

    @Test
    public void testCum() throws InterruptedException {
        Stimulator stimulator = getRightStimulator();
        testStimulation(new Cum(stimulator));
    }

    static private void testStimulation(Stimulation stimulation) throws InterruptedException {
        double durationSeconds = 30.0;
        for (int i = Stimulation.MinIntensity; i <= Stimulation.MaxIntensity; i++) {
            stimulation.play(durationSeconds, i);
            stimulation.complete();
            Thread.sleep(5000);
        }
    }

    @Test
    public void testAll() throws InterruptedException {
        Stimulator a = getLeftStimulator();
        Stimulator c = getRightStimulator();
        final double durationSeconds = 10.0;
        Stimulation[] r = { new Walk(a), new Trot(a), new Walk(a), new Run(a), new Run(a), new Run(a), new Run(a) };
        Stimulation[] l = { new Tease(c), new Cum(c), new Tease(c), new Cum(c), new Cum(c), new Cum(c), new Cum(c) };
        for (int j = 0; j < r.length; j++) {
            for (int i = Stimulation.MinIntensity; i <= Stimulation.MaxIntensity; i++) {
                Stimulation left = l[j];
                Stimulation right = r[j];
                System.out.println(left.toString() + " " + right.toString());

                right.play(durationSeconds, i);
                left.play(durationSeconds, i);

                right.complete();
                left.complete();

                Thread.sleep(5000);
            }
        }
    }
}
