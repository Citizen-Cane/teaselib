package teaselib.stimulation;

public class ConstantWave extends SquareWave {

    public ConstantWave(double seconds) {
        super(seconds, 0.0);
    }

    public ConstantWave(long durationMillis) {
        super(durationMillis, 0);
    }
}
