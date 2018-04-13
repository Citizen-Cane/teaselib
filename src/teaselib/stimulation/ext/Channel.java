package teaselib.stimulation.ext;

import java.util.List;
import java.util.Optional;

import teaselib.stimulation.Stimulator;
import teaselib.stimulation.WaveForm;

public class Channel {
    public final Stimulator stimulator;
    public final WaveForm waveForm;
    public final long startMillis;

    public Channel(Stimulator stimulator, WaveForm waveForm, long startMillis) {
        this.stimulator = stimulator;
        this.waveForm = waveForm;
        this.startMillis = startMillis;
    }

    public WaveForm getWaveForm() {
        return waveForm;
    }

    public static Channel maxDuration(Channel a, Channel b) {
        return a.waveForm.getDurationMillis() > b.waveForm.getDurationMillis() ? a : b;
    }

    public double getValue(long timeStampMillis) {
        return waveForm.getValue(startMillis + timeStampMillis);
    }

    public static long nextTimeStamp(List<Channel> channels, long currentTimeMillis) {
        Optional<Long> min = channels.stream().map(Channel::getWaveForm)
                .map((WaveForm waveform) -> waveform.nextTime(currentTimeMillis)).reduce(Math::min);
        if (min.isPresent()) {
            return min.get();
        } else {
            throw new IllegalArgumentException(channels.toString());
        }
    }

    public static void getSamples(List<Channel> channels, long timeStampMillis, double[] values) {
        for (int i = 0; i < values.length; i++) {
            values[i] = channels.get(i).getValue(timeStampMillis);
        }
    }

    @Override
    public String toString() {
        return stimulator + "->" + waveForm;
    }
}