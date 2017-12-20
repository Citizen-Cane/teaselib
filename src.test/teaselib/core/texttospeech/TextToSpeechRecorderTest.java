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
import teaselib.core.Configuration;
import teaselib.core.ResourceLoader;
import teaselib.core.util.ReflectionUtils;
import teaselib.test.DebugSetup;
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
        String resourcesRoot = "Test";
        ResourceLoader resources = new ResourceLoader(this.getClass(),
                ReflectionUtils.asAbsolutePath(ReflectionUtils.classParentName(this)));

        // TODO Reading speech doesn't work because the speech recorder creates resources only project-wide,
        // e.g. not for local classes

        List<Message> messages = Arrays.asList(new Message(actor, "I have a dream."),
                new Message(actor, "I dream of white sheep standing on the lawn."),
                new Message(actor, "I dream of yellow sheep standing on the lawn"),
                new Message(actor, "I dream of orange sheep standing on the lawn."),
                new Message(actor, "I dream of red sheep standing on the lawn."),
                new Message(actor, "I dream of green sheep standing on the lawn"),
                new Message(actor, "I dream of blue sheep standing on the lawn."),
                new Message(actor, "I dream of black sheep standing on the lawn"));
        assertEquals(8, messages.size());
        TextToSpeechRecorder recorder1 = recordVoices(new TestScriptScanner(messages), path, resourcesRoot, resources);
        assertEquals(8, recorder1.newEntries);
        assertEquals(0, recorder1.reusedDuplicates);
        assertEquals(0, recorder1.changedEntries);
        assertEquals(0, recorder1.upToDateEntries);

        // TODO Fails test
        // resources.addAssets(recorder1.assetPath().getAbsolutePath());

        Configuration config = new Configuration();
        new DebugSetup().withInput().withOutput().applyTo(config);
        TextToSpeechPlayer tts = new TextToSpeechPlayer(config);
        tts.loadActorVoiceProperties(resources);
        tts.acquireVoice(actor, resources);

        testAssets(tts, resources, messages);

        List<Message> updatedMessages = new ArrayList<>(messages);
        Message update = new Message(actor, "I dream of grey sheep standing on the lawn");
        updatedMessages.add(update);
        assertEquals(9, updatedMessages.size());
        TextToSpeechRecorder recorder2 = recordVoices(new TestScriptScanner(updatedMessages), path, resourcesRoot,
                resources);
        assertEquals(1, recorder2.newEntries);
        assertEquals(0, recorder2.reusedDuplicates);
        assertEquals(0, recorder2.changedEntries);
        assertEquals(8, recorder2.upToDateEntries);

        testAssets(tts, resources, updatedMessages);

        assertEquals(9, updatedMessages.size());
        TextToSpeechRecorder recorder3 = recordVoices(new TestScriptScanner(updatedMessages), path, resourcesRoot,
                resources);
        assertEquals(0, recorder3.newEntries);
        assertEquals(0, recorder3.reusedDuplicates);
        assertEquals(0, recorder3.changedEntries);
        assertEquals(9, recorder3.upToDateEntries);

        testAssets(tts, resources, updatedMessages);

        updatedMessages.remove(0);
        assertEquals(8, updatedMessages.size());
        TextToSpeechRecorder recorder4 = recordVoices(new TestScriptScanner(updatedMessages), path, resourcesRoot,
                resources);
        assertEquals(0, recorder4.newEntries);
        assertEquals(0, recorder4.reusedDuplicates);
        assertEquals(0, recorder4.changedEntries);
        assertEquals(8, recorder4.upToDateEntries);

        testAssets(tts, resources, updatedMessages);

        assertTrue(updatedMessages.contains(update));
        updatedMessages.add(update);
        assertEquals(9, updatedMessages.size());
        Message update2 = new Message(actor, "I dream of violet sheep standing on the lawn");
        updatedMessages.add(update2);
        assertTrue(updatedMessages.contains(update2));
        updatedMessages.add(update2);
        assertEquals(11, updatedMessages.size());
        TextToSpeechRecorder recorder5 = recordVoices(new TestScriptScanner(updatedMessages), path, resourcesRoot,
                resources);
        assertEquals(1, recorder5.newEntries);
        assertEquals(2, recorder5.reusedDuplicates);
        assertEquals(0, recorder5.changedEntries);
        assertEquals(8, recorder5.upToDateEntries);

        testAssets(tts, resources, updatedMessages);
    }

    private static void testAssets(TextToSpeechPlayer tts, ResourceLoader resources, List<Message> messages) {
        // for (Message message : messages) {
        // Message speech = tts.createSpeechMessage(message, resources);
        // speech.getParts().stream().filter((part) -> part.type == Type.Speech)
        // .forEach((part) -> assertTrue(Message.Type.isSound(part.value)));
        // }
    }

    TextToSpeechRecorder recordVoices(ScriptScanner scriptScanner, File path, String resourcesRoot,
            ResourceLoader resources) throws IOException, InterruptedException, ExecutionException {
        TextToSpeechRecorder recorder = new TextToSpeechRecorder(path, resourcesRoot, resources, new TextVariables());

        recorder.preparePass("Test", "Test");
        recorder.run(scriptScanner);
        recorder.finish();

        return recorder;
    }
}
