package teaselib.core.util;

import static teaselib.core.state.AbstractProxy.removeProxies;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import teaselib.core.ItemImpl;
import teaselib.core.ItemsImpl;
import teaselib.core.StateImpl;
import teaselib.core.state.AbstractProxy;
import teaselib.core.state.ItemProxy;
import teaselib.core.state.StateProxy;

/**
 * @author Citizen-Cane
 *
 */
public class QualifiedStringMapping {

    public static QualifiedString of(Object object) {
        if (object instanceof String) {
            return new QualifiedString((String) object);
        } else if (object instanceof Enum<?>) {
            Enum<?> item = (Enum<?>) object;
            return new QualifiedString(ReflectionUtils.qualifiedName(item));
        } else if (object instanceof QualifiedString) {
            return (QualifiedString) object;
        } else if (object instanceof Class<?>) {
            return QualifiedString.of((Class<?>) object);
        } else if (object instanceof StateProxy) {
            StateProxy state = (StateProxy) object;
            return of(AbstractProxy.removeProxy(state));
        } else if (object instanceof StateImpl) {
            StateImpl state = (StateImpl) object;
            return state.name;
        } else if (object instanceof ItemProxy) {
            ItemProxy item = (ItemProxy) object;
            return of(AbstractProxy.removeProxy(item));
        } else if (object instanceof ItemImpl) {
            ItemImpl item = (ItemImpl) object;
            return item.name;
        } else {
            throw new UnsupportedOperationException(object.getClass().getSimpleName() + " " + object);
        }
    }

    public static Set<QualifiedString> map(UnaryOperator<Collection<Object>> precondition, Object... peers) {
        return map(precondition, QualifiedStringMapping::identityMapping, peers);
    }

    public static Set<QualifiedString> map(UnaryOperator<Collection<Object>> precondition,
            Function<Collection<Object>, Set<QualifiedString>> mapper, Object... peers) {
        return mapper.apply(precondition.apply(removeProxies(flatten(peers))));
    }

    public static Collection<Object> flatten(Object... peers) {
        List<Object> flattenedPeers = new ArrayList<>(peers.length);
        for (Object peer : peers) {
            if (peer instanceof ItemsImpl items) {
                flattenedPeers.addAll(items.oneOfEachKind());
            } else if (peer instanceof Collection<?> collection) {
                flattenedPeers.addAll(collection);
            } else if (peer instanceof Object[]) {
                var list = Arrays.asList(peer);
                flattenedPeers.addAll(list);
            } else {
                flattenedPeers.add(peer);
            }
        }
        return flattenedPeers;
    }

    @SafeVarargs
    public static <T extends Object> Set<QualifiedString> of(T[]... values) {
        return Stream.of(values).flatMap(Stream::of).map(QualifiedString::of).collect(Collectors.toSet());
    }

    @SafeVarargs
    public static <T extends Object> Set<QualifiedString> of(T... values) {
        return Stream.of(values).map(QualifiedString::of).collect(Collectors.toSet());
    }

    private static Set<QualifiedString> identityMapping(Collection<Object> values) {
        Set<QualifiedString> mapped = new HashSet<>(values.size());
        for (Object value : values) {
            mapped.add(QualifiedString.of(value));
        }
        return mapped;
    }

    public static Set<QualifiedString> reduceItemGuidsToStates(Collection<Object> values) {
        Set<QualifiedString> mapped = new HashSet<>(values.size());
        for (Object value : values) {
            if (value instanceof ItemImpl itemImpl) {
                mapped.add(QualifiedString.of(itemImpl.name.kind()));
            } else {
                mapped.add(QualifiedString.of(value));
            }
        }
        return mapped;
    }

}
