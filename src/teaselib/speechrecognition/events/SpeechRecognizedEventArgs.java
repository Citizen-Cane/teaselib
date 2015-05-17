package teaselib.speechrecognition.events;

import teaselib.speechrecognition.SpeechRecognitionResult;

public class SpeechRecognizedEventArgs extends SpeechRecognitionEventArgs
{
	public final SpeechRecognitionResult result[];

	public SpeechRecognizedEventArgs(SpeechRecognitionResult[] result) {
		this.result = result;
	}

	@Override
	public String toString() {
		StringBuilder results = new StringBuilder();
		if (result != null)
		{
			for(SpeechRecognitionResult r : result)
			{
				if (results.length() > 0)
				{
					results.append(", ");
				}
				results.append(r.toString());
			}
		}
		else
		{
			results.append("<none>");
		}
		return getClass().getSimpleName() + "Result = " + results;
	}
}
