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
        Decorator[] decorators = script.decorators(Optional.empty());

        assertEquals("Listen slave:", RenderedMessage.of(message, decorators).stream()
                .filter(part -> part.type == Type.Text).map(part -> part.value).findFirst().get());
    }

    @Test
    public void testScriptVariableOverride() {
        Script script = TestScript.getOne();
        script.actor.textVariables.put(TextVariables.Names.Slave, "Anne");
        Message message = new Message(script.actor, "Listen #slave:");
        Decorator[] decorators = script.decorators(Optional.empty());
        assertEquals("Listen Anne:", RenderedMessage.of(message, decorators).stream()
                .filter(part -> part.type == Type.Text).map(part -> part.value).findFirst().get());
    }

    @Test
    public void testScriptVariableCombined() {
        Script script = TestScript.getOne();
        Message message = new Message(script.actor, "Listen #slave:");
        Decorator[] decorators = script.decorators(Optional.empty());

        assertEquals("Listen slave:", RenderedMessage.of(message, decorators).stream()
                .filter(part -> part.type == Type.Text).map(part -> part.value).findFirst().get());

        script.actor.textVariables.put(TextVariables.Names.Slave, "Anne");
        assertEquals("Listen Anne:", RenderedMessage.of(message, decorators).stream()
                .filter(part -> part.type == Type.Text).map(part -> part.value).findFirst().get());

        script.actor.textVariables.remove(TextVariables.Names.Slave);
        assertEquals("Listen slave:", RenderedMessage.of(message, decorators).stream()
                .filter(part -> part.type == Type.Text).map(part -> part.value).findFirst().get());
    }
}
