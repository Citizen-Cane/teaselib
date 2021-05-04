package teaselib.core.texttospeech.implementation.loquendo;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import org.bridj.BridJ;
import org.bridj.Pointer;
import org.bridj.Pointer.StringType;

import teaselib.Mood;
import teaselib.core.texttospeech.TextToSpeechImplementation;
import teaselib.core.texttospeech.Voice;
import teaselib.core.texttospeech.implementation.Unsupported;
import teaselib.core.texttospeech.implementation.loquendo.LoquendoTTSLibrary.ttsQueryType;
import teaselib.core.util.Environment;

public class LoquendoTTS extends TextToSpeechImplementation {
    private static final byte ASYNCHRONOUS = (byte) LoquendoTTSLibrary.ttsFALSE;
    private static final byte SYNCHRONOUS = (byte) LoquendoTTSLibrary.ttsTRUE;

    public static TextToSpeechImplementation newInstance() throws IOException {
        if (Environment.SYSTEM == Environment.Windows) {
            File library = new File(System.getenv("ProgramFiles"), "Loquendo/LTTS7/bin/LoqTTS7.dll");
            if (library.exists()) {
                BridJ.getNativeLibrary("LoquendoTTS", library);
                return new LoquendoTTS();
            } else {
                return Unsupported.Instance;
            }
        } else {
            return Unsupported.Instance;
        }
    }

    private final List<Voice> voices;

    final Pointer<?> hSession;
    final Pointer<?> hReader;

    private final AtomicBoolean cancelSpeech = new AtomicBoolean(false);

    private LoquendoTTS() {
        Pointer<Pointer<?>> phSession = Pointer.allocatePointer();
        checkResult(LoquendoTTSLibrary.ttsNewSession(phSession, null));
        hSession = phSession.get();

        Pointer<Pointer<?>> phReader = Pointer.allocatePointer();
        checkResult(LoquendoTTSLibrary.ttsNewReader(phReader, hSession));
        hReader = phReader.get();

        speakToAudioBoard();

        checkResult(
                LoquendoTTSLibrary.ttsSetTextFormat(hReader, LoquendoTTSLibrary.ttsTextFormatType.TTSAUTODETECTFORMAT));

        voices = enumerateVoices();
    }

    /**
     * @param ttsNewSession
     */
    static void checkResult(int errNo) {
        if (errNo != LoquendoTTSLibrary.tts_OK) {
            Pointer<Byte> message = LoquendoTTSLibrary.ttsGetErrorMessage(errNo);
            throw new LoquendoTTSException("Error #" + Integer.toString(errNo) + ": " + message.getCString());
        }
    }

    private List<Voice> enumerateVoices() {
        List<Voice> list = new ArrayList<>();

        Pointer<Pointer<Byte>> id = Pointer.allocatePointer(Byte.class);
        Pointer<Pointer<?>> phEnum = Pointer.allocatePointer();

        int r = LoquendoTTSLibrary.ttsEnumFirst(phEnum, hSession, LoquendoTTSLibrary.ttsQueryType.TTSOBJECTVOICE, null,
                id);
        if (r == LoquendoTTSLibrary.tts_OK && id.get() != null)
            do {
                String guid = id.get().getString(StringType.C);

                Pointer<Pointer<?>> phVoice = Pointer.allocatePointer();
                checkResult(LoquendoTTSLibrary.ttsNewVoice(phVoice, hSession, Pointer.pointerToCString(guid)));

                Pointer<Pointer<?>> phLanguage = Pointer.allocatePointer();
                String motherTongue = queryVoiceAttribute(id.get(), "MotherTongue");
                checkResult(LoquendoTTSLibrary.ttsNewLanguage(phLanguage, hSession,
                        Pointer.pointerToCString(motherTongue)));

                list.add(new LoquendoVoice(this, phVoice.get(), phLanguage.get(), guid));
            } while (LoquendoTTSLibrary.ttsEnumNext(phEnum.get(), id) == LoquendoTTSLibrary.tts_OK && id.get() != null);
        LoquendoTTSLibrary.ttsEnumClose(phEnum.get());

        return list;
    }

    String queryVoiceAttribute(Pointer<Byte> id, String attribute) {
        ttsQueryType queryType = LoquendoTTSLibrary.ttsQueryType.TTSOBJECTVOICE;
        return queryAttribute(queryType, id, attribute);
    }

