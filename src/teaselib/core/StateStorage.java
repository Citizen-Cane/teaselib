package teaselib.core;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import teaselib.Duration;
import teaselib.core.TeaseLib.PersistentBoolean;
import teaselib.core.TeaseLib.PersistentString;
import teaselib.core.util.Persist;
import teaselib.core.util.PersistedObject;
import teaselib.core.util.QualifiedString;

class StateStorage {
    private static final Logger logger = LoggerFactory.getLogger(StateStorage.class);

    final StateImpl state;

    final PersistentBoolean appliedStorage;
    final PersistentString durationStorage;
    final PersistentString peerStorage;
    final PersistentString attributeStorage;

    StateStorage(StateImpl state, String domain, String name) {
        this.state = state;
        this.appliedStorage = persistentApplied(domain, name);
        this.durationStorage = persistentDuration(domain, name);
        this.peerStorage = persistentPeers(domain, name);
        this.attributeStorage = persistentAttributes(domain, name);
    }

    boolean persisted() {
        return appliedStorage.available();
    }

    boolean restoreApplied() {
        return appliedStorage.value();
    }

    Duration restoreDuration() {
        if (persisted()) {
            // TODO Refactor persistence to duration class
            String[] argv = durationStorage.value().split(" ");
            long start = Long.parseLong(argv[0]);
            long limit = argv.length > 1 ? string2timespan(argv[1]) : 0;
            if (argv.length == 1) {
                return new DurationImpl(state.cache.teaseLib, start, 0, TimeUnit.SECONDS);
            } else if (argv.length == 2) {
                return new DurationImpl(state.cache.teaseLib, start, limit, TimeUnit.SECONDS);
            } else if (argv.length == 3) {
                long elapsed = string2timespan(argv[2]);
                return new FrozenDuration(state.cache.teaseLib, start, limit, elapsed, TimeUnit.SECONDS);
            } else {
                throw new IllegalStateException(durationStorage.value());
            }
        } else {
            return new FrozenDuration(state.cache.teaseLib, 0, 0, TimeUnit.SECONDS);
        }
    }

    private static long string2timespan(String limitString) {
        long limit;
        if (limitString.equals(StateImpl.FOREVER_KEYWORD)) {
            limit = Duration.INFINITE;
        } else {
            limit = Long.parseLong(limitString);
        }
        return limit;
    }

    Set<QualifiedString> restorePeers() {
        Set<QualifiedString> peers = new HashSet<>();
        if (peerStorage.available()) {
            String persisted = peerStorage.value();
            List<String> presistedPeers = new PersistedObject(ArrayList.class, persisted).toValues();
            for (String persistedPeer : presistedPeers) {
                restorePersistedPeer(peers, persistedPeer);
            }
        }
        return peers;
    }

    private void restorePersistedPeer(Set<QualifiedString> peers, String persisted) {
        try {
            QualifiedString peer = Persist.from(persisted);
            addPeerThatHasBeenPersistedWithMe(peers, peer);
        } catch (NoSuchElementException e) {
            logger.warn("Item {} does not exist anymore: {}", persisted, e.getMessage());
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Peer " + persisted + " not restored: " + e.getMessage(), e);
        }
    }

    private void addPeerThatHasBeenPersistedWithMe(Set<QualifiedString> peers, QualifiedString peer) {
        if (persistentDuration(state.domain, peer.toString()).available()) {
            peers.add(peer);
        } else if (peer.isItem()) {
            peers.add(peer);
        } else if (isCached(peer) && state.state(peer).applied()) {
            peers.add(peer);
        } else if (!state.state(peer).storage.persisted()) {
            logger.warn("Ignoring temporary peer {}", peer);
        } else {
            throw new IllegalStateException("Unexpected peer " + peer);
        }
    }

    private boolean isCached(QualifiedString peer) {
        return state.cache.stateMap(state.domain, peer).contains(peer);
    }

