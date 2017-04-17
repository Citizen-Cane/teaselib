package teaselib;

import teaselib.core.ResourceLoader;
import teaselib.core.TeaseLib;
import teaselib.core.TeaseScriptBase;

/**
 * @author Citizen-Cane
 *
 *         Implements non-object oriented persistence helpers.
 */

public class TeaseScriptPersistenceUtil extends TeaseScriptPersistence {
    protected TeaseScriptPersistenceUtil(TeaseLib teaseLib, ResourceLoader resources, Actor actor,
            String namespace) {
        super(teaseLib, resources, actor, namespace);
    }

    protected TeaseScriptPersistenceUtil(TeaseScriptBase script, Actor actor) {
        super(script, actor);
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
}
