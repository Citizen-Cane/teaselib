package teaselib.core;

import java.util.Arrays;
import java.util.List;
import java.util.function.Predicate;

public class ApplyRules {

    public record ApplyRule(String name, Predicate<ItemImpl> predicate) {
    }

    private static final ApplyRule isAvailable = new ApplyRule("available", item -> item.isAvailable());

    private static final ApplyRule notAlreadyApplied = new ApplyRule("not already fully applied", item -> {
        if (item.defaultPeers.isEmpty()) {
            return !item.applied();
        } else {
            return !item.defaultStates().allMatch(state -> state.applied());
        }
    });

    private static final ApplyRule canApplyToPeers = new ApplyRule("canApply", item -> {
        return item.defaultStates().allMatch(state -> {
            if (state.applied()) {
                return state.is(item);
            } else {
                return true;
            }
        });
    }

    );

    private static final ApplyRule blockedPeers = new ApplyRule("blocked",
            item -> item.blockers.isEmpty() || item.blockers.stream()
                    .map(name -> item.teaseLib.state(item.domain, name)).noneMatch(state -> state.applied()));

    public static final List<ApplyRule> All = Arrays.asList(//
            isAvailable, //
            notAlreadyApplied, //
            canApplyToPeers, //
            blockedPeers//
    );

    final List<ApplyRule> rules;

    public ApplyRules(List<ApplyRule> rules) {
        this.rules = rules;
    }

    public boolean test(ItemImpl item) {
        return rules.stream().allMatch(rule -> rule.predicate.test(item));
    }

    public ApplyRule log(ItemImpl item) {
        return rules.stream().filter(rule -> rule.predicate.test(item)).findFirst().orElse(null);
    }

}
