package teaselib.core.devices.xinput.stimulation;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.junit.Test;

import teaselib.stimulation.ConstantWave;
import teaselib.stimulation.StimulationDevice;
import teaselib.stimulation.Stimulator;
import teaselib.stimulation.WaveForm;
import teaselib.stimulation.ext.StimulationTarget;
import teaselib.stimulation.ext.StimulationTargets;
import teaselib.stimulation.ext.StimulationTargets.Samples;
import teaselib.stimulation.ext.TestStimulationDevice;
import teaselib.stimulation.ext.TestStimulator;
import teaselib.test.TestException;

/**
 * @author Citizen-Cane
 *
 */
public class StimulationSamplerTaskTest {
    static class TestStimulationSamplerTask extends StimulationSamplerTask {
        final List<Samples> sampled = new ArrayList<>();

        @Override
        void playSamples(Samples samples) {
            double[] values = samples.getValues();
            Samples cloned = new Samples(values.length);
            for (int i = 0; i < values.length; i++) {
                cloned.getValues()[i] = values[i];
            }
            sampled.add(cloned);
        }

        @Override
        void clearSamples() {
            // Ignore
        }
    }

    TestStimulationDevice device = new TestStimulationDevice();

    Stimulator stim1 = device.add(new TestStimulator(device, 1));
    Stimulator stim2 = device.add(new TestStimulator(device, 2));
    Stimulator stim3 = device.add(new TestStimulator(device, 3));

    final StimulationTargets constantSignal = constantSignal(device, 1, TimeUnit.MILLISECONDS);

    private static StimulationTargets constantSignal(StimulationDevice device, long duration, TimeUnit timeUnit) {
        WaveForm waveForm = new ConstantWave(timeUnit.toMillis(duration));
        StimulationTargets targets = new StimulationTargets(device);
        for (Stimulator stimulator : device.stimulators()) {
            targets.set(new StimulationTarget(stimulator, waveForm, 0));
        }
        return targets;
    }

    @Test
    public void testWaveformProcessing() throws Exception {
        try (TestStimulationSamplerTask testSampler = new TestStimulationSamplerTask()) {
            testSampler.play(constantSignal);
            testSampler.complete();

            assertEquals(2, testSampler.sampled.size());
            assertEquals(1.0, testSampler.sampled.get(0).getValues()[0], 0.0);
            assertEquals(1.0, testSampler.sampled.get(0).getValues()[1], 0.0);
            assertEquals(1.0, testSampler.sampled.get(0).getValues()[2], 0.0);
            assertEquals(0.0, testSampler.sampled.get(1).getValues()[0], 0.0);
            assertEquals(0.0, testSampler.sampled.get(1).getValues()[1], 0.0);
            assertEquals(0.0, testSampler.sampled.get(1).getValues()[2], 0.0);
        }
    }

    @Test(expected = TestException.class)
    public void testErrorHandling() throws TestException {
        try (TestStimulationSamplerTask testSampler = new TestStimulationSamplerTask() {
            @Override
            void playSamples(Samples samples) {
                throw new TestException();
            }
        }) {
            testSampler.play(constantSignal);
            testSampler.complete();
        }
    }

    @Test(expected = TestException.class)
    public void testErrorHandlingAfterCancel() throws TestException, InterruptedException {
        try (TestStimulationSamplerTask testSampler = new TestStimulationSamplerTask() {
            @Override
            void playSamples(Samples samples) {
                throw new TestException();
            }
        }) {
            testSampler.play(constantSignal);

            // Do something, wait until error occurs
            Thread.sleep(1000);
            testSampler.stop();

            testSampler.complete();
        }
    }
}
