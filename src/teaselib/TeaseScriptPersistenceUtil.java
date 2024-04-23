package teaselib;

import teaselib.core.TeaseLib;
import teaselib.core.util.QualifiedName;

/**
 * @author Citizen-Cane
 *
 *         Implements non-object oriented persistence helpers.
 */

public class TeaseScriptPersistenceUtil {

    private final TeaseLib teaseLib;

    private final String domain;
    private final String namespace;

    protected TeaseScriptPersistenceUtil(TeaseLib teaseLib, String namespace) {
        this(teaseLib, TeaseLib.DefaultDomain, namespace);
    }

    public TeaseScriptPersistenceUtil(TeaseLib teaseLib, String domain, String namespace) {
        this.teaseLib = teaseLib;
        this.domain = domain;
        this.namespace = namespace;
    }

    // script-bindings

    public TeaseLib.PersistentBoolean newBoolean(String name) {
        return newBoolean(namespace, name);
    }

    public TeaseLib.PersistentBoolean newBoolean(@SuppressWarnings("hiding") String namespace, String name) {
        return teaseLib.getBoolean(QualifiedName.of(domain, namespace, name));
    }

    public <T extends Enum<?>> TeaseLib.PersistentEnum<T> newEnum(String name, Class<T> enumClass) {
        return newEnum(namespace, name, enumClass);
    }

    public <T extends Enum<?>> TeaseLib.PersistentEnum<T> newEnum(@SuppressWarnings("hiding") String namespace, String name, Class<T> enumClass) {
        return teaseLib.getEnum(QualifiedName.of(domain, namespace, name), enumClass);
    }

    public TeaseLib.PersistentFloat newFloat(String name) {
        return newFloat(namespace, name);
    }

    public TeaseLib.PersistentFloat newFloat(@SuppressWarnings("hiding") String namespace, String name) {
        return teaseLib.getFloat(QualifiedName.of(domain, namespace, name));
    }

    public TeaseLib.PersistentNumber newNumber(String name) {
        return newNumber(namespace, name);
    }

    public TeaseLib.PersistentNumber newNumber(@SuppressWarnings("hiding") String namespace, String name) {
        return teaseLib.getNumber(QualifiedName.of(domain, namespace, name));
    }

    public TeaseLib.PersistentString newString(String name) {
        return newString(namespace, name);
    }

    public TeaseLib.PersistentString newString(@SuppressWarnings("hiding") String namespace, String name) {
        return teaseLib.getString(QualifiedName.of(domain, namespace, name));
    }

    // simple set/get/clear generics for enums

    public <T, S extends Enum<?> & PersistentEnum<T>> T value(S name) {
        var value = teaseLib.persistence.get(QualifiedName.of(domain, name));
        return name.fromString(value);
    }

    public <T, S extends Enum<?> & PersistentEnum<T>> void set(S name, T value) {
        teaseLib.persistence.set(QualifiedName.of(domain, name), name.toString(value));
    }

    public <T, S extends Enum<?> & PersistentEnum<T>> void clear(S name) {
        teaseLib.persistence.clear(QualifiedName.of(domain, name));
    }

    public <T, S extends Enum<?> & PersistentEnum<T>> void clear(S[] names) {
        for (var name : names) {
            clear(name);
        }
    }

    // global enum-typed variables

    public TeaseLib.PersistentBoolean newBoolean(Enum<?> name) {
        return teaseLib.getBoolean(QualifiedName.of(domain, name));
    }

    public <T extends Enum<?>> TeaseLib.PersistentEnum<T> newEnum(Enum<?> name, Class<T> enumClass) {
        return teaseLib.getEnum(QualifiedName.of(domain, name), enumClass);
    }

    public <T extends Enum<?>> TeaseLib.PersistentEnum<T> newEnum(Class<T> enumClass) {
        return teaseLib.getEnum(domain, enumClass);
    }

    public TeaseLib.PersistentFloat newFloat(Enum<?> name) {
        return teaseLib.getFloat(QualifiedName.of(domain, name));
    }

    public TeaseLib.PersistentNumber newNumber(Enum<?> name) {
        return teaseLib.getNumber(QualifiedName.of(domain, name));
    }

    public TeaseLib.PersistentString newString(Enum<?> name) {
        return teaseLib.getString(QualifiedName.of(domain, name));
    }

    // TODO never used - based on PersistentString -> move to game logic, refactor persistence provide out

    public <T extends Enum<T>> TeaseLib.PersistentSequence<T> newSequence(String name, T[] values) {
        return teaseLib.new PersistentSequence<>(domain, namespace, name, values);
    }

}
