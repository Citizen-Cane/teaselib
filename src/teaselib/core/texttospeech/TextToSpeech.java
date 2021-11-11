package teaselib.core.texttospeech;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import teaselib.Sexuality.Gender;
import teaselib.core.AudioSync;
import teaselib.core.Closeable;
import teaselib.core.events.DelegateExecutor;
import teaselib.core.texttospeech.implementation.TeaseLibTTS;
import teaselib.core.texttospeech.implementation.Unsupported;
import teaselib.core.util.ExceptionUtil;

public class TextToSpeech implements Closeable {
    public static final Voice None = new Voice() {

        @Override
        public TextToSpeechImplementation tts() {
            return Unsupported.Instance;
        }

        @Override
        public String guid() {
            return "None";
        }

        @Override
        public Gender gender() {
            return Male;
        }

        @Override
        public String locale() {
            return "??-??";
        }

        @Override
        public VoiceInfo info() {
            return new VoiceInfo("None", "None", "None");
        }

    };

    static final Set<String> BlackList = new HashSet<>(Arrays.asList("LTTS7Ludoviko", "MSMary", "MSMike"));
    private static final String[] NoHints = null;

    private final Set<Class<?>> ttsSDKClasses = new LinkedHashSet<>();
    private final Map<Class<? extends TextToSpeechImplementation>, TextToSpeechImplementation> ttsSDKCLass2Implementation = new LinkedHashMap<>();
    private final Map<String, TextToSpeechImplementation> ttsSDKs = new LinkedHashMap<>();
    private final Map<String, DelegateExecutor> ttsExecutors = new LinkedHashMap<>();

    public final AudioSync audioSync;

    private final Map<String, Voice> voices = new LinkedHashMap<>();

    private static void ttsEngineNotInitialized() {
        throw new IllegalStateException("TTS engine not initialized");
    }

    static TextToSpeech allSystemVoices() {
        return new TextToSpeech(operatingSpecificSDKs());
    }

    static TextToSpeech none() {
        return new TextToSpeech(Collections.emptyList());
    }

    private TextToSpeech(List<Class<? extends TextToSpeechImplementation>> ttsClasses) {
        this.audioSync = new AudioSync();
        for (Class<? extends TextToSpeechImplementation> ttsClass : ttsClasses) {
            addImplementation(ttsClass);
        }
    }

    // TODO only used by test code -> use production code constructor instead
    TextToSpeech(TextToSpeechImplementation ttsImpl) {
        this.audioSync = new AudioSync();
        addSDK(ttsImpl, newDelegateExecutor(ttsImpl.sdkName()));
    }

    private static List<Class<? extends TextToSpeechImplementation>> operatingSpecificSDKs() {
        return Arrays.asList(TeaseLibTTS.Loquendo.class, TeaseLibTTS.Microsoft.class);
    }

    private void addImplementation(Class<? extends TextToSpeechImplementation> ttsClass) {
        if (ttsSDKCLass2Implementation.containsKey(ttsClass)) {
            throw new IllegalStateException("Implementation " + ttsClass.getName() + " already added.");
        } else {
            addImplementationInstance(ttsClass);
            ttsSDKClasses.add(ttsClass);
        }
    }

    private void addImplementationInstance(Class<? extends TextToSpeechImplementation> ttsClass) {
        var delegateThread = newDelegateExecutor(ttsClass.getSimpleName());
        delegateThread.run(() -> {
            var getInstance = ttsClass.getDeclaredMethod("newInstance");
            TextToSpeechImplementation newTTS = (TextToSpeechImplementation) getInstance.invoke(this);
            addSDK(newTTS, delegateThread);
        });
    }

    private void addSDK(TextToSpeechImplementation ttsImpl, DelegateExecutor executor) {
        Map<String, Voice> newVoices = getVoices(ttsImpl);
        if (!newVoices.isEmpty()) {
            addNewVoices(newVoices);
            ttsSDKCLass2Implementation.put(ttsImpl.getClass(), ttsImpl);
            ttsSDKs.put(ttsImpl.sdkName(), ttsImpl);
            ttsExecutors.put(ttsImpl.sdkName(), executor);
        } else {
            ttsImpl.dispose();
        }
    }

    private static Map<String, Voice> getVoices(TextToSpeechImplementation ttsImpl) {
        List<Voice> voices;
        try {
            voices = ttsImpl.getVoices();
        } catch (UnsupportedOperationException e) {
            return Collections.emptyMap();
        }

        Map<String, Voice> newVoices = new LinkedHashMap<>();
        for (Voice voice : voices) {
            if (!isBlackListed(voice)) {
                newVoices.put(voice.guid(), voice);
            }
        }
        return newVoices;
    }

