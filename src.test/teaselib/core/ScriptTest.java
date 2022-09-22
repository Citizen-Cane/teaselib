package teaselib.core;

import static org.junit.Assert.*;

import java.io.IOException;
import java.util.Optional;

import org.junit.Test;

import teaselib.Message;
import teaselib.Message.Type;
import teaselib.TeaseScript;
import teaselib.core.media.RenderedMessage;
import teaselib.core.media.RenderedMessage.Decorator;
import teaselib.test.TestScript;
import teaselib.util.TextVariables;

public class ScriptTest {
    @Test
    public void testScriptVariableDefault() throws IOException {
        try (TestScript script = new TestScript()) {
            Message message = new Message(script.actor, "Listen #slave:");

            assertEquals("Listen slave:", text(script, message));
        }
    }

    @Test
    public void testScriptVariableOverride() throws IOException {
        try (TestScript script = new TestScript()) {
            script.actor.textVariables.set(TextVariables.Identity.Slave_Name, "Anne");
            Message message = new Message(script.actor, "Listen #slave:");

            assertEquals("Listen Anne:", text(script, message));
        }
    }

    @Test
    public void testScriptVariableOverrideAliasKey() throws IOException {
        try (TestScript script = new TestScript()) {
            script.actor.textVariables.set(TextVariables.Identity.Alias.Slave, "Anne");
            Message message = new Message(script.actor, "Listen #slave:");

            assertEquals("Listen Anne:", text(script, message));
        }
    }

    @Test
    public void testScriptVariableOverrideUpdatesAlias() throws IOException {
        try (TestScript script = new TestScript()) {
            script.actor.textVariables.set(TextVariables.Identity.Slave_Name, "Anne");
            Message message = new Message(script.actor, "Listen #slave:");

            assertEquals("Listen Anne:", text(script, message));
        }
    }

    @Test
    public void testScriptVariableOverrideAndUndo() throws IOException {
        try (TestScript script = new TestScript()) {
            Message message = new Message(script.actor, "Listen #slave:");

            assertEquals("Listen slave:", text(script, message));

            script.actor.textVariables.set(TextVariables.Identity.Slave_Name, "Anne");
            assertEquals("Listen Anne:", text(script, message));

            script.actor.textVariables.remove(TextVariables.Identity.Slave_Name);
            assertEquals("Listen slave:", text(script, message));
        }
    }

    @Test
    public void testScriptVariableOverrideAndUndoAlias() throws IOException {
        try (TestScript script = new TestScript()) {
            Message message = new Message(script.actor, "Listen #slave:");

            assertEquals("Listen slave:", text(script, message));

            script.actor.textVariables.set(TextVariables.Identity.Alias.Slave, "Anne");
            assertEquals("Listen Anne:", text(script, message));

            script.actor.textVariables.remove(TextVariables.Identity.Alias.Slave);
            assertEquals("Listen slave:", text(script, message));
        }
    }

    public static class FoobarScript extends TeaseScript {
        public FoobarScript(TeaseScript script) {
            super(script, script.actor);
        }
    }

    @Test
    public void testScriptsAreCachedByActor() throws IOException {
        try (TestScript one = new TestScript(); TestScript another = new TestScript()) {
            FoobarScript foo = one.script(FoobarScript.class);
            FoobarScript fooAsWell = one.script(FoobarScript.class);
            assertSame(foo, fooAsWell);

            FoobarScript bar = another.script(FoobarScript.class);
            assertNotSame(foo, bar);
        }
    }

    public class NotAStaticClassScript extends TeaseScript {
        public NotAStaticClassScript(TeaseScript script) {
            super(script, script.actor);
        }
    }

    @Test(expected = NoSuchMethodException.class)
    public void testScriptClassNestedInNonScriptClassFails() throws IOException, NoSuchMethodException {
        try (TestScript one = new TestScript()) {
            NotAStaticClassScript foo;
            try {
                foo = one.script(NotAStaticClassScript.class);
            } catch (RuntimeException e) {
                if (e.getCause() instanceof NoSuchMethodException)
                    throw (NoSuchMethodException) e.getCause();
                else
                    throw e;
            }
            assertNull(foo);
        }
    }

    static class StaticTestScript extends TeaseScript {
        public StaticTestScript(TestScript script) {
            super(script, script.actor);
        }

        public class NestedScriptClass extends TeaseScript {
            public NestedScriptClass(TeaseScript script) {
                super(script, script.actor);
            }
        }
    }

    @Test
    public void testNestedScriptClass() throws IOException {
        TestScript main = new TestScript();
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

        public static class FooScript extends TeaseScript {
            final Script baz = script(BazScript.class);

            public FooScript(TeaseScript script) {
                super(script, script.actor);
            }
        }

        public static class BazScript extends TeaseScript {
            public BazScript(TeaseScript script) {
                super(script, script.actor);
            }
        }

    }

    @Test
    public void testNestedScriptCaching() throws IOException {
        try (TestScript test = new TestScript()) {
            Script script = test.script(NestedScriptCaching.FooScript.class);

            assertNotNull(script);
        }
    }

}
