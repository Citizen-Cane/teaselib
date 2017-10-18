package teaselib.core.texttospeech;

public class VoiceInfo {
    public String vendor;
    public String language;
    public String name;

    public VoiceInfo(String vendor, String language, String name) {
        this.vendor = vendor;
        this.language = language;
        this.name = name;
    }
}