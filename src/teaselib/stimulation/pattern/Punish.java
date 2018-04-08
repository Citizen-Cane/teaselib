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
public class Punish extends Stimulation {
    static final double MinOnDurationSeconds = 2.0;
    static final double IntensityFactor = 0.25;

    enum PunishType {
        Constant,
        Burst
    }

    private final PunishType punishType;

    public Punish() {
        this(PunishType.Constant);
    }

    public Punish(PunishType punishType) {
        this.punishType = punishType;
    }

    @Override
    public WaveForm waveform(Stimulator stimulator, int intensity) {
        double punishSeconds = MinOnDurationSeconds + IntensityFactor * intensity;

        if (punishType == PunishType.Constant) {
            return new ConstantWave(punishSeconds);
        } else if (punishType == PunishType.Burst) {
            return new BurstSquareWave((int) punishSeconds, 0.80, 0.20);
        } else {
            throw new IllegalArgumentException(punishType.toString());
        }
    }
}
