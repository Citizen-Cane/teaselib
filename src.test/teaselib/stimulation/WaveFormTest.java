package teaselib.stimulation;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import teaselib.stimulation.pattern.Whip;

public class WaveFormTest {
    private static final Logger logger = LoggerFactory.getLogger(WaveFormTest.class);

    private static final class TestStimulator implements Stimulator {
        WaveForm waveForm = null;

        @Override
        public String getDeviceName() {
            return getClass().getSimpleName();
        }

        @Override
        public String getLocation() {
            return "Virtual Rumble Motor";
        }

        @Override
        public StimulationDevice getDevice() {
            return null;
        }

        @Override
        public ChannelDependency channelDependency() {
            return ChannelDependency.Independent;
        }

        @Override
        public Output output() {
            return Output.EStim;
        }

        @Override
        public double minimalSignalDuration() {
            return 0.05;
        }

        @Override
        public void play(WaveForm waveForm, double durationSeconds, double strength) {
            this.waveForm = waveForm;
        }

        @Override
        public void extend(double durationSeconds) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void stop() {
            throw new UnsupportedOperationException();
        }

        @Override
        public void complete() {
            throw new UnsupportedOperationException();
        }
    }

    @Test
    public void testWhip() {
        TestStimulator stim = new TestStimulator();
        Whip whip = new Whip(stim, 1, 0);
        whip.play(0, 0);
        logger.info("{}", stim.waveForm);

        assertEquals(2, stim.waveForm.size());
    }

    @Test
    public void testMultiWhip() {
        TestStimulator stim = new TestStimulator();
        Whip whip = new Whip(stim, 2, 0.2);
        whip.play(0, 0);
        logger.info("{}", stim.waveForm);

        assertEquals(4, stim.waveForm.size());
    }
}
