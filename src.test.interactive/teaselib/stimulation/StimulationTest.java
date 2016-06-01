/**
 * 
 */
package teaselib.stimulation;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import teaselib.TeaseLib;
import teaselib.hosts.DummyHost;
import teaselib.hosts.DummyPersistence;
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

    public static Stimulator getLeftStimulator() {
        return StimulationDevices.Instance.getDefaultDevice().stimulators()
                .get(0);
    }

    public static Stimulator getRightStimulator() {
        return StimulationDevices.Instance.getDefaultDevice().stimulators()
                .get(1);
    }

    @Test
    public void testEnumerateStimulators() {
        TeaseLib.init(new DummyHost(), new DummyPersistence());
        for (Stimulator stimulator : StimulationDevices.Instance
                .getDefaultDevice().stimulators()) {
            System.out.println(stimulator.getDeviceName());
        }
    }

    @Test
    public void testGaits() {
        TeaseLib.init(new DummyHost(), new DummyPersistence());
        Stimulator stimulator = getRightStimulator();
        testStimulation(new Walk(stimulator));
        testStimulation(new Trot(stimulator));
        testStimulation(new Run(stimulator));
    }

    @Test
    public void testTease() {
        TeaseLib.init(new DummyHost(), new DummyPersistence());
        Stimulator stimulator = getRightStimulator();
        testStimulation(new Tease(stimulator));
    }

    @Test
    public void testAttention() {
        TeaseLib.init(new DummyHost(), new DummyPersistence());
        Stimulator stimulator = getRightStimulator();
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
        TeaseLib.init(new DummyHost(), new DummyPersistence());
        Stimulator stimulator = getLeftStimulator();
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
        TeaseLib.init(new DummyHost(), new DummyPersistence());
        Stimulator stimulator = getLeftStimulator();
        Stimulation stimulation = new Whip(stimulator);
        for (int i = 1; i <= Stimulation.MaxIntensity; i++) {
            stimulation.play(i, 0);
            try {
                Thread.sleep(1000 * 3);
            } catch (InterruptedException e) {
                break;
            }
        }
    }

    @Test
    public void testCum() {
        TeaseLib.init(new DummyHost(), new DummyPersistence());
        Stimulator stimulator = getRightStimulator();
        testStimulation(new Cum(stimulator));
    }

    static private void testStimulation(Stimulation stimulation) {
        for (int i = 1; i <= Stimulation.MaxIntensity; i++) {
            final double durationSeconds = 5.0
                    * stimulation.periodDurationSeconds;
            stimulation.play(i, durationSeconds);
            try {
                Thread.sleep((long) (durationSeconds * 1000));
            } catch (InterruptedException e) {
                break;
            }
        }
    }

    @Test
    public void testAll() {
        TeaseLib.init(new DummyHost(), new DummyPersistence());
        Stimulator a = getLeftStimulator();
        Stimulator c = getRightStimulator();
        final double durationSeconds = 10.0;
        Stimulation[] r = { new Walk(a), new Trot(a), new Walk(a), new Run(a),
                new Run(a), new Run(a), new Run(a) };
        Stimulation[] l = { new Tease(c), new Cum(c), new Tease(c), new Cum(c),
                new Cum(c), new Cum(c), new Cum(c) };
        for (int j = 0; j < r.length; j++) {
            for (int i = 1; i <= Stimulation.MaxIntensity; i++) {
                Stimulation left = l[j];
                Stimulation right = r[j];
                System.out.println(left.toString() + " " + right.toString());
                right.play(i, durationSeconds);
                left.play(i, durationSeconds);
                right.complete();
                left.complete();
            }
        }
    }

}
