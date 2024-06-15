package teaselib.core.texttospeech;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import teaselib.core.concurrency.NamedExecutorService;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TextToSpeechMultithreadedTest {

    @TempDir
    Path tempFolder;

    private Path getNewTempFile() throws IOException {
        return Files.createTempFile(tempFolder, "speech", "wav");
    }

    @Test
    public void testTextToSpeechIsSessionInstance() throws InterruptedException, ExecutionException, IOException {
        Callable<Path> test = () -> {
            try (var tts = TextToSpeech.allSystemVoices()) {
                Map<String, Voice> voices = tts.getVoices();
                assertFalse(voices.isEmpty());
                Voice voice = voices.values().iterator().next();
                Path path = getNewTempFile();
                tts.speak(voice, path.toAbsolutePath().toString(), new String[]{});
                return path;
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
