/**
 * 
 */
package teaselib.stimulation;

import static org.junit.Assert.assertNotNull;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import teaselib.core.Configuration;
import teaselib.core.devices.Devices;
import teaselib.test.DebugSetup;

/**
 * @author Citizen-Cane
 *
 */
public class StimulationControllerTest {
    private static StimulationDevice device;

    @BeforeClass
    public static void openDefaultDevice() {
        Configuration config = DebugSetup.getConfiguration();
        Devices devices = new Devices(config);

        device = devices.get(StimulationDevice.class).getDefaultDevice();
    }

    @AfterClass
    public static void closeDefaultDevice() {
        device.close();
    }

    public static Stimulator getLeftStimulator() {
        return device.stimulators().get(0);
    }

    public static Stimulator getRightStimulator() {
        return device.stimulators().get(1);
    }

    @Test
    public void testAnusAndBalls() {
        Stimulator anus = getRightStimulator();
        assertNotNull(anus);
        Stimulator balls = getLeftStimulator();
        assertNotNull(balls);
        StimulationController<StimulationType> stim = new StimulationController<StimulationType>();
        StimulationType.stimulateAnusAndBalls(stim, anus, balls);
        testWhipStimulation(stim);
        testStimulation(stim);
    }

    @Test
    public void testCockAndAnus() {
        Stimulator cock = StimulationTest.getRightStimulator();
        assertNotNull(cock);
        Stimulator balls = StimulationTest.getLeftStimulator();
        assertNotNull(balls);
        StimulationController<StimulationType> stim = new StimulationController<StimulationType>();
        StimulationType.stimulateCockAndAnus(stim, cock, balls);
        testWhipStimulation(stim);
        testStimulation(stim);
    }

    private static void testWhipStimulation(StimulationController<StimulationType> stim) {
        for (int i = 1; i < 10; i++) {
            stim.stimulate(StimulationType.Attention, 0);
            stim.complete();
            stim.stimulate(StimulationType.Whip, 0);
            stim.stimulate(StimulationType.Walk, 0);
            stim.complete();
        }
    }

    private static void testStimulation(StimulationController<StimulationType> stim) {
        int durationSeconds = 10;
        for (int i = 1; i < 10; i++) {
            stim.stimulate(StimulationType.Attention, 0);
            stim.complete();
            stim.stimulate(StimulationType.Whip, 0);
            stim.stimulate(StimulationType.Walk, durationSeconds);
            stim.complete();
            stim.stimulate(StimulationType.Whip, 0);
            stim.stimulate(StimulationType.Trot, durationSeconds);
            stim.complete();
            stim.stimulate(StimulationType.Whip, 0);
            stim.stimulate(StimulationType.Run, durationSeconds);
            stim.complete();
            stim.stimulate(StimulationType.Punish, 0);
            stim.complete();
            if (stim.canStimulate(StimulationType.Tease)) {
                stim.stimulate(StimulationType.Tease, durationSeconds);
                stim.complete();
            }
            if (stim.canStimulate(StimulationType.Cum)) {
                stim.stimulate(StimulationType.Whip, 0);
                stim.stimulate(StimulationType.Cum, durationSeconds);
                stim.complete();
            }
            stim.increaseIntensity();
        }
        stim.stimulate(StimulationType.Punish, 0);
        stim.complete();
        if (stim.canStimulate(StimulationType.Tease)) {
            stim.stimulate(StimulationType.Tease, 60);
            stim.complete();
        }
        if (stim.canStimulate(StimulationType.Cum)) {
            stim.stimulate(StimulationType.Cum, 60);
            stim.complete();
        }
    }
}
