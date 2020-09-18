package teaselib.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertSame;

import java.util.Optional;

import org.junit.Test;

import teaselib.Message;
import teaselib.Message.Type;
import teaselib.core.media.RenderedMessage;
import teaselib.core.media.RenderedMessage.Decorator;
import teaselib.test.TestScript;
import teaselib.util.TextVariables;

public class ScriptTest {
    @Test
    public void testScriptVariableDefault() {
        Script script = TestScript.getOne();
        Message message = new Message(script.actor, "Listen #slave:");

        assertEquals("Listen slave:", text(script, message));
    }

    @Test
    public void testScriptVariableOverride() {
        Script script = TestScript.getOne();

        script.actor.textVariables.set(TextVariables.Identity.Slave_Name, "Anne");
        Message message = new Message(script.actor, "Listen #slave:");

        assertEquals("Listen Anne:", text(script, message));
    }

    @Test
    public void testScriptVariableOverrideAliasKey() {
        Script script = TestScript.getOne();

        script.actor.textVariables.set(TextVariables.Identity.Alias.Slave, "Anne");
        Message message = new Message(script.actor, "Listen #slave:");

        assertEquals("Listen Anne:", text(script, message));
    }

    @Test
    public void testScriptVariableOverrideUpdatesAlias() {
        Script script = TestScript.getOne();

        script.actor.textVariables.set(TextVariables.Identity.Slave_Name, "Anne");
        Message message = new Message(script.actor, "Listen #slave:");

        assertEquals("Listen Anne:", text(script, message));
    }

    @Test
    public void testScriptVariableOverrideAndUndo() {
        Script script = TestScript.getOne();
        Message message = new Message(script.actor, "Listen #slave:");

        assertEquals("Listen slave:", text(script, message));

        script.actor.textVariables.set(TextVariables.Identity.Slave_Name, "Anne");
        assertEquals("Listen Anne:", text(script, message));

        script.actor.textVariables.remove(TextVariables.Identity.Slave_Name);
        assertEquals("Listen slave:", text(script, message));
    }

    @Test
    public void testScriptVariableOverrideAndUndoAlias() {
        Script script = TestScript.getOne();
        Message message = new Message(script.actor, "Listen #slave:");

        assertEquals("Listen slave:", text(script, message));

        script.actor.textVariables.set(TextVariables.Identity.Alias.Slave, "Anne");
        assertEquals("Listen Anne:", text(script, message));

        script.actor.textVariables.remove(TextVariables.Identity.Alias.Slave);
        assertEquals("Listen slave:", text(script, message));
    }

    public static class FoobarScript extends Script {
        public FoobarScript(Script script) {
            super(script, script.actor);
        }
    }

    @Test
    public void testScriptsAreCachedByActor() {
        Script one = TestScript.getOne();
        Script another = TestScript.getOne();

        FoobarScript foo = one.script(FoobarScript.class);
        FoobarScript fooAsWell = one.script(FoobarScript.class);
        assertSame(foo, fooAsWell);

        FoobarScript bar = another.script(FoobarScript.class);
        assertNotSame(foo, bar);
    }

    public class NotAStaticClassScript extends Script {
        public NotAStaticClassScript(Script script) {
            super(script, script.actor);
        }
    }

    @Test(expected = NoSuchMethodException.class)
    public void testScriptClassNestedInNonScriptClassFails() throws Throwable {
        Script one = TestScript.getOne();

        NotAStaticClassScript foo;
        try {
            foo = one.script(NotAStaticClassScript.class);
        } catch (RuntimeException e) {
            throw e.getCause();
        }
    }

    static class StaticTestScript extends Script {
        public StaticTestScript(TestScript script) {
            super(script, script.actor);
        }

        public class NestedScriptClass extends Script {
            public NestedScriptClass(Script script) {
                super(script, script.actor);
            }
        }
    }

    @Test
    public void testNestedScriptClass() {
        TestScript main = TestScript.getOne();
        Script script = new StaticTestScript(main);

        StaticTestScript.NestedScriptClass foo = script.script(StaticTestScript.NestedScriptClass.class);
        assertNotNull(foo);
    }

    private static String text(Script script, Message message) {
        Decorator[] decorators = script.decorators(Optional.empty());
        return RenderedMessage.of(message, decorators).stream().filter(part -> part.type == Type.Text)
                .map(part -> part.value).findFirst().orElseThrow();
    }

    public static class NestedScriptCaching {

        private NestedScriptCaching() { //
        }

        public static class FooScript extends Script {
            final Script baz = script(BazScript.class);

            public FooScript(Script script) {
                super(script, script.actor);
            }
        }

        public static class BazScript extends Script {
            public BazScript(Script script) {
                super(script, script.actor);
            }
        }

    }

    @Test
    public void testNestedScriptCaching() {
        TestScript test = TestScript.getOne();
        Script script = test.script(NestedScriptCaching.FooScript.class);

        assertNotNull(script);
    }

}
