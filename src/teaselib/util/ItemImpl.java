package teaselib.util;

import static java.util.Collections.*;
import static teaselib.core.util.QualifiedStringMapping.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import teaselib.Duration;
import teaselib.State;
import teaselib.core.StateImpl;
import teaselib.core.StateImpl.Precondition;
import teaselib.core.TeaseLib;
import teaselib.core.state.AbstractProxy;
import teaselib.core.util.Persist;
import teaselib.core.util.Persist.Persistable;
import teaselib.core.util.QualifiedString;
import teaselib.core.util.ReflectionUtils;

/**
 * @author Citizen-Cane
 *
 */
public class ItemImpl implements Item, State.Options, State.Attributes, Persistable {

    public static final String Available = "Available";

    final TeaseLib teaseLib;

    public final String domain;
    public final QualifiedString name;
    public final String displayName;
    private final TeaseLib.PersistentBoolean available;
    public final Set<QualifiedString> defaultPeers;
    public final Set<QualifiedString> attributes;

    public static String createDisplayName(QualifiedString item) {
        return item.guid().orElseThrow().replace("_", " ");
    }

    public ItemImpl(TeaseLib teaseLib, String domain, QualifiedString guid, String displayName) {
        this(teaseLib, domain, guid, displayName, new Object[] {}, new Object[] {});
    }

    public ItemImpl(TeaseLib teaseLib, String domain, QualifiedString name, String displayName, Object[] defaultPeers,
            Object[] attributes) {
        this.teaseLib = teaseLib;
        this.domain = domain;
        this.name = name;
        this.displayName = displayName;
        this.available = teaseLib.new PersistentBoolean(domain, kind().toString(),
                name.guid().orElseThrow() + "." + Available);
        this.defaultPeers = unmodifiableSet(map(Precondition::apply, defaultPeers));
        this.attributes = unmodifiableSet(attributes(map(Precondition::apply, attributes)));
    }

    public QualifiedString kind() {
        return name.kind();
    }

    private StateImpl state() {
        return state(kind());
    }

    @Override
    public List<String> persisted() {
        return Arrays.asList(Persist.persist(name.toString()));
    }

    private Set<QualifiedString> attributes(Collection<QualifiedString> attributes) {
        Set<QualifiedString> all = new HashSet<>();
        all.add(name.kind());
        all.addAll(attributes);
        return all;
    }

    @Override
    public boolean isAvailable() {
        return available.value();
    }

    @Override
    public void setAvailable(boolean isAvailable) {
        available.set(isAvailable);
    }

    @Override
    public String displayName() {
        return displayName;
    }

    @Override
    public String toString() {
        return name.guid().orElseThrow() + " " + attributes + " " + teaseLib.state(domain, kind());
    }

    @Override
    public boolean is(Object... attributes3) {
        return isImpl(map(Precondition::is, attributes3));
    }

    private boolean isImpl(Set<QualifiedString> flattenedAttributes) {
        if (flattenedAttributes.isEmpty()) {
            return false;
        }

        StateImpl state = state();
        boolean isMyState = state(name.kind()).is(name);
        for (QualifiedString element : flattenedAttributes) {
            if (!isImpl(element, state, isMyState)) {
                return false;
            }
        }

        return true;
    }

    private boolean isImpl(QualifiedString element, StateImpl state, boolean isMyState) {
        if (this.attributes.stream().anyMatch(element::is)) { // Item attributes
            return true;
        } else if (element.is(this.name)) {
            return true;
        } else if (element.name().equals(QualifiedString.ANY) && // Item class attributes
                (StateImpl.haveClass(attributes, element))) {
            return true;
        } else {
            if (isMyState) { // Item identity
                if (state.isImpl(state.getAttributes(), element)) { // State attributes
                    return applied();
                } else if (state.isImpl(state.peers(), element)) { // State peers
                    return applied();
                }
            }
        }
        return false;
    }

    @Override
    public boolean canApply() {
        if (defaultPeers.isEmpty()) {
            return !state().is(this.name);
        } else {
            return defaultStates().allMatch(state -> !state.applied());
        }
    }

    @Override
    public boolean applied() {
        StateImpl state = state();
        if (state.applied()) {
            if (defaultPeers.isEmpty()) {
                return containsMyGuid(state);
            } else {
                return defaultStates().anyMatch(this::containsMyGuid);
            }
        } else {
            return false;
        }
    }

    @Override
    public boolean expired() {
        return state().expired();
    }

    @Override
    public Duration duration() {
        return state().duration();
    }

    @Override
    public State.Options apply() {
        StateImpl state = state();

        if (defaultPeers.isEmpty()) {
            state.apply();
        } else {
            applyInstanceTo(defaultPeers);
        }

        state.applyAttributes(this.attributes);
        state.applyTo(this.name);

        updateLastUsedGuidState();
        return this;
    }

    @Override
    public State.Options applyTo(Object... peers) {
        if (peers.length == 0 && defaultPeers.isEmpty()) {
            throw new IllegalArgumentException("Item without default peers must be applied with explicit peer list");
        }

        return applyToImpl(map(Precondition::apply, peers));
    }

