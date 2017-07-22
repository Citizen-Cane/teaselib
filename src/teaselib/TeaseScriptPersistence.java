/**
 * 
 */
package teaselib;

import java.util.Set;
import java.util.concurrent.TimeUnit;

import teaselib.State.Persistence;
import teaselib.core.ResourceLoader;
import teaselib.core.StateMaps;
import teaselib.core.TeaseLib;
import teaselib.core.TeaseScriptBase;
import teaselib.util.Item;
import teaselib.util.Items;

/**
 * @author Citizen-Cane
 * 
 *         Implements object-oriented persistence.
 *
 */
public abstract class TeaseScriptPersistence extends TeaseScriptBase {

    protected TeaseScriptPersistence(TeaseLib teaseLib, ResourceLoader resources, Actor actor, String namespace) {
        super(teaseLib, resources, actor, namespace);
    }

    protected TeaseScriptPersistence(TeaseScriptBase script, Actor actor) {
        super(script, actor);
    }

    public class ItemProxy implements Item, StateMaps.Attributes {
        final Item item;

        public ItemProxy(Item item) {
            this.item = item;
        }

        @Override
        public boolean isAvailable() {
            return item.isAvailable();
        }

        @Override
        public void setAvailable(boolean isAvailable) {
            item.setAvailable(isAvailable);
        }

        @Override
        public String displayName() {
            return item.displayName();
        }

        @Override
        public boolean is(Object... attributes) {
            return item.is(attributes);
        }

        @Override
        public <S> Options applyTo(S... items) {
            injectNamespace();
            return new StateOptionsProxy(item.applyTo(items));
        }

        @Override
        public Options apply() {
            injectNamespace();
            return new StateOptionsProxy(item.apply());
        }

        private void injectNamespace() {
            ((StateMaps.Attributes) item).applyAttributes(namespace);
        }

        @Override
        public Persistence remove() {
            return new StatePersistenceProxy(item.remove());
        }

        @Override
        public Persistence remove(Object peer) {
            return new StatePersistenceProxy(item.remove(peer));
        }

        @Override
        public boolean canApply() {
            return item.canApply();
        }

        @Override
        public boolean applied() {
            return item.applied();
        }

        @Override
        public boolean expired() {
            return item.expired();
        }

        @Override
        public Duration duration() {
            return item.duration();
        }

        @Override
        public void applyAttributes(Object... attributes) {
            ((StateMaps.Attributes) item).applyAttributes(attributes);
        }

        @Override
        public String toString() {
            return item.toString();
        }

    }

    public class StateProxy implements State, StateMaps.Attributes {
        final State state;

        public StateProxy(State state) {
            this.state = state;
        }

        @Override
        public Options apply() {
            injectNamespace();
            return new StateOptionsProxy(state.apply());
        }

        @Override
        public <S> Options applyTo(S... items) {
            injectNamespace();
            return new StateOptionsProxy(state.applyTo(items));
        }

        private void injectNamespace() {
            ((StateMaps.Attributes) state).applyAttributes(namespace);

        }

        public Set<Object> peers() {
            return ((StateMaps.StateImpl) state).peers();
        }

        @Override
        public boolean is(Object... objects) {
            return state.is(objects);
        }

        @Override
        public boolean applied() {
            return state.applied();
        }

        @Override
        public boolean expired() {
            return state.expired();
        }

        @Override
        public Duration duration() {
            return state.duration();
        }

        @Override
        public Persistence remove() {
            return new StatePersistenceProxy(state.remove());
        }

        @Override
        public Persistence remove(Object peer) {
            return new StatePersistenceProxy(state.remove(peer));
        }

        @Override
        public void applyAttributes(Object... attributes) {
            ((StateMaps.Attributes) state).applyAttributes(attributes);
        }

        @Override
        public String toString() {
            return state.toString();
        }

    }

    public class StateOptionsProxy implements State.Options {
        final State.Options options;

        public StateOptionsProxy(State.Options options) {
            this.options = options;
        }

        @Override
        public State remember() {
            return new StateProxy(options.remember());
        }

        @Override
        public Persistence over(long duration, TimeUnit unit) {
            return new StatePersistenceProxy(options.over(duration, unit));
        }

        @Override
        public Persistence over(Duration duration) {
            return new StatePersistenceProxy(options.over(duration));
        }

        @Override
        public String toString() {
            return options.toString();
        }
    }

    public class StatePersistenceProxy implements State.Persistence {
        final State.Persistence persistence;

        public StatePersistenceProxy(Persistence persistence) {
            this.persistence = persistence;
        }

        @Override
        public State remember() {
            return new StateProxy(persistence.remember());
        }

        @Override
        public String toString() {
            return persistence.toString();
        }
    }

