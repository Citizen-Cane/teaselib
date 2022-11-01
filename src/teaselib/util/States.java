package teaselib.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collector;

import teaselib.State;

public class States extends ArrayList<State> {

    public interface Query extends Supplier<States> {
        @Override
        States get();

        void forEach(Consumer<? super State> function);
    }

    public static class QueryImpl implements Query {
        private final Supplier<States> states;

        public QueryImpl(Supplier<States> states) {
            this.states = states;
        }

        @Override
        public States get() {
            return states.get();
        }

        @Override
        public void forEach(Consumer<? super State> function) {
            states.get().forEach(function);
        }
    }

    public static Collector<State, States, States> toStates() {
        return Collector.of(States::new, //
                (states, state) -> states.add(state), //
                (states1, states2) -> {
                    states1.addAll(states2);
                    return states1;
                }, Collector.Characteristics.UNORDERED);
    }

    private static final long serialVersionUID = 1L;

    public States() {
        super();
    }

    public States(State element) {
        super(Collections.singletonList(element));
    }

    public States(State... elements) {
        super(Arrays.asList(elements));
    }

    public States(List<State> elements) {
        super(elements);
    }

    public States applied() {
        return stream().filter(State::applied).collect(States.toStates());
    }

    public States expired() {
        return stream().filter(State::expired).collect(States.toStates());
    }

    public boolean allApplied() {
        return stream().allMatch(State::applied);
    }

    public boolean anyApplied() {
        return stream().anyMatch(State::applied);
    }

    public boolean noneApplied() {
        return stream().noneMatch(State::applied);
    }

    public boolean allRemoved() {
        return stream().allMatch(State::removed);
    }

    public boolean anyRemoved() {
        return stream().anyMatch(State::removed);
    }

    public boolean noneRemoved() {
        return stream().noneMatch(State::removed);
    }

    public boolean allExpired() {
        return stream().allMatch(State::expired);
    }

    public boolean anyExpired() {
        return stream().anyMatch(State::expired);
    }

    public boolean noneExpired() {
        return stream().noneMatch(State::expired);
    }

    public boolean anyNotExpired() {
        return !allExpired();
    }

    public boolean none() {
        return isEmpty();
    }

    public boolean any() {
        return !isEmpty();
    }

}
