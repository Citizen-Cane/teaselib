package teaselib.core.jni;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Predicate;

public class NativeLibraries {

    private NativeLibraries() { //
    }

    public static final String TEASELIB_FRAMEWORK = "TeaseLibFramework";

    public static final String TEASELIB_AI = "TeaseLibAI";
    public static final String TEASELIB_SR = "TeaseLibSR";
    public static final String TEASELIB_TTS = "TeaseLibTTS";
    public static final String TEASELIB_X360C = "TeaseLibx360c";

    private static final Set<String> loaded = new HashSet<>();

    public static void require(String... libraries) {
        Arrays.stream(libraries).filter(Predicate.not(loaded::contains)).forEach(LibraryLoader::load);
    }

}
