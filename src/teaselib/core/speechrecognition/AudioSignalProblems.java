package teaselib.core.speechrecognition;

import java.util.EnumMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import teaselib.core.speechrecognition.SpeechRecognition.AudioSignalProblem;

public class AudioSignalProblems {
    Map<SpeechRecognition.AudioSignalProblem, AtomicInteger> problems = new EnumMap<>(AudioSignalProblem.class);

    public AudioSignalProblems() {
        clear();
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
        for (AtomicInteger value : problems.values()) {
            if (value.get() > 0) {
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