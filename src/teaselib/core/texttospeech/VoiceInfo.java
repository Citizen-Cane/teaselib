package teaselib.core.texttospeech;

public class VoiceInfo {
    public final String vendor;
    public final String language;
    public final String name;

    public VoiceInfo(String vendor, String language, String name) {
        this.vendor = vendor;
        this.language = language;
        this.name = name;
    }
}