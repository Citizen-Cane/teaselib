package teaselib.core.texttospeech.implementation.loquendo;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

import org.bridj.Pointer;
import org.bridj.Pointer.StringType;

import teaselib.core.texttospeech.TextToSpeechImplementation;
import teaselib.core.texttospeech.Voice;
import teaselib.core.texttospeech.implementation.loquendo.LoquendoTTSLibrary.ttsHandleType;
import teaselib.core.texttospeech.implementation.loquendo.LoquendoTTSLibrary.tts_API_DEFINITION;

public class LoquendoTTS extends TextToSpeechImplementation {
    private final List<Voice> voices;

    ttsHandleType hSession;
    ttsHandleType hReader;

    public LoquendoTTS() {
        // TODO tts_API_DEFINITION must be ttsResultType - see hello example -> custom jaenerator script
        // Include other include files to get the corret types
        Pointer<ttsHandleType> phSession = Pointer.allocate(ttsHandleType.class);
        tts_API_DEFINITION ttsNewSession = LoquendoTTSLibrary.ttsNewSession(phSession, null);
        hSession = phSession.get();

        Pointer<ttsHandleType> phReader = Pointer.allocate(ttsHandleType.class);
        tts_API_DEFINITION ttsNewReader = LoquendoTTSLibrary.ttsNewReader(phReader, phSession.get());
        hReader = phReader.get();

        voices = enumerateVoices();
    }

    private List<Voice> enumerateVoices() {
        List<Voice> voices = new ArrayList<>();

        return voices;
    }

    @Override
    public List<Voice> getVoices() {
        return voices;
    }

    @Override
    public void setVoice(Voice voice) {
        LoquendoTTSLibrary.ttsSetVoice(hReader, ((LoquendoVoice) voice).hVoice);

    }

    @Override
    public void speak(String prompt) {
        // LoquendoTTSLibrary.ttsRead(hReader, Pointer.pointerToString(prompt, StringType.C, Charset.defaultCharset()),
        // true, false, 0);

        // TODO define Loquendo ttsBoolType as non-empty interface -> values true==1/false==0
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
