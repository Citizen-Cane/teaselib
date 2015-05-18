/**
 * 
 */
package teaselib.stimulation;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import teaselib.stimulation.pattern.Attention;
import teaselib.stimulation.pattern.Cum;
import teaselib.stimulation.pattern.Punish;
import teaselib.stimulation.pattern.Run;
import teaselib.stimulation.pattern.Tease;
import teaselib.stimulation.pattern.Trot;
import teaselib.stimulation.pattern.Walk;
import teaselib.stimulation.pattern.Whip;

/**
 * @author someone
 *
 */
public class StimulationTest {

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

    private static Stimulator getRoughStimulator() {
        return Stimulators.getAll().get(0);
    }

    private static Stimulator getSmoothStimulator() {
        return Stimulators.getAll().get(1);
    }

    @Test
    public void testGaits() {
        Stimulator stimulator = getRoughStimulator();
        testStimulation(new Walk(stimulator));
        testStimulation(new Trot(stimulator));
        testStimulation(new Run(stimulator));
    }

    @Test
    public void testTease() {
        Stimulator stimulator = getSmoothStimulator();
        testStimulation(new Tease(stimulator, 1.0, 0.05));
    }

    @Test
    public void testAttention() {
        Stimulator stimulator = getSmoothStimulator();
        Stimulation stimulation = new Attention(stimulator);
        for (int i = 1; i <= Stimulation.MaxIntensity; i++) {
            stimulation.play(i, 0);
            try {
                Thread.sleep((long) (1000 * (1.0 + Attention.getSeconds(i))));
            } catch (InterruptedException e) {
                break;
            }
        }
    }

    @Test
    public void testPunish() {
        Stimulator stimulator = getSmoothStimulator();
        Stimulation stimulation = new Punish(stimulator);
        for (int i = 1; i <= Stimulation.MaxIntensity; i++) {
            stimulation.play(i, 0);
            try {
                Thread.sleep((long) (1000 * (1.0 + Punish.getSeconds(i))));
            } catch (InterruptedException e) {
                break;
            }
        }
    }

    @Test
    public void testWhip() {
        Stimulator stimulator = getRoughStimulator();
        Stimulation stimulation = new Whip(stimulator);
        for (int i = 1; i <= Stimulation.MaxIntensity; i++) {
            stimulation.play(i, 0);
            try {
                Thread.sleep((long) (1000 * (1.0 + Whip.getSeconds(i))));
            } catch (InterruptedException e) {
                break;
            }
        }
    }

    @Test
    public void testCum() {
        Stimulator stimulator = getSmoothStimulator();
        testStimulation(new Cum(stimulator));
    }

    static private void testStimulation(Stimulation stimulation) {
        for (int i = 1; i <= Stimulation.MaxIntensity; i++) {
            stimulation.play(i, 5.0 * stimulation.periodDurationSeconds);
            try {
                Thread.sleep((long) (5 * 1000 * stimulation.periodDurationSeconds));
            } catch (InterruptedException e) {
                break;
            }
        }
    }

}
