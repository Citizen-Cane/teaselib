/**
 * 
 */
package teaselib.stimulation;

import static org.junit.Assert.assertNotNull;

import org.junit.Test;

/**
 * @author someone
 *
 */
public class StimulationControllerTest {

    @Test
    public void testAnusAndBalls() {
        Stimulator anus = StimulationTest.getRightStimulator();
        assertNotNull(anus);
        Stimulator balls = StimulationTest.getLeftStimulator();
        assertNotNull(balls);
        StimulationController<StimulationType> stim = StimulationType
                .stimulateAnusAndBalls(anus, balls);
        testWhipStimulation(stim);
        testStimulation(stim);
    }

    @Test
    public void testCockAndAnus() {
        Stimulator cock = StimulationTest.getRightStimulator();
        assertNotNull(cock);
        Stimulator balls = StimulationTest.getLeftStimulator();
        assertNotNull(balls);
        StimulationController<StimulationType> stim = StimulationType
                .stimulateCockAndAnus(cock, balls);
        testWhipStimulation(stim);
        testStimulation(stim);
    }

    private static void testWhipStimulation(
            StimulationController<StimulationType> stim) {
        for (int i = 1; i < 10; i++) {
            stim.stimulate(StimulationType.Attention, 0);
            stim.complete();
            stim.stimulate(StimulationType.Whip, 0);
            stim.stimulate(StimulationType.Walk, 0);
            stim.complete();
        }
    }

    private static void testStimulation(
            StimulationController<StimulationType> stim) {
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
