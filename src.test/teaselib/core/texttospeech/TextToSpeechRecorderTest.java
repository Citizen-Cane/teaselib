package teaselib.core.texttospeech;

import static org.junit.Assert.*;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutionException;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import teaselib.Actor;
import teaselib.Message;
import teaselib.Sexuality.Gender;
import teaselib.core.ResourceLoader;
import teaselib.util.TextVariables;

public class TextToSpeechRecorderTest {

    protected Actor actor = new Actor("Mr. Foo", Gender.Masculine, Locale.US);

    static class TestScriptScanner implements ScriptScanner {
        private final List<Message> messages;

        public TestScriptScanner(Message... messages) {
            this(Arrays.asList(messages));
        }

        public TestScriptScanner(List<Message> messages) {
            this.messages = messages;
        }

        @Override
        public String getScriptName() {
            return "test";
        }

        @Override
        public Iterator<Message> iterator() {
            return messages.iterator();
        }
    }

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    @Test
    public void testAll() throws IOException, InterruptedException, ExecutionException {
        File path = tempFolder.getRoot();
        String resourcesRoot = "test";

        List<Message> messages = Arrays.asList(new Message(actor, "I have a dream."),
                new Message(actor, "I dream of white sheep standing on the lawn."),
                new Message(actor, "I dream of yellow sheep standing on the lawn"),
                new Message(actor, "I dream of orange sheep standing on the lawn."),
                new Message(actor, "I dream of red sheep standing on the lawn."),
                new Message(actor, "I dream of green sheep standing on the lawn"),
                new Message(actor, "I dream of blue sheep standing on the lawn."),
                new Message(actor, "I dream of black sheep standing on the lawn"));
        assertEquals(8, messages.size());
        TextToSpeechRecorder recorder1 = recordVoices(new TestScriptScanner(messages), path, resourcesRoot);
        assertEquals(8, recorder1.newEntries);
        assertEquals(0, recorder1.reusedDuplicates);
        assertEquals(0, recorder1.changedEntries);
        assertEquals(0, recorder1.upToDateEntries);

        List<Message> updatedMessages = new ArrayList<>(messages);
        Message update = new Message(actor, "I dream of grey sheep standing on the lawn");
        updatedMessages.add(update);
        assertEquals(9, updatedMessages.size());
        TextToSpeechRecorder recorder2 = recordVoices(new TestScriptScanner(updatedMessages), path, resourcesRoot);
        assertEquals(1, recorder2.newEntries);
        assertEquals(0, recorder2.reusedDuplicates);
        assertEquals(0, recorder2.changedEntries);
        assertEquals(8, recorder2.upToDateEntries);

        assertEquals(9, updatedMessages.size());
        TextToSpeechRecorder recorder3 = recordVoices(new TestScriptScanner(updatedMessages), path, resourcesRoot);
        assertEquals(0, recorder3.newEntries);
        assertEquals(0, recorder3.reusedDuplicates);
        assertEquals(0, recorder3.changedEntries);
        assertEquals(9, recorder3.upToDateEntries);

        updatedMessages.remove(0);
        assertEquals(8, updatedMessages.size());
        TextToSpeechRecorder recorder4 = recordVoices(new TestScriptScanner(updatedMessages), path, resourcesRoot);
        assertEquals(0, recorder4.newEntries);
        assertEquals(0, recorder4.reusedDuplicates);
        assertEquals(0, recorder4.changedEntries);
        assertEquals(8, recorder4.upToDateEntries);

        assertTrue(updatedMessages.contains(update));
        updatedMessages.add(update);
        assertEquals(9, updatedMessages.size());
        Message update2 = new Message(actor, "I dream of violet sheep standing on the lawn");
        updatedMessages.add(update2);
        assertTrue(updatedMessages.contains(update2));
        updatedMessages.add(update2);
        assertEquals(11, updatedMessages.size());
        TextToSpeechRecorder recorder5 = recordVoices(new TestScriptScanner(updatedMessages), path, resourcesRoot);
        assertEquals(1, recorder5.newEntries);
        assertEquals(2, recorder5.reusedDuplicates);
        assertEquals(0, recorder5.changedEntries);
        assertEquals(8, recorder5.upToDateEntries);

    }

    TextToSpeechRecorder recordVoices(ScriptScanner scriptScanner, File path, String resourcesRoot)
            throws IOException, InterruptedException, ExecutionException {
        ResourceLoader resources = new ResourceLoader(this.getClass());
        TextToSpeechRecorder recorder = new TextToSpeechRecorder(path, resourcesRoot, resources, new TextVariables());

        recorder.preparePass("Test", "Test");
        recorder.run(scriptScanner);

        recorder.finish();

        return recorder;
    }
}
