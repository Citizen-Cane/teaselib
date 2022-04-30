package teaselib.core;

import java.util.Arrays;
import java.util.List;
import java.util.function.Predicate;

public class ApplyRules {

    private static final Predicate<ItemImpl> isAvailable = item -> item.isAvailable();

    private static final Predicate<ItemImpl> notAlreadyApplied = item -> !item.state().is(item.name);

    private static final Predicate<ItemImpl> canApplyToPeers = item -> item.defaultPeers.isEmpty()
            || item.defaultStates().noneMatch(state -> state.applied());

    private static final Predicate<ItemImpl> blockedPeers = item -> item.blockers.isEmpty() || item.blockers.stream()
            .map(name -> item.teaseLib.state(item.domain, name)).noneMatch(state -> state.applied());

    public static final List<Predicate<ItemImpl>> All = Arrays.asList(isAvailable, notAlreadyApplied, canApplyToPeers,
            blockedPeers);

    final List<Predicate<ItemImpl>> rules;

    public ApplyRules(List<Predicate<ItemImpl>> rules) {
        this.rules = rules;
    }

    public boolean test(ItemImpl item) {
        return rules.stream().allMatch(rule -> rule.test(item));
    }

}
