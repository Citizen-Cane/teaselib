package teaselib.core.texttospeech;

import teaselib.Message;

public interface ScriptScanner extends Iterable<Message> {

	public String getScriptName();
}
