/**
 * 
 */
package teaselib.stimulation;

import static org.junit.Assert.assertNotNull;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import teaselib.stimulation.Stimulation.Type;

/**
 * @author someone
 *
 */
public class StimulationControllerTest {

    /**
     * @throws java.lang.Exception
     */
    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
    }

    /**
     * @throws java.lang.Exception
     */
    @AfterClass
    public static void tearDownAfterClass() throws Exception {
    }

    /**
     * @throws java.lang.Exception
     */
    @Before
    public void setUp() throws Exception {
    }

    /**
     * @throws java.lang.Exception
     */
    @After
    public void tearDown() throws Exception {
    }

    // @Test
    // public void testAnusAndBalls() {
    // Stimulator anus = Stimulators.getAll().get(0);
    // Stimulator balls = Stimulators.getAll().get(1);
    // StimulationController<Stimulation.Type> stim = StimulationController
    // .stimulateAnusAndBalls(anus, balls);
    // }

    @Test
    public void testCockAndAnus() {
        Stimulator cock = StimulationTest.getRightStimulator();
        assertNotNull(cock);
        Stimulator balls = StimulationTest.getLeftStimulator();
        assertNotNull(balls);
        StimulationController<Stimulation.Type> stim = StimulationController
                .stimulateCockAndAnus(cock, balls);
        testWhipStimulation(stim);
        testStimulation(stim);
    }

    private static void testWhipStimulation(
            StimulationController<Stimulation.Type> stim) {
        for (int i = 1; i < 10; i++) {
            stim.stimulate(Type.Attention, 0);
            stim.complete();
            stim.stimulate(Type.Whip, 0);
            stim.stimulate(Type.Walk, 0);
            stim.complete();
        }
    }

    private static void testStimulation(
            StimulationController<Stimulation.Type> stim) {
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
