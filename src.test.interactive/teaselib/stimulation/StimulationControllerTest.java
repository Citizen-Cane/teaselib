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
import teaselib.stimulation.BodyStimulationController.Type;
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
        BodyStimulationController stim = new BodyStimulationController();
        stim.stimulateAnusAndBalls(anus, balls);
        testWhipStimulation(stim);
        testStimulation(stim);
    }

    @Test
    public void testCockAndAnus() {
        Stimulator cock = StimulationTest.getRightStimulator();
        assertNotNull(cock);
        Stimulator balls = StimulationTest.getLeftStimulator();
        assertNotNull(balls);
        BodyStimulationController stim = new BodyStimulationController();
        stim.stimulateCockAndAnus(cock, balls);
        testWhipStimulation(stim);
        testStimulation(stim);
    }

    private static void testWhipStimulation(BodyStimulationController stim) {
        for (int i = 1; i < 10; i++) {
            stim.stimulate(Type.Attention, 0);
            stim.complete();
            stim.stimulate(Type.Whip, 0);
            stim.stimulate(Type.Walk, 0);
            stim.complete();
        }
    }

    private static void testStimulation(BodyStimulationController stim) {
        int durationSeconds = 10;
        for (int i = 1; i < 10; i++) {
            stim.stimulate(Type.Attention, 0);
            stim.complete();
            stim.stimulate(Type.Whip, 0);
            stim.stimulate(Type.Walk, durationSeconds);
            stim.complete();
            stim.stimulate(Type.Whip, 0);
            stim.stimulate(Type.Trot, durationSeconds);
            stim.complete();
            stim.stimulate(Type.Whip, 0);
            stim.stimulate(Type.Run, durationSeconds);
            stim.complete();
            stim.stimulate(Type.Punish, 0);
            stim.complete();
            if (stim.canStimulate(Type.Tease)) {
                stim.stimulate(Type.Tease, durationSeconds);
                stim.complete();
            }
            if (stim.canStimulate(Type.Cum)) {
                stim.stimulate(Type.Whip, 0);
                stim.stimulate(Type.Cum, durationSeconds);
                stim.complete();
            }
            stim.increaseIntensity();
        }
        stim.stimulate(Type.Punish, 0);
        stim.complete();
        if (stim.canStimulate(Type.Tease)) {
            stim.stimulate(Type.Tease, 60);
            stim.complete();
        }
        if (stim.canStimulate(Type.Cum)) {
            stim.stimulate(Type.Cum, 60);
            stim.complete();
        }
    }
}
