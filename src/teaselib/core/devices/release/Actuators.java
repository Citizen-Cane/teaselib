package teaselib.core.devices.release;

import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Actuators implements Iterable<Actuator> {
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

    public Actuator get(long duration, TimeUnit unit) {
        List<Long> durations = stream().map(actuator -> actuator.available(unit)).collect(Collectors.toList());
        return get(getActuatorIndex(duration, durations));
    }

    static int getActuatorIndex(long duration, List<Long> durations) {
        long bestDifferenceSoFar = Integer.MAX_VALUE;
        int unset = Integer.MIN_VALUE;
        int bestActuator = unset;
        long maxDuration = unset;
        int maxActuator = unset;
        for (int actuator = 0; actuator < durations.size(); actuator++) {
            long availableDuration = durations.get(actuator);
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
        return bestActuator != unset ? bestActuator : maxActuator;
    }

    // TODO instead of min/max provide interface that can deal with more than two actuators and a duration

    public Actuator min() {
        Optional<Actuator> actuator = stream().reduce(Actuators::min);
        if (actuator.isPresent())
            return actuator.get();
        else
            throw new IllegalStateException("Empty list");
    }

    public Actuator max() {
        Optional<Actuator> actuator = stream().reduce(Actuators::max);
        if (actuator.isPresent())
            return actuator.get();
        else
            throw new IllegalStateException("Empty list");
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
}
