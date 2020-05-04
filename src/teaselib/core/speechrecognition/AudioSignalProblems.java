package teaselib.core.speechrecognition;

import java.util.EnumMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicInteger;

public class AudioSignalProblems {
    private final Map<AudioSignalProblem, AtomicInteger> problems = new EnumMap<>(
            AudioSignalProblem.class);
    private final Map<AudioSignalProblem, Integer> limits = new EnumMap<>(AudioSignalProblem.class);
    private final Map<AudioSignalProblem, Double> penalties = new EnumMap<>(AudioSignalProblem.class);

    public AudioSignalProblems() {
        clear();
        setLimits();
        setPenalties();
    }

    private void setLimits() {
        limits.put(AudioSignalProblem.None, Integer.MAX_VALUE);
        limits.put(AudioSignalProblem.Noise, 1);
        limits.put(AudioSignalProblem.NoSignal, 1);
        limits.put(AudioSignalProblem.TooLoud, 1);
        limits.put(AudioSignalProblem.TooQuiet, 1);
        limits.put(AudioSignalProblem.TooFast, 1);
        limits.put(AudioSignalProblem.TooSlow, 1);
    }

    private void setPenalties() {
        penalties.put(AudioSignalProblem.None, 0.0);
        penalties.put(AudioSignalProblem.Noise, 1.0);
        penalties.put(AudioSignalProblem.NoSignal, 1.0);
        penalties.put(AudioSignalProblem.TooLoud, 1.0);
        penalties.put(AudioSignalProblem.TooQuiet, 1.0);
        penalties.put(AudioSignalProblem.TooFast, 1.0);
        penalties.put(AudioSignalProblem.TooSlow, 1.0);
    }

    public void clear() {
        problems.clear();
    }

    public void add(AudioSignalProblem audioSignalProblem) {
        problems.getOrDefault(audioSignalProblem, new AtomicInteger(0)).incrementAndGet();
    }

    public boolean exceedLimits() {
        for (Entry<AudioSignalProblem, AtomicInteger> problem : problems.entrySet()) {
            if (problem.getValue().get() > limits.get(problem.getKey())) {
                return true;
            }
        }
        return false;
    }

    public double penalty() {
        double penalty = 0.0;
        for (Entry<AudioSignalProblem, AtomicInteger> problem : problems.entrySet()) {
            penalty += penalties.get(problem.getKey()) * limits.get(problem.getKey());
        }
        return penalty;
    }

    @Override
    public String toString() {
        return problems.toString();
    }
}