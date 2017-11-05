package teaselib.core.texttospeech.implementation.loquendo;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

import org.bridj.BridJ;
import org.bridj.Pointer;
import org.bridj.Pointer.StringType;

import teaselib.core.texttospeech.TextToSpeechImplementation;
import teaselib.core.texttospeech.Voice;
import teaselib.core.texttospeech.implementation.loquendo.LoquendoTTSLibrary.ttsQueryType;

public class LoquendoTTS extends TextToSpeechImplementation {
    private static LoquendoTTS instance = null;

    public static synchronized LoquendoTTS getInstance() throws IOException {
        if (instance == null) {
            System.load("C:/Program Files/Loquendo/LTTS7/bin/LoqTTS7.dll");
            BridJ.getNativeLibrary("LoquendoTTS", new File("C:/Program Files/Loquendo/LTTS7/bin/LoqTTS7.dll"));
            instance = new LoquendoTTS();
        }
        return instance;
    }

    private final List<Voice> voices;

    Pointer<?> hSession;
    Pointer<?> hReader;

    public LoquendoTTS() {
        Pointer<Pointer<?>> phSession = Pointer.allocatePointer();
        checkResult(LoquendoTTSLibrary.ttsNewSession(phSession, null));
        hSession = phSession.get();

        Pointer<Pointer<?>> phReader = Pointer.allocatePointer();
        checkResult(LoquendoTTSLibrary.ttsNewReader(phReader, hSession));
        hReader = phReader.get();

        voices = enumerateVoices();
    }

    /**
     * @param ttsNewSession
     */
    static void checkResult(int result) {
        if (result != LoquendoTTSLibrary.tts_OK) {
            throw new RuntimeException(LoquendoTTS.class.getSimpleName() + ": error" + Integer.toString(result));
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

    }

    @Override
    public void speak(String prompt) {
        Pointer<?> pointerToString = Pointer.pointerToString(prompt, StringType.C, Charset.defaultCharset());

        LoquendoTTSLibrary.ttsRead(hReader, pointerToString, (byte) LoquendoTTSLibrary.ttsTRUE,
                (byte) LoquendoTTSLibrary.ttsFALSE, null);
    }

    @Override
    public String speak(String prompt, String wav) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void stop() {
        LoquendoTTSLibrary.ttsStop(hReader);
    }

    @Override
    public void dispose() {
    }

    @Override
    public String sdkName() {
        return "loquendo";
    }

    @Override
    public void addLexiconEntry(String locale, String word, int partOfSpeech, String pronunciation) {
        // TODO Auto-generated method stub
    }

    @Override
    public String phonemeAlphabetName() {
        return IPA;
    }

}
