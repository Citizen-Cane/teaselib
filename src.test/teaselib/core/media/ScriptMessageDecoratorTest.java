package teaselib.core.media;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import teaselib.Message.Type;
import teaselib.MessagePart;

public class ScriptMessageDecoratorTest {

    @Test
    public void testScriptMessageDecorator() {
        assertEquals(new MessagePart(Type.Delay, "30.0"), ScriptMessageDecorator
                .accumulateDelay(new MessagePart(Type.Delay, "10"), new MessagePart(Type.Delay, "20.0")));

        assertEquals(new MessagePart(Type.Delay, "120.0 150.0"), ScriptMessageDecorator
                .accumulateDelay(new MessagePart(Type.Delay, "30"), new MessagePart(Type.Delay, "90.0 120")));

        assertEquals(new MessagePart(Type.Delay, "120.0 150.0"), ScriptMessageDecorator
                .accumulateDelay(new MessagePart(Type.Delay, "90 120"), new MessagePart(Type.Delay, "30.0")));

        assertEquals(new MessagePart(Type.Delay, "120.0 180.0"), ScriptMessageDecorator
                .accumulateDelay(new MessagePart(Type.Delay, "30.0 60"), new MessagePart(Type.Delay, "90 120.0")));

        assertEquals(new MessagePart(Type.Delay, "5.5 20.0"), ScriptMessageDecorator
                .accumulateDelay(new MessagePart(Type.Delay, "2 12.5"), new MessagePart(Type.Delay, "3.5 7.5")));
    }
}
