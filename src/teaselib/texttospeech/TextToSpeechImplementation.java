package teaselib.texttospeech;

import java.util.Map;

import teaselib.TeaseLib;

public abstract class TextToSpeechImplementation {
	
//	public Delegates speechFinished;

	public abstract void getVoices(Map<String, Voice> voices);

	public abstract void init(Voice voice);

	public abstract void speak(String prompt);
	public abstract void speak(String prompt, String wav);
	
	public abstract void dispose();

	@Override
	protected void finalize() throws Throwable {
		try {
			dispose();
		} catch (Throwable t) {
			TeaseLib.log(this, t);
		}
		super.finalize();
	}
}