    private State.Options applyToImpl(Set<QualifiedString> flattenedPeers) {
        applyInstanceTo(defaultPeers);
        applyInstanceTo(flattenedPeers);

        StateImpl state = state();
        state.applyAttributes(this.attributes);
        state.applyImpl(Collections.singleton(this.name));
        state.applyImpl(flattenedPeers);
        updateLastUsedGuidState();
        return this;
    }

    private void applyInstanceTo(Collection<QualifiedString> items) {
        Set<QualifiedString> me = new HashSet<>(2);
        me.add(this.name);
        me.add(this.name.kind());
        for (QualifiedString peer : items) {
            if (peer.isItem()) {
                state(peer.kind()).applyImpl(me);
            } else {
                state(peer).applyImpl(me);
            }
        }
    }

    @Override
    public Persistence over(long duration, TimeUnit unit) {
        StateImpl state = state();
        state.over(duration, unit);
        return this;
    }

    @Override
    public Persistence over(Duration duration) {
        StateImpl state = state();
        state.over(duration);
        return this;
    }

    @Override
    public void remember(Until forget) {
        StateImpl state = state();
        state.remember(forget);
        state(QualifiedString.of(forget)).applyTo(this.name).remember(forget);
    }

    @Override
    public void remove() {
        StateImpl state = state();
        if (containsMyGuid(state)) {
            for (QualifiedString peer : new ArrayList<>(state.peers())) {
                if (!peer.isItem()) {
                    StateImpl peerState = state(peer);
                    peerState.removeFrom(this.name);
                    state.removeFrom(this.name);
                }
            }

            // TODO better: remove only peers of this instances that are not peered by any other item

            releaseInstanceGuid();
            if (state.peers().isEmpty()) {
                state.remove();
            }
            updateLastUsedGuidState(duration());
        } else {
            throw new IllegalStateException("This item is not applied: " + name);
        }
    }

    @Override
    public void removeFrom(Object... peers) {
        removeInternal(map(Precondition::remove, peers));
    }

    private void removeInternal(Set<QualifiedString> peers) {
        StateImpl state = state();
        if (containsMyGuid(state)) {
            for (QualifiedString peer : peers) {
                if (!peer.isItem()) {
                    StateImpl peerState = state(peer);
                    peerState.removeFrom(this.name);
                }
            }

            releaseInstanceGuid();
            if (state.peers().isEmpty()) {
                state.remove();
            }
            updateLastUsedGuidState(duration());
        } else {
            throw new IllegalStateException("This item is not applied: " + name);
        }
    }

    public boolean releaseInstanceGuid() {
        StateImpl state = state();
        if (state.peerStates().stream().anyMatch(this::containsMyGuid)) {
            return false;
        }
        state.removeFrom(this.name);
        return true;
    }

    private void updateLastUsedGuidState() {
        lastUsed().updateLastUsed();
    }

    private void updateLastUsedGuidState(Duration duration) {
        lastUsed().updateLastUsed(duration);
    }

    private StateImpl lastUsed() {
        var lastUsedStateName = ReflectionUtils.qualified(name.kind().toString(), name.guid().orElseThrow());
        return (StateImpl) teaseLib.state(domain, lastUsedStateName);
    }

    private Stream<StateImpl> defaultStates() {
        return defaultPeers.stream().map(this::state);
    }

    private StateImpl state(QualifiedString peer) {
        return (StateImpl) teaseLib.state(domain, peer);
    }

    private boolean containsMyGuid(StateImpl state) {
        return state.peers().contains(this.name);
    }

    @Override
    public void applyAttributes(Object... attributes) {
        state().applyAttributes(attributes);
    }

    @Override
    public boolean removed() {
        return !applied();
    }

    @Override
    public long removed(TimeUnit unit) {
        if (applied()) {
            return 0;
        } else {
            return lastUsed().removed(unit);
        }
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((attributes == null) ? 0 : attributes.hashCode());
        result = prime * result + ((displayName == null) ? 0 : displayName.hashCode());
        result = prime * result + ((domain == null) ? 0 : domain.hashCode());
        result = prime * result + ((name == null) ? 0 : name.hashCode());
        result = prime * result + defaultPeers.hashCode();
        result = prime * result + ((teaseLib == null) ? 0 : teaseLib.hashCode());
        result = prime * result + ((available == null) ? 0 : available.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof AbstractProxy<?>) {
            return equals(((AbstractProxy<?>) obj).state);
        } else {
            return generatedEqualsMethod(obj);
        }
    }

    private boolean generatedEqualsMethod(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        ItemImpl other = (ItemImpl) obj;
        if (attributes == null) {
            if (other.attributes != null)
                return false;
        } else if (!attributes.equals(other.attributes))
            return false;
        if (displayName == null) {
            if (other.displayName != null)
                return false;
        } else if (!displayName.equals(other.displayName))
            return false;
        if (domain == null) {
            if (other.domain != null)
                return false;
        } else if (!domain.equals(other.domain))
            return false;
        if (name == null) {
            if (other.name != null)
                return false;
        } else if (!name.equals(other.name))
            return false;
        if (!defaultPeers.equals(other.defaultPeers))
            return false;
        if (teaseLib == null) {
            if (other.teaseLib != null)
                return false;
        } else if (!teaseLib.equals(other.teaseLib))
            return false;
        if (available == null) {
            if (other.available != null)
                return false;
        } else if (!available.equals(other.available))
            return false;
        return true;
    }
}