    private void addNewVoices(Map<String, Voice> newVoices) {
        for (Entry<String, Voice> entry : newVoices.entrySet()) {
            if (!notAlreadyAVailableByVendorSepcificSDK(entry)) {
                voices.put(entry.getKey(), entry.getValue());
            }
        }
    }

    private boolean notAlreadyAVailableByVendorSepcificSDK(Entry<String, Voice> entry) {
        return voices.containsKey(entry.getKey());
    }

    private static DelegateExecutor newDelegateExecutor(String sdkName) {
        return new DelegateExecutor(sdkName + "Text-To-Speech");
    }

    public Map<String, Voice> getVoices() {
        return voices;
    }

    private static boolean isBlackListed(Voice voice) {
        return BlackList.contains(voice.guid());
    }

    public void speak(Voice voice, String prompt) {
        speak(voice, prompt, new String[] {});
    }

    /**
     * @param voice
     * @param prompt
     * @param hints
     *            Mood or other hints that might affect the prompt
     * @throws InterruptedException
     */
    public void speak(Voice voice, String prompt, String[] hints) {
        TextToSpeechImplementation tts = voice.tts();

        if (tts != null && ttsSDKs.containsKey(tts.sdkName())) {
            run(tts, () -> audioSync.synchronizeTextToSpeech(() -> speak(voice, prompt, hints, tts)));
        } else {
            ttsEngineNotInitialized();
        }
    }

    private static void speak(Voice voice, String prompt, String[] hints, TextToSpeechImplementation tts) {
        try {
            tts.setVoice(voice);
            tts.setHints(hints);
            tts.speak(prompt);
        } finally {
            tts.setHints(NoHints);
        }
    }

    public String speak(Voice voice, String prompt, File file, String[] hints) throws IOException {
        var soundFilePath = new StringBuilder();
        TextToSpeechImplementation tts = voice.tts();

        if (tts != null && ttsSDKs.containsKey(tts.sdkName())) {
            try {
                run(tts, () -> {
                    try {
                        tts.setVoice(voice);
                        tts.setHints(hints);
                        String actualPath = tts.speak(prompt, file.getAbsolutePath());
                        soundFilePath.append(actualPath);
                    } catch (IOException e) {
                        throw ExceptionUtil.asRuntimeException(e);
                    } finally {
                        tts.setHints(NoHints);
                    }
                });
            } catch (RuntimeException e) {
                throwIOException(e);
            }
        } else {
            ttsEngineNotInitialized();
        }

        return soundFilePath.toString();
    }

    private static void throwIOException(RuntimeException e) throws IOException {
        Throwable cause = e.getCause();
        if (cause instanceof IOException) {
            throw (IOException) cause;
        } else {
            throw e;
        }
    }

    private void run(TextToSpeechImplementation tts, DelegateExecutor.Runnable delegate) {
        var delegateExecutor = ttsExecutors.get(tts.sdkName());
        delegateExecutor.run(delegate);
    }

    public void stop(Voice voice) {
        TextToSpeechImplementation tts = voice.tts();

        if (tts != null && ttsSDKs.containsKey(tts.sdkName())) {
            tts.stop();
        } else {
            ttsEngineNotInitialized();
        }
    }

    /**
     * Estimate the duration for displaying the text when not spoken by speech synthesis.
     * 
     * @param text
     *            Text to estimate the time needed to speak for
     * @return duration, in milliseconds
     */
    public static long getEstimatedSpeechDuration(String text) {
        long millisecondsPerLetter = 70;
        long pauseAfterParagraph = 1 * 1000L;
        return text.length() * millisecondsPerLetter + pauseAfterParagraph;
    }

    public void initPhoneticDictionary(PronunciationDictionary pronunciationDictionary) throws IOException {
        try {
            for (Entry<String, TextToSpeechImplementation> ttsSDK : ttsSDKs.entrySet()) {
                TextToSpeechImplementation ttsImpl = ttsSDK.getValue();
                run(ttsImpl, () -> {
                    try {
                        ttsImpl.setPhoneticDictionary(pronunciationDictionary);
                    } catch (IOException e) {
                        throw ExceptionUtil.asRuntimeException(e);
                    }
                });
            }
        } catch (RuntimeException e) {
            throwIOException(e);
        }
    }

    @Override
    public void close() {
        for (Entry<String, DelegateExecutor> entry : ttsExecutors.entrySet()) {
            DelegateExecutor executorService = entry.getValue();
            String key = entry.getKey();
            executorService.run(ttsSDKs.remove(key)::close);
        }

        for (Entry<String, DelegateExecutor> entry : ttsExecutors.entrySet()) {
            DelegateExecutor executorService = entry.getValue();
            executorService.shutdown();
        }

        ttsExecutors.clear();
        voices.clear();
    }

}
