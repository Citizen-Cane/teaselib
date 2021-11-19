package teaselib.core.util;

import static teaselib.core.state.AbstractProxy.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.UnaryOperator;

import teaselib.core.StateImpl;
import teaselib.core.state.AbstractProxy;
import teaselib.core.state.ItemProxy;
import teaselib.core.state.StateProxy;
import teaselib.util.ItemImpl;
import teaselib.util.Items;

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
            throw new UnsupportedOperationException(object.toString());
        }
    }

    public static Set<QualifiedString> map(UnaryOperator<Collection<Object>> precondition, Object... peers) {
        return map(precondition.apply(removeProxies(flatten(Arrays.asList(peers)))));
    }

    public static Collection<Object> flatten(Collection<? extends Object> peers) {
        List<Object> flattenedPeers = new ArrayList<>(peers.size());
        for (Object peer : peers) {
            if (peer instanceof Items) {
                var items = (Items) peer;
                flattenedPeers.addAll(items.firstOfEachKind());
            } else if (peer instanceof Collection) {
                var collection = (Collection<?>) peer;
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

    private static Set<QualifiedString> map(Collection<Object> values) {
        Set<QualifiedString> mapped = new HashSet<>(values.size());
        for (Object value : values) {
            mapped.add(QualifiedString.of(value));
        }
        return mapped;
    }

}