    String queryLanguageAttribute(Pointer<Byte> id, String attribute) {
        ttsQueryType queryType = LoquendoTTSLibrary.ttsQueryType.TTSOBJECTLANGUAGE;
        return queryAttribute(queryType, id, attribute);
    }

    public String queryAttribute(ttsQueryType queryType, Pointer<Byte> id, String attribute) {
        Pointer<Byte> sAttribute = Pointer.pointerToCString(attribute).as(Byte.class);
        Pointer<Byte> pResultBuffer = Pointer.allocateBytes(LoquendoTTSLibrary.ttsSTRINGMAXLEN);
        checkResult(LoquendoTTSLibrary.ttsQueryAttribute(hSession, queryType, sAttribute, id, pResultBuffer,
                LoquendoTTSLibrary.ttsSTRINGMAXLEN));
        return pResultBuffer.getCString();
    }

    @Override
    public List<Voice> getVoices() {
        return voices;
    }

    @Override
    public void setVoice(Voice voice) {
        checkResult(LoquendoTTSLibrary.ttsSetVoice(hReader, ((LoquendoVoice) voice).hVoice));
        checkResult(LoquendoTTSLibrary.ttsSetLanguage(hReader, ((LoquendoVoice) voice).hLanguage));
    }

    @Override
    public void speak(String prompt) {
        cancelSpeech.set(false);
        applyHintsAndSpeak(prompt, SYNCHRONOUS);
        waitForAsynchronousSpeech();
    }

    private void waitForAsynchronousSpeech() {
        Pointer<Byte> bSignaled = Pointer.allocate(Byte.class);
        bSignaled.set((byte) 0);
        while (bSignaled.getByte() == 0) {
            if (cancelSpeech.get()) {
                checkResult(LoquendoTTSLibrary.ttsStop(hReader));
                break;
            } else {
                checkResult(LoquendoTTSLibrary.ttsWaitForEndOfSpeech(hReader, 100, bSignaled));
            }
        }
    }

    public void speakToAudioBoard() {
        checkResult(LoquendoTTSLibrary.ttsSetAudio(hReader, Pointer.pointerToCString("LTTS7AudioBoard"), null, 32000,
                LoquendoTTSLibrary.ttsAudioEncodingType.tts_LINEAR, LoquendoTTSLibrary.ttsAudioSampleType.tts_MONO,
                null));
    }

    @Override
    public String speak(String prompt, String wav) {
        speakToFile(wav);
        try {
            applyHintsAndSpeak(prompt, ASYNCHRONOUS);
        } finally {
            speakToAudioBoard();
        }
        return wav;
    }

    private void applyHintsAndSpeak(String prompt, byte asynchronous) {
        applyHints();
        checkResult(LoquendoTTSLibrary.ttsRead(hReader, Pointer.pointerToCString(prompt), asynchronous,
                (byte) LoquendoTTSLibrary.ttsFALSE, null));
    }

    public void speakToFile(String wav) {
        checkResult(LoquendoTTSLibrary.ttsSetAudio(hReader, Pointer.pointerToCString("LTTS7AudioFile"),
                Pointer.pointerToCString(wav), 32000, LoquendoTTSLibrary.ttsAudioEncodingType.tts_LINEAR,
                LoquendoTTSLibrary.ttsAudioSampleType.tts_MONO, null));
    }

    private void applyHints() {
        if (new HashSet<Object>(Arrays.asList(getHints())).contains(Mood.Reading)) {
            checkResult(LoquendoTTSLibrary.ttsSetVolume(hReader, 15));
            checkResult(LoquendoTTSLibrary.ttsSetSpeed(hReader, 40));
        } else {
            checkResult(LoquendoTTSLibrary.ttsSetVolume(hReader, 12));
            checkResult(LoquendoTTSLibrary.ttsSetSpeed(hReader, 50));
        }
    }

    @Override
    public void stop() {
        cancelSpeech.set(true);
    }

    @Override
    public void dispose() {
        stop();
        LoquendoTTSLibrary.ttsDeleteReader(hReader);
        LoquendoTTSLibrary.ttsDeleteSession(hSession);
    }

    @Override
    public String sdkName() {
        return "loquendo";
    }

    @Override
    public void addLexiconEntry(String locale, String word, int partOfSpeech, String pronunciation) {
        // Can only load complete dictionaries, plus entries with phonemes distort the prosody
    }

    @Override
    public String phonemeAlphabetName() {
        return IPA;
    }

}