    Set<QualifiedString> restoreAttributes() {
        if (attributeStorage.available()) {
            Set<QualifiedString> attributes = new HashSet<>();
            try {
                attributes.addAll(Persist.from(ArrayList.class, attributeStorage.value()));
            } catch (ReflectiveOperationException e) {
                throw new IllegalArgumentException("Cannot restore attributes for state " + state.name + ": ", e);
            }
            return attributes;
        } else {
            return new HashSet<>();
        }
    }

    void persistApplied(boolean applied) {
        appliedStorage.set(applied);
    }

    void persistDuration(Duration duration) {
        var start = Long.toString(duration.start(TimeUnit.SECONDS));
        long limitValue = duration.limit(TimeUnit.SECONDS);
        var limit = timespan2String(limitValue);
        if (duration instanceof FrozenDuration) {
            var elapsed = timespan2String(duration.elapsed(TimeUnit.SECONDS));
            durationStorage.set(start + " " + limit + " " + elapsed);
        } else {
            if (limitValue != 0) {
                durationStorage.set(start + " " + limit);
            } else {
                durationStorage.set(start);
            }
        }
    }

    private static String timespan2String(long timespan) {
        String timespanString;
        if (timespan < 0) {
            timespanString = Long.toString(0);
        } else if (timespan == Duration.INFINITE) {
            timespanString = StateImpl.FOREVER_KEYWORD;
        } else {
            timespanString = Long.toString(timespan);
        }
        return timespanString;
    }

    void persistPeers(Set<QualifiedString> peers) {
        if (peers.isEmpty()) {
            peerStorage.clear();
        } else {
            peerStorage.set(Persist.persistValues(peers));
        }
    }

    void persistAttributes(Set<QualifiedString> attributes) {
        if (attributes.isEmpty()) {
            attributeStorage.clear();
        } else {
            attributeStorage.set(Persist.persistValues(attributes));
        }
    }

    TeaseLib.PersistentBoolean persistentApplied(String domain, String name) {
        return persistentBoolean(domain, name, "applied");
    }

    TeaseLib.PersistentString persistentDuration(String domain, String name) {
        return persistentString(domain, name, "duration");
    }

    PersistentString persistentPeers(String domain, String name) {
        return persistentString(domain, name, "peers");
    }

    PersistentString persistentAttributes(String domain, String name) {
        return persistentString(domain, name, "attributes");
    }

    private PersistentBoolean persistentBoolean(String domain, String name, String property) {
        return state.cache.teaseLib.new PersistentBoolean(domain, name, "state." + property);
    }

    private PersistentString persistentString(String domain, String name, String property) {
        return state.cache.teaseLib.new PersistentString(domain, name, "state." + property);
    }

    void updatePersistence() {
        persistApplied(state.applied);
        persistDuration(state.duration);
        persistPeers(state.peers);
        persistAttributes(state.attributes);
    }

    void deletePersistence() {
        appliedStorage.clear();
        durationStorage.clear();
        peerStorage.clear();
        attributeStorage.clear();
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((appliedStorage == null) ? 0 : appliedStorage.hashCode());
        result = prime * result + ((attributeStorage == null) ? 0 : attributeStorage.hashCode());
        result = prime * result + ((durationStorage == null) ? 0 : durationStorage.hashCode());
        result = prime * result + ((peerStorage == null) ? 0 : peerStorage.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        StateStorage other = (StateStorage) obj;
        if (appliedStorage == null) {
            if (other.appliedStorage != null)
                return false;
        } else if (!appliedStorage.equals(other.appliedStorage))
            return false;
        if (attributeStorage == null) {
            if (other.attributeStorage != null)
                return false;
        } else if (!attributeStorage.equals(other.attributeStorage))
            return false;
        if (durationStorage == null) {
            if (other.durationStorage != null)
                return false;
        } else if (!durationStorage.equals(other.durationStorage))
            return false;
        if (peerStorage == null) {
            if (other.peerStorage != null)
                return false;
        } else if (!peerStorage.equals(other.peerStorage))
            return false;
        return true;
    }
}