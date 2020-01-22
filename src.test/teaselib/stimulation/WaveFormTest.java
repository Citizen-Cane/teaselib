package teaselib.stimulation;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import teaselib.stimulation.pattern.Whip;

public class WaveFormTest {
    private static final Logger logger = LoggerFactory.getLogger(WaveFormTest.class);

    @Test
    public void testSampleDurationAndValues() {
        SquareWave squareWave = new SquareWave(0.5, 0.5);

        assertEquals(1000, squareWave.getDurationMillis());

        assertEquals(0, squareWave.nextTime(-1));
        assertEquals(500, squareWave.nextTime(0));
        assertEquals(1000, squareWave.nextTime(500));
        assertEquals(Long.MAX_VALUE, squareWave.nextTime(1000));

        assertEquals(0.0, squareWave.getValue(-1), 0.0);
        assertEquals(1.0, squareWave.getValue(0), 0.0);
        assertEquals(1.0, squareWave.getValue(499), 0.0);
        assertEquals(0.0, squareWave.getValue(500), 0.0);
        assertEquals(0.0, squareWave.getValue(1000), 0.0);
    }

    static final class TestStimulator implements Stimulator {
        WaveForm waveForm = null;

        @Override
        public String getName() {
            return getClass().getSimpleName();
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
        public Signal signal() {
            return Signal.Discrete;
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
        Whip whip = new Whip(1, 1.0);
        stim.play(whip.waveform(stim, 0), 0, 0);
        logger.info("{}", stim.waveForm);

        assertEquals(2, stim.waveForm.size());
    }

    @Test
    public void testMultiWhip() {
        TestStimulator stim = new TestStimulator();
        Whip whip = new Whip(2, 0.2);
        stim.play(whip.waveform(stim, 0), 0, 0);
        logger.info("{}", stim.waveForm);

        assertEquals(4, stim.waveForm.size());
    }

    @Test
    public void testSliceFull() {
        WaveForm waveForm = getSlicableWaveform();
        WaveForm slice = waveForm.slice(0, waveForm.getDurationMillis());
        assertEquals(waveForm, slice);

        slice = waveForm.slice(0);
        assertEquals(waveForm, slice);
    }
    // TODO Test slicing variants: all, start and end mid-samples, start and end on sample boundaries

    @Test
    public void testSliceStart() {
        WaveForm slice = getSlicableWaveform().slice(1000);
        assertEquals(0.75, slice.getValue(0), 0.0);
        assertEquals(0.5, slice.getValue(1000), 0.0);
        assertEquals(0.25, slice.getValue(2000), 0.0);
        assertEquals(0.0, slice.getValue(3000), 0.0);
    }

    @Test
    public void testSliceStartPartialFirstSample() {
        WaveForm slice = getSlicableWaveform().slice(500);
        assertEquals(1.0, slice.getValue(0), 0.0);
        assertEquals(1.0, slice.getValue(499), 0.0);
        assertEquals(0.75, slice.getValue(500), 0.0);
        assertEquals(0.75, slice.getValue(1499), 0.0);
        assertEquals(0.5, slice.getValue(1500), 0.0);
        assertEquals(0.25, slice.getValue(2500), 0.0);
        assertEquals(0.25, slice.getValue(3499), 0.0);
        assertEquals(0.0, slice.getValue(3500), 0.0);
    }

    @Test
    public void testSliceStartPartialSecondSample() {
        WaveForm slice = getSlicableWaveform().slice(1750);
        assertEquals(0.75, slice.getValue(0), 0.0);
        assertEquals(0.75, slice.getValue(249), 0.0);
        assertEquals(0.5, slice.getValue(250), 0.0);
        assertEquals(0.5, slice.getValue(1249), 0.0);
        assertEquals(0.25, slice.getValue(1250), 0.0);
        assertEquals(0.25, slice.getValue(2249), 0.0);
        assertEquals(0.0, slice.getValue(2250), 0.0);
    }

    @Test
    public void testSliceEnd() {
        WaveForm slice = getSlicableWaveform().slice(0, 3000);
        assertEquals(1.0, slice.getValue(0), 0.0);
        assertEquals(1.0, slice.getValue(999), 0.0);
        assertEquals(0.75, slice.getValue(1000), 0.0);
        assertEquals(0.75, slice.getValue(1999), 0.0);
        assertEquals(0.5, slice.getValue(2000), 0.0);
        assertEquals(0.5, slice.getValue(2999), 0.0);
        assertEquals(0.0, slice.getValue(3000), 0.0);
    }

    @Test
    public void testSliceEndPartialLastSample() {
        WaveForm slice = getSlicableWaveform().slice(0, 3500);
        assertEquals(1.0, slice.getValue(0), 0.0);
        assertEquals(1.0, slice.getValue(999), 0.0);
        assertEquals(0.75, slice.getValue(1000), 0.0);
        assertEquals(0.75, slice.getValue(1999), 0.0);
        assertEquals(0.5, slice.getValue(2000), 0.0);
        assertEquals(0.5, slice.getValue(2999), 0.0);
        assertEquals(0.25, slice.getValue(3000), 0.0);
        assertEquals(0.25, slice.getValue(3499), 0.0);
        assertEquals(0.0, slice.getValue(3500), 0.0);
    }

    @Test
    public void testSliceEndPartialSecondLastSample() {
        WaveForm slice = getSlicableWaveform().slice(0, 2250);
        assertEquals(1.0, slice.getValue(0), 0.0);
        assertEquals(1.0, slice.getValue(999), 0.0);
        assertEquals(0.75, slice.getValue(1000), 0.0);
        assertEquals(0.75, slice.getValue(1999), 0.0);
        assertEquals(0.5, slice.getValue(2000), 0.0);
        assertEquals(0.5, slice.getValue(2249), 0.0);
        assertEquals(0.0, slice.getValue(2250), 0.0);
    }

    @Test
    public void testSliceMidPartial() {
        WaveForm slice = getSlicableWaveform().slice(200, 2600);
        assertEquals(1.0, slice.getValue(0), 0.0);
        assertEquals(1.0, slice.getValue(799), 0.0);
        assertEquals(0.75, slice.getValue(800), 0.0);
        assertEquals(0.75, slice.getValue(1799), 0.0);
        assertEquals(0.5, slice.getValue(1800), 0.0);
        assertEquals(0.5, slice.getValue(2399), 0.0);
        assertEquals(0.0, slice.getValue(2400), 0.0);
    }

    private static WaveForm getSlicableWaveform() {
        WaveForm waveForm = new WaveForm();
        waveForm.add(1.0, 1000);
        waveForm.add(0.75, 1000);
        waveForm.add(0.5, 1000);
        waveForm.add(0.25, 1000);

        assertEquals(4000, waveForm.getDurationMillis());

        assertEquals(0, waveForm.nextTime(-1));
        assertEquals(1000, waveForm.nextTime(0));
        assertEquals(2000, waveForm.nextTime(1000));
        assertEquals(Long.MAX_VALUE, waveForm.nextTime(4000));

        assertEquals(0.0, waveForm.getValue(-1), 0.0);
        assertEquals(1.0, waveForm.getValue(0), 0.0);
        assertEquals(1.0, waveForm.getValue(1), 0.0);
        assertEquals(0.75, waveForm.getValue(1500), 0.0);
        assertEquals(0.25, waveForm.getValue(3250), 0.0);
        return waveForm;
    }
}
