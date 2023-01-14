package teaselib.core;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import teaselib.Duration;
import teaselib.core.util.QualifiedName;
import teaselib.core.util.QualifiedString;
import teaselib.core.util.QualifiedStringMapping;
import teaselib.core.util.ReflectionUtils;
import teaselib.util.Daytime;
import teaselib.util.Item;

public class ItemLogger {

    static final ItemLogger None = new ItemLogger(null);
    private static final String IS = " -> ";

    private final Consumer<String> logger;

    public ItemLogger(Consumer<String> logger) {
        this.logger = logger;
    }

    public void log(String text) {
        if (logger == null) return;
        logger.accept(text);
    }

    public void log(StateImpl state, String statement, Object[] attributes, boolean value) {
        if (logger == null) return;
        if (attributes.length > 0) {
            logger.accept(name(state) + " " + statement + " " + qualifiedStrings(QualifiedStringMapping.flatten(attributes)) + IS + value);
        } else {
            logger.accept(name(state) + " " + statement);
        }
    }

    public void log(Item item, String statement, Object[] items, boolean value) {
        if (logger == null) return;
        if (items.length > 0) {
            logger.accept(item.displayName() + " " + statement + " " + qualifiedStrings(QualifiedStringMapping.flatten(items)) + IS + value);
        } else {
            logger.accept(item.displayName() + " " + statement);
        }
    }

    public void log(StateImpl state, String statement) {
        if (logger == null) return;
        logger.accept(name(state) + " " + statement);
    }

    public void log(StateImpl state, String statement, Object[] peers) {
        if (logger == null) return;
        logger.accept(name(state) + " " + statement + IS + qualifiedStrings(QualifiedStringMapping.flatten(peers)));
    }

    public void log(Item item, String name) {
        if (logger == null) return;
        logger.accept(item.displayName() + " " + name);
    }

    public void log(Item item, String name, Object[] items) {
        if (logger == null) return;
        logger.accept(item.displayName() + " " + name + IS + qualifiedStrings(QualifiedStringMapping.flatten(items)));
    }

    public void log(StateImpl state, String name, Enum<?> value) {
        if (logger == null) return;
        logger.accept(name(state) + " " + name + " " + QualifiedName.strip(ReflectionUtils.qualifiedName(value)));
    }

    public void log(String item, String name, Enum<?> value) {
        if (logger == null) return;
        logger.accept(item + " " + name + " " + QualifiedName.strip(ReflectionUtils.qualifiedName(value)));
    }

    public void log(Item item, String name, Enum<?> value) {
        if (logger == null) return;
        logger.accept(item.displayName() + " " + name + " " + QualifiedName.strip(ReflectionUtils.qualifiedName(value)));
    }

    public void log(StateImpl state, String name, boolean value) {
        if (logger == null) return;
        logger.accept(name(state) + " " + name + IS + value);
    }

    public void log(Item item, String name, boolean value) {
        if (logger == null) return;
        logger.accept(item.displayName() + " " + name + IS + value);
    }

    public void log(String item, String name, boolean value) {
        if (logger == null) return;
        logger.accept(item + " " + name + IS + value);
    }

    public void log(String item, String name, String value) {
        if (logger == null) return;
        logger.accept(item + " " + name + IS + value);
    }

    public long log(StateImpl state, String name, long value, TimeUnit unit) {
        if (logger == null) return value;
        logger.accept(name(state) + " " + name + " " + value + " " + unit);
        return value;
    }

    public long log(String item, String name, long value, TimeUnit unit) {
        if (logger == null) return value;
        logger.accept(item + " " + name + " " + value + " " + unit);
        return value;
    }

    public long log(Item item, String name, long value, TimeUnit unit) {
        if (logger == null) return value;
        logger.accept(item.displayName() + " " + name + " " + value + " " + unit);
        return value;
    }

    public void log(StateImpl state, String name, Duration duration) {
        if (logger == null) return;
        logger.accept(name(state) + " " + name + " " + duration);
    }

    public void log(String item, String name, Duration duration) {
        if (logger == null) return;
        logger.accept(item + " " + name + " " + duration);
    }

    public void log(Item item, String name, Duration duration) {
        if (logger == null) return;
        logger.accept(item.displayName() + " " + name + " " + duration);
    }

    public boolean log(List<Item> elements, String name, boolean value) {
        if (logger == null) return value;
        logger.accept(displayNames(elements) + " " + name + IS + value);
        return value;
    }

    public long log(List<Item> elements, String name, long value, TimeUnit unit) {
        if (logger == null) return value;
        logger.accept(displayNames(elements) + " " + name + IS + value + " " + unit);
        return value;
    }

    public void log(String name, ItemsImpl items) {
        if (logger == null) return;
        if (items.isEmpty()) {
            logger.accept(name + " " + displayNames(items.inventory) + IS + "<None>");
        } else {
            logger.accept(name + IS + displayNames(items.toList()));
        }
    }

    private static String displayNames(List<Item> items) {
        return items.isEmpty()
                ? "<None>"
                : items.stream().map(Item::displayName).collect(Collectors.joining(" "));
    }

    private static String qualifiedStrings(Collection<Object> items) {
        return items.isEmpty()
                ? "<None>"
                : items.stream()
                        .map(QualifiedString::of)
                        .map(QualifiedString::toString)
                        .map(QualifiedName::strip)
                        .collect(Collectors.joining(" "));
    }

    private static String name(StateImpl state) {
        return QualifiedName.strip(state.name.toString());
    }

    public void log(TimeOfDayImpl timeOfDay, String statement, Daytime dayTime, boolean value) {
        if (logger == null) return;
        logger.accept(timeOfDay + " " + statement + " " + dayTime + IS + value);
    }

    public void log(TimeOfDayImpl timeOfDay, String statement, Daytime[] dayTimes, boolean value) {
        if (logger == null) return;
        logger.accept(timeOfDay + " " + statement + " " + Arrays.toString(dayTimes) + IS + value);
    }

}
