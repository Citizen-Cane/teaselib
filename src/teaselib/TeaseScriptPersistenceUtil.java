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
    protected TeaseScriptPersistenceUtil(TeaseLib teaseLib,
            ResourceLoader resources, Actor actor, String namespace) {
        super(teaseLib, resources, actor, namespace);
    }

    protected TeaseScriptPersistenceUtil(TeaseScriptBase script, Actor actor) {
        super(script, actor);
    }

    public void clear(String name) {
        teaseLib.clear(namespace, name);
    }

    public void clear(Enum<?> name) {
        teaseLib.clear(namespace, name);
    }

    public void set(Enum<?> name, boolean value) {
        teaseLib.set(namespace, name, value);
    }

    public void set(Enum<?> name, int value) {
        teaseLib.set(namespace, name, value);
    }

    public void set(Enum<?> name, long value) {
        teaseLib.set(namespace, name, value);
    }

    public void set(Enum<?> name, double value) {
        teaseLib.set(namespace, name, value);
    }

    public void set(Enum<?> name, String value) {
        teaseLib.set(namespace, name, value);
    }

    public void set(String name, boolean value) {
        teaseLib.set(namespace, name, value);
    }

    public void set(String name, int value) {
        teaseLib.set(namespace, name, value);
    }

    public void set(String name, long value) {
        teaseLib.set(namespace, name, value);
    }

    public void set(String name, double value) {
        teaseLib.set(namespace, name, value);
    }

    public void set(String name, String value) {
        teaseLib.set(namespace, name, value);
    }

    public boolean getBoolean(String name) {
        return teaseLib.getBoolean(namespace, name);
    }

    public int getInteger(String name) {
        return teaseLib.getInteger(namespace, name);
    }

    public long getLong(String name) {
        return teaseLib.getInteger(namespace, name);
    }

    public double getFloat(String name) {
        return teaseLib.getFloat(namespace, name);
    }

    public String getString(String name) {
        return teaseLib.getString(namespace, name);
    }

    public boolean getBoolean(Enum<?> name) {
        return teaseLib.getBoolean(namespace, name);
    }

    public int getInteger(Enum<?> name) {
        return teaseLib.getInteger(namespace, name);
    }

    public long getLong(Enum<?> name) {
        return teaseLib.getLong(namespace, name);
    }

    public double getFloat(Enum<?> name) {
        return teaseLib.getFloat(namespace, name);
    }

    public String getString(Enum<?> name) {
        return teaseLib.getString(namespace, name);
    }

    public void clear(String global, String name) {
        teaseLib.clear(global, name);
    }

    public void clear(String global, Enum<?> name) {
        teaseLib.clear(global, name);
    }

    public void set(String global, Enum<?> name, boolean value) {
        teaseLib.set(global, name, value);
    }

    public void set(String global, Enum<?> name, int value) {
        teaseLib.set(global, name, value);
    }

    public void set(String global, Enum<?> name, long value) {
        teaseLib.set(global, name, value);
    }

    public void set(String global, Enum<?> name, double value) {
        teaseLib.set(global, name, value);
    }

    public void set(String global, Enum<?> name, String value) {
        teaseLib.set(global, name, value);
    }

    public void set(String global, String name, boolean value) {
        teaseLib.set(global, name, value);
    }

    public void set(String global, String name, int value) {
        teaseLib.set(global, name, value);
    }

    public void set(String global, String name, long value) {
        teaseLib.set(global, name, value);
    }

    public void set(String global, String name, double value) {
        teaseLib.set(global, name, value);
    }

    public void set(String global, String name, String value) {
        teaseLib.set(global, name, value);
    }

    public boolean getBoolean(String global, String name) {
        return teaseLib.getBoolean(global, name);
    }

    public int getInteger(String global, String name) {
        return teaseLib.getInteger(global, name);
    }

    public long getLong(String global, String name) {
        return teaseLib.getInteger(global, name);
    }

    public double getFloat(String global, String name) {
        return teaseLib.getFloat(global, name);
    }

    public String getString(String global, String name) {
        return teaseLib.getString(global, name);
    }

    public boolean getBoolean(String global, Enum<?> name) {
        return teaseLib.getBoolean(global, name);
    }

    public int getInteger(String global, Enum<?> name) {
        return teaseLib.getInteger(global, name);
    }

    public long getLong(String global, Enum<?> name) {
        return teaseLib.getLong(global, name);
    }

    public double getFloat(String global, Enum<?> name) {
        return teaseLib.getFloat(global, name);
    }

    public String getString(String global, Enum<?> name) {
        return teaseLib.getString(global, name);
    }

}