    private Items proxiesOf(Items items) {
        Items proxies = new Items();
        for (Item item : items) {
            if (item instanceof ItemProxy) {
                proxies.add(item);
            } else {
                proxies.add(new ItemProxy(item));
            }
        }
        return proxies;
    }

    public class Domain {
        final String domain;

        public Domain(String domain) {
            this.domain = domain;
        }

        public <T extends Enum<?>> Items items(T... values) {
            if (values.length > 0) {
                return proxiesOf(teaseLib.items(domain, values));
            } else {
                return Items.None;
            }
        }

        public Items items(String... values) {
            if (values.length > 0) {
                return proxiesOf(teaseLib.items(domain, values));
            } else {
                return Items.None;
            }
        }

        public <T extends Enum<?>> Item item(T value) {
            return new ItemProxy(teaseLib.item(domain, value));
        }

        public Item item(String value) {
            return new ItemProxy(teaseLib.item(domain, value));
        }

        public <T extends Enum<?>> Items items(T[]... values) {
            Items items = new Items();
            for (T[] s : values) {
                items.addAll(items(s));
            }
            return items;
        }

        public Items items(String[]... values) {
            Items items = new Items();
            for (String[] s : values) {
                items.addAll(items(s));
            }
            return items;
        }

        @Override
        public String toString() {
            return domain;
        }

    }

    final Domain defaultDomain = new Domain(TeaseLib.DefaultDomain);

    public Domain domain(String domain) {
        return new Domain(domain);
    }

    /**
     * Get items from a enumeration. This is different from toys and clothing in
     * that toys and clothing is usually maintained by the host, whereas
     * script-related enumerations are handled by the script.
     * 
     * @param values
     * @return A list of items whose names are based on the enumeration members
     */
    public <T extends Enum<?>> Items items(T... values) {
        return defaultDomain.items(values);
    }

    public Items items(String... values) {
        return defaultDomain.items(values);
    }

    /**
     * Get the item from an enumeration member
     * 
     * @param value
     *            The enumeration value to get the item for. The item is stored
     *            in the namespace of the script under its simple class name.
     * @return The item of the enumeration member
     */
    public <T extends Enum<?>> Item item(T value) {
        return defaultDomain.item(value);
    }

    public Item item(String value) {
        return defaultDomain.item(value);
    }

    public <T extends Enum<?>> Items items(T[]... values) {
        return defaultDomain.items(values);
    }

    public Items items(String[]... values) {
        return defaultDomain.items(values);
    }

    public TeaseLib.PersistentBoolean persistentBoolean(String name) {
        return teaseLib.new PersistentBoolean(TeaseLib.DefaultDomain, namespace, name);
    }

    public TeaseLib.PersistentBoolean persistentBoolean(Enum<?> name) {
        return teaseLib.new PersistentBoolean(TeaseLib.DefaultDomain, name);
    }

    public <T extends Enum<?>> TeaseLib.PersistentEnum<T> persistentEnum(String name, Class<T> enumClass) {
        return teaseLib.new PersistentEnum<T>(TeaseLib.DefaultDomain, namespace, name, enumClass);
    }

    public <T extends Enum<?>> TeaseLib.PersistentEnum<T> persistentEnum(Enum<?> name, Class<T> enumClass) {
        return teaseLib.new PersistentEnum<T>(TeaseLib.DefaultDomain, name, enumClass);
    }

    public <T extends Enum<?>> TeaseLib.PersistentEnum<T> persistentEnum(Class<T> enumClass) {
        return teaseLib.new PersistentEnum<T>(TeaseLib.DefaultDomain, enumClass);
    }

    public TeaseLib.PersistentInteger persistentInteger(String name) {
        return teaseLib.new PersistentInteger(TeaseLib.DefaultDomain, namespace, name);
    }

    public TeaseLib.PersistentLong persistentLong(String name) {
        return teaseLib.new PersistentLong(TeaseLib.DefaultDomain, namespace, name);
    }

    public TeaseLib.PersistentFloat persistentFloat(String name) {
        return teaseLib.new PersistentFloat(TeaseLib.DefaultDomain, namespace, name);
    }

    public TeaseLib.PersistentString persistentString(String name) {
        return teaseLib.new PersistentString(TeaseLib.DefaultDomain, namespace, name);
    }

    public <T extends Enum<T>> TeaseLib.PersistentSequence<T> persistentSequence(String name, T[] values) {
        return teaseLib.new PersistentSequence<T>(TeaseLib.DefaultDomain, namespace, name, values);
    }

    public <T extends Enum<?>> State state(T item) {
        return new StateProxy(teaseLib.state(TeaseLib.DefaultDomain, item));
    }

    public State state(String item) {
        return new StateProxy(teaseLib.state(TeaseLib.DefaultDomain, item));
    }
}
