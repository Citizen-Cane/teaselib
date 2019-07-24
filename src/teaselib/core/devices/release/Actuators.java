package teaselib.core.devices.release;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Actuators implements Iterable<Actuator> {
    public static final Actuators NONE = new Actuators(Collections.emptyList());

    final List<Actuator> elements;

    public Actuators(List<Actuator> elements) {
        this.elements = elements;
    }

    public Stream<Actuator> stream() {
        return elements.stream();
    }

    @Override
    public Iterator<Actuator> iterator() {
        return elements.iterator();
    }

    public Actuator get(int actuatorIndex) {
        return elements.get(actuatorIndex);
    }

    public int size() {
        return elements.size();
    }

    public boolean isEmpty() {
        return elements.isEmpty();
    }

    public Optional<Actuator> get(long duration, TimeUnit unit) {
        if (isEmpty()) {
            return Optional.empty();
        } else {
            return Optional.ofNullable(get(getActuatorIndex(duration, durations(unit))));
        }
    }

    public Actuators available() {
        return new Actuators(stream().filter(actuator -> !actuator.isRunning()).collect(Collectors.toList()));
    }

    private List<Long> durations(TimeUnit unit) {
        return stream().map(actuator -> actuator.available(unit)).collect(Collectors.toList());
    }

    static int getActuatorIndex(long duration, List<Long> durations) {
        long bestDifferenceSoFar = Integer.MAX_VALUE;
        int unset = Integer.MIN_VALUE;
        int bestActuator = unset;
        long maxDuration = unset;
        int maxActuator = unset;
        for (int actuator = 0; actuator < durations.size(); actuator++) {
            long availableDuration = durations.get(actuator);
            if (duration <= 0 && availableDuration < bestDifferenceSoFar) {
                bestActuator = actuator;
                bestDifferenceSoFar = availableDuration;
                maxDuration = availableDuration;
                maxActuator = actuator;
            } else {
                long difference = availableDuration - duration;
                if (0 <= difference && difference < bestDifferenceSoFar) {
                    bestActuator = actuator;
                    bestDifferenceSoFar = difference;
                }
                if (availableDuration > maxDuration) {
                    maxDuration = availableDuration;
                    maxActuator = actuator;
                }
            }
        }
        return bestActuator != unset ? bestActuator : maxActuator;
    }

    public Optional<Actuator> min() {
        return stream().reduce(Actuators::min);
    }

    public Optional<Actuator> max() {
        return stream().reduce(Actuators::max);
    }

    public static Actuator min(Actuator a, Actuator b) {
        if (a.available(TimeUnit.SECONDS) < b.available(TimeUnit.SECONDS)) {
            return a;
        } else {
            return b;
        }
    }

    public static Actuator max(Actuator a, Actuator b) {
        if (a.available(TimeUnit.SECONDS) > b.available(TimeUnit.SECONDS)) {
            return a;
        } else {
            return b;
        }
    }

    @Override
    public String toString() {
        return elements.toString();
    }

}
