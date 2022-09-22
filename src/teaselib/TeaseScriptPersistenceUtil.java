package teaselib;

import teaselib.core.TeaseLib;

/**
 * @author Citizen-Cane
 *
 *         Implements non-object oriented persistence helpers.
 */

public class TeaseScriptPersistenceUtil {

    private final TeaseLib teaseLib;
    private final String namespace;

    protected TeaseScriptPersistenceUtil(TeaseLib teaseLib, String namespace) {
        this.teaseLib = teaseLib;
        this.namespace = namespace;
    }

    public void clear(String name) {
        teaseLib.clear(TeaseLib.DefaultDomain, namespace, name);
    }

    public void clear(Enum<?> name) {
        teaseLib.clear(TeaseLib.DefaultDomain, name);
    }

    public void set(Enum<?> name, boolean value) {
        teaseLib.set(TeaseLib.DefaultDomain, name, value);
    }

    public void set(Enum<?> name, int value) {
        teaseLib.set(TeaseLib.DefaultDomain, name, value);
    }

    public void set(Enum<?> name, long value) {
        teaseLib.set(TeaseLib.DefaultDomain, name, value);
    }

    public void set(Enum<?> name, double value) {
        teaseLib.set(TeaseLib.DefaultDomain, name, value);
    }

    public void set(Enum<?> name, String value) {
        teaseLib.set(TeaseLib.DefaultDomain, name, value);
    }

    public void set(String name, boolean value) {
        teaseLib.set(TeaseLib.DefaultDomain, namespace, name, value);
    }

    public void set(String name, int value) {
        teaseLib.set(TeaseLib.DefaultDomain, namespace, name, value);
    }

    public void set(String name, long value) {
        teaseLib.set(TeaseLib.DefaultDomain, namespace, name, value);
    }

    public void set(String name, double value) {
        teaseLib.set(TeaseLib.DefaultDomain, namespace, name, value);
    }

    public void set(String name, String value) {
        teaseLib.set(TeaseLib.DefaultDomain, namespace, name, value);
    }

    public boolean getBoolean(String name) {
        return teaseLib.getBoolean(TeaseLib.DefaultDomain, namespace, name);
    }

    public int getInteger(String name) {
        return teaseLib.getInteger(TeaseLib.DefaultDomain, namespace, name);
    }

    public long getLong(String name) {
        return teaseLib.getInteger(TeaseLib.DefaultDomain, namespace, name);
    }

    public double getFloat(String name) {
        return teaseLib.getFloat(TeaseLib.DefaultDomain, namespace, name);
    }

    public String getString(String name) {
        return teaseLib.getString(TeaseLib.DefaultDomain, namespace, name);
    }

    public boolean getBoolean(Enum<?> name) {
        return teaseLib.getBoolean(TeaseLib.DefaultDomain, name);
    }

    public int getInteger(Enum<?> name) {
        return teaseLib.getInteger(TeaseLib.DefaultDomain, name);
    }

    public long getLong(Enum<?> name) {
        return teaseLib.getLong(TeaseLib.DefaultDomain, name);
    }

    public double getFloat(Enum<?> name) {
        return teaseLib.getFloat(TeaseLib.DefaultDomain, name);
    }

    public String getString(Enum<?> name) {
        return teaseLib.getString(TeaseLib.DefaultDomain, name);
    }

    public TeaseLib.PersistentBoolean newBoolean(String name) {
        return teaseLib.new PersistentBoolean(TeaseLib.DefaultDomain, namespace, name);
    }

    public TeaseLib.PersistentBoolean newBoolean(Enum<?> name) {
        return teaseLib.new PersistentBoolean(TeaseLib.DefaultDomain, name);
    }

    public <T extends Enum<?>> TeaseLib.PersistentEnum<T> newEnum(String name, Class<T> enumClass) {
        return teaseLib.new PersistentEnum<>(TeaseLib.DefaultDomain, namespace, name, enumClass);
    }

    public <T extends Enum<?>> TeaseLib.PersistentEnum<T> newEnum(Enum<?> name, Class<T> enumClass) {
        return teaseLib.new PersistentEnum<>(TeaseLib.DefaultDomain, name, enumClass);
    }

    public <T extends Enum<?>> TeaseLib.PersistentEnum<T> newEnum(Class<T> enumClass) {
        return teaseLib.new PersistentEnum<>(TeaseLib.DefaultDomain, enumClass);
    }

    public TeaseLib.PersistentInteger newInteger(String name) {
        return teaseLib.new PersistentInteger(TeaseLib.DefaultDomain, namespace, name);
    }

    public TeaseLib.PersistentLong newLong(String name) {
        return teaseLib.new PersistentLong(TeaseLib.DefaultDomain, namespace, name);
    }

    public TeaseLib.PersistentFloat newFloat(String name) {
        return teaseLib.new PersistentFloat(TeaseLib.DefaultDomain, namespace, name);
    }

    public TeaseLib.PersistentString newString(String name) {
        return teaseLib.new PersistentString(TeaseLib.DefaultDomain, namespace, name);
    }

    public <T extends Enum<T>> TeaseLib.PersistentSequence<T> newSequence(String name, T[] values) {
        return teaseLib.new PersistentSequence<>(TeaseLib.DefaultDomain, namespace, name, values);
    }

}
