package teaselib.core.texttospeech;

import static org.junit.jupiter.api.Assertions.*;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

import org.junit.Rule;
import org.junit.jupiter.api.Test;
import org.junit.rules.TemporaryFolder;

import teaselib.core.concurrency.NamedExecutorService;

class TextToSpeechMultithreadedTest {
    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    private File getNewTempFile() throws IOException {
        synchronized (tempFolder) {
            File file = tempFolder.newFile();
            return file;
        }
    }

    @Test
    public void testTextToSpeechIsSessionInstance() throws InterruptedException, ExecutionException, IOException {
        tempFolder.create();

        Callable<Path> test = () -> {
            try (var tts = TextToSpeech.allSystemVoices()) {
                Map<String, Voice> voices = tts.getVoices();
                assertFalse(voices.isEmpty());
                Voice voice = voices.values().iterator().next();
                File file = getNewTempFile();
                tts.speak(voice, file.getAbsolutePath(), new String[] {});
                return file.toPath();
            }
        };

        ExecutorService ex = NamedExecutorService.newUnlimitedThreadPool("TTS test", Long.MAX_VALUE,
                TimeUnit.MILLISECONDS);
        var f1 = ex.submit(test);
        var f2 = ex.submit(test);
        var f3 = ex.submit(test);

        Path path1 = f1.get();
        assertTrue(Files.exists(path1));

        Path path2 = f2.get();
        assertTrue(Files.exists(path2));

        Path path3 = f3.get();
        assertTrue(Files.exists(path3));

        ex.shutdown();
    }

}
