/**
 * 
 */
package teaselib.stimulation;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import teaselib.stimulation.pattern.Gait;
import teaselib.stimulation.pattern.Run;
import teaselib.stimulation.pattern.Trot;
import teaselib.stimulation.pattern.Walk;

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

    @Test
    public void test() {
        Stimulator stimulator = Stimulators.getAll().get(0);
        testGait(new Walk(stimulator));
        testGait(new Trot(stimulator));
        testGait(new Run(stimulator));
    }

    static private void testGait(Gait gait) {
        for (int i = 1; i <= Stimulation.maxIntensity; i++) {
            gait.play(i, 5.0 * gait.stepDurationSeconds);
            try {
                Thread.sleep((long) (5 * 1000 * gait.stepDurationSeconds));
            } catch (InterruptedException e) {
                break;
            }
        }
    }

}
