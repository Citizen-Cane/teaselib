package teaselib.core;

import static org.junit.Assert.*;

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

    private static String text(Script script, Message message) {
        Decorator[] decorators = script.decorators(Optional.empty());
        return RenderedMessage.of(message, decorators).stream().filter(part -> part.type == Type.Text)
                .map(part -> part.value).findFirst().get();
    }
}
