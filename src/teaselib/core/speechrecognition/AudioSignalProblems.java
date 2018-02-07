package teaselib.core.speechrecognition;

import java.util.EnumMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicInteger;

import teaselib.core.speechrecognition.SpeechRecognition.AudioSignalProblem;

public class AudioSignalProblems {
    private final Map<SpeechRecognition.AudioSignalProblem, AtomicInteger> problems = new EnumMap<>(
            AudioSignalProblem.class);
    private final Map<SpeechRecognition.AudioSignalProblem, Integer> limits = new EnumMap<>(AudioSignalProblem.class);

    public AudioSignalProblems() {
        clear();
        setLimits();
    }

    private void setLimits() {
        limits.put(AudioSignalProblem.None, Integer.MAX_VALUE);
        limits.put(AudioSignalProblem.Noise, 0);
        limits.put(AudioSignalProblem.NoSignal, 0);
        limits.put(AudioSignalProblem.TooLoud, 0);
        limits.put(AudioSignalProblem.TooQuiet, 1);
        limits.put(AudioSignalProblem.TooFast, 0);
        limits.put(AudioSignalProblem.TooSlow, 0);
    }

    public void clear() {
        problems.clear();
        for (AudioSignalProblem audioSignalProblem : AudioSignalProblem.values()) {
            problems.put(audioSignalProblem, new AtomicInteger(0));
        }
    }

    public void add(AudioSignalProblem audioSignalProblem) {
        problems.get(audioSignalProblem).incrementAndGet();
    }

    public boolean occured() {
        for (Entry<AudioSignalProblem, AtomicInteger> problem : problems.entrySet()) {
            if (problem.getValue().get() > limits.get(problem.getKey())) {
                return true;
            }
        }
        return false;
    }

    @Override
    public String toString() {
        return problems.toString();
    }
}