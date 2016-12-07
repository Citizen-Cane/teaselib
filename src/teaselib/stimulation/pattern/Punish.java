/**
 * 
 */
package teaselib.stimulation.pattern;

import teaselib.stimulation.BurstSquareWave;
import teaselib.stimulation.SquareWave;
import teaselib.stimulation.Stimulation;
import teaselib.stimulation.Stimulator;
import teaselib.stimulation.WaveForm;

/**
 * @author someone
 *
 */
public class Punish extends Stimulation {

    private final PunishType punishType;

    enum PunishType {
        Constant,
        Burst
    }

    public Punish(Stimulator stimulator) {
        this(stimulator, PunishType.Constant);
    }

    public Punish(Stimulator stimulator, PunishType punishType) {
        super(stimulator, 0.0);
        this.punishType = punishType;
    }

    @Override
    public WaveForm waveform(int intensity) {
        double punishSeconds = Punish.getSeconds(intensity);
        if (punishType == PunishType.Constant) {
            return new SquareWave(punishSeconds * 1000, punishSeconds * 1000);
        } else {
            return new BurstSquareWave(punishSeconds * 1000,
                    punishSeconds * 1000, 0.05 * 1000);
        }
    }

    public static double getSeconds(int intensity) {
        return 2.0 * intensity / MaxIntensity;
    }
}
