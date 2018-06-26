package teaselib.stimulation.pattern;

import teaselib.stimulation.BurstSquareWave;
import teaselib.stimulation.ConstantWave;
import teaselib.stimulation.Stimulation;
import teaselib.stimulation.Stimulator;
import teaselib.stimulation.WaveForm;

/**
 * @author Citizen-Cane
 *
 */
public class Punish implements Stimulation {
    static final double MinOnDurationSeconds = 2.0;
    static final double IntensityFactor = 0.25;

    public enum Type {
        Long,
        Smack,
        Burst
    }

    private final Type punishType;

    public Punish() {
        this(Type.Long);
    }

    public Punish(Type punishType) {
        this.punishType = punishType;
    }

    @Override
    public WaveForm waveform(Stimulator stimulator, int intensity) {
        double punishSeconds = MinOnDurationSeconds + IntensityFactor * intensity;

        if (punishType == Type.Long) {
            return new ConstantWave(punishSeconds);
        } else if (punishType == Type.Smack) {
            return new ConstantWave(punishSeconds / 4);
        } else if (punishType == Type.Burst) {
            return new BurstSquareWave((int) punishSeconds, 0.80, 0.20);
        } else {
            throw new IllegalArgumentException(punishType.toString());
        }
    }
}
