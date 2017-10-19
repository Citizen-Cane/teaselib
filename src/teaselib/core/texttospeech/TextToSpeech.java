package teaselib.core.texttospeech;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import teaselib.core.ScriptInterruptedException;
import teaselib.core.events.Delegate;
import teaselib.core.events.DelegateExecutor;
import teaselib.core.texttospeech.implementation.TeaseLibTTS;
import teaselib.core.util.ExceptionUtil;

public class TextToSpeech {
    private final Map<String, TextToSpeechImplementation> ttsSDKs = new LinkedHashMap<>();
    private final Map<String, DelegateExecutor> ttsExecutors = new LinkedHashMap<>();

    private final Map<String, Voice> voices = new LinkedHashMap<>();

    private String[] NoHints = null;

    private static void ttsEngineNotInitialized() {
        throw new IllegalStateException("TTS engine not initialized");
    }

    public static final Lock AudioOutput = new ReentrantLock();

    public static final Set<String> BlackList = new HashSet<>(Arrays.asList("LTTS7Ludoviko", "MSMary", "MSMike"));

    public TextToSpeech() {
        Set<String> names = getImplementations();
        if (!names.isEmpty()) {
            addImplementation(names.iterator().next());
        }
    }

    public TextToSpeech(TextToSpeechImplementation ttsImpl) {
        addSDK(ttsImpl, newDelegateExecutor());
    }

    public TextToSpeech(Class<TextToSpeechImplementation> ttsClass) {
        addImplementation(ttsClass);
    }

    private static Set<String> getImplementations() {
        Set<String> names = new HashSet<>();
        names.add(TeaseLibTTS.class.getName());
        return names;
    }

    private void addImplementation(String className) {
        try {
            Class<?> ttsClass = getClass().getClassLoader().loadClass(className);
            addImplementation(ttsClass);
        } catch (ReflectiveOperationException e) {
            throw ExceptionUtil.asRuntimeException(e);
        }
    }

    private void addImplementation(Class<?> ttsClass) {
        DelegateExecutor delegateThread = newDelegateExecutor();

        Delegate delegate = new Delegate() {
            @Override
            public void run() throws ReflectiveOperationException {
                Method getInstance = ttsClass.getDeclaredMethod("getInstance");
                TextToSpeechImplementation newTTS = null;
                if (getInstance != null) {
                    newTTS = (TextToSpeechImplementation) getInstance.invoke(this);
                } else {
                    newTTS = (TextToSpeechImplementation) ttsClass.newInstance();
                }

                addSDK(newTTS, delegateThread);
            }
        };

        try {
            delegateThread.run(delegate);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ScriptInterruptedException(e);
        }
    }

    private void addSDK(TextToSpeechImplementation ttsImpl, DelegateExecutor executor) {
        Map<String, Voice> newVoices = new LinkedHashMap<>();
        for (Voice voice : ttsImpl.getVoices()) {
            if (!isBlackListed(voice)) {
                newVoices.put(voice.guid, voice);
            }
        }

        if (!newVoices.isEmpty()) {
            voices.putAll(newVoices);
            ttsSDKs.put(ttsImpl.sdkName(), ttsImpl);
            ttsExecutors.put(ttsImpl.sdkName(), executor);
        } else {
            ttsImpl.dispose();
        }
    }

    private static DelegateExecutor newDelegateExecutor() {
        return new DelegateExecutor("Speech Synthesis dispatcher thread");
    }

    /**
     * @return Whether TextToSpeech is ready to render speech
     */
    public boolean isReady() {
        return !ttsSDKs.isEmpty();
    }

    public Map<String, Voice> getVoices() {
        return voices;
    }

    /**
     * @param voices
     */
    private static boolean isBlackListed(Voice voice) {
        return BlackList.contains(voice.guid);
    }

    public void speak(Voice voice, String prompt) throws InterruptedException {
        speak(voice, prompt, new String[] {});
    }

    /**
     * @param voice
     * @param prompt
     * @param hints
     *            Mood or other hints that might affect the prompt
     * @throws InterruptedException
     */
    public void speak(Voice voice, String prompt, String[] hints) throws InterruptedException {
        TextToSpeechImplementation tts = voice.ttsImpl;

        if (tts != null && ttsSDKs.containsKey(tts.sdkName())) {
            Delegate delegate = new Delegate() {
                @Override
                public void run() {
                    synchronized (AudioOutput) {
                        try {
                            tts.setVoice(voice);
                            tts.setHints(hints);
                            tts.speak(prompt);
                        } finally {
                            tts.setHints(NoHints);
                        }
                    }
                }
            };

            run(tts, delegate);
        } else {
            ttsEngineNotInitialized();
        }
    }

    public String speak(Voice voice, String prompt, File file) throws InterruptedException {
        return speak(voice, prompt, file, new String[] {});
    }

    public String speak(Voice voice, String prompt, File file, String[] hints) throws InterruptedException {
        StringBuilder soundFilePath = new StringBuilder();
        TextToSpeechImplementation tts = voice.ttsImpl;

        if (tts != null && ttsSDKs.containsKey(tts.sdkName())) {
            Delegate delegate = new Delegate() {
                @Override
                public void run() {
                    try {
                        tts.setVoice(voice);
                        tts.setHints(hints);
                        String actualPath = tts.speak(prompt, file.getAbsolutePath());
                        soundFilePath.append(actualPath);
                    } finally {
                        tts.setHints(NoHints);
                    }
                }
            };

            run(tts, delegate);
        } else {
            ttsEngineNotInitialized();
        }

        return soundFilePath.toString();
    }

    private void run(TextToSpeechImplementation tts, Delegate delegate) throws InterruptedException {
        try {
            DelegateExecutor delegateExecutor = ttsExecutors.get(tts.sdkName());
            delegateExecutor.run(delegate);
        } catch (InterruptedException e) {
            throw e;
        } catch (Exception e) {
            throw ExceptionUtil.asRuntimeException(ExceptionUtil.reduce(e));
        }
    }

    public void stop(Voice voice) {
        TextToSpeechImplementation tts = voice.ttsImpl;

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
        for (Entry<String, TextToSpeechImplementation> ttsSDK : ttsSDKs.entrySet()) {
            ttsSDK.getValue().setPhoneticDictionary(pronunciationDictionary);
        }
    }

}
