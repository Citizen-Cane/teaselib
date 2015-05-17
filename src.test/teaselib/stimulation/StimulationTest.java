/**
 * 
 */
package teaselib.stimulation;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

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
        Walk walk = new Walk(stimulator);
        for (int i = 1; i <= Stimulation.maxIntensity; i++) {
            walk.play(i, 5.0 * Walk.StepDurationSeconds);
            try {
                Thread.sleep((long) (5000 * Walk.StepDurationSeconds));
            } catch (InterruptedException e) {
                break;
            }
        }
    }

}
