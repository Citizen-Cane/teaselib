package teaselib.texttospeech;

import teaselib.text.Message;

public interface ScriptScanner extends Iterable<Message> {

	public String getScriptName();
}
