package teaselib.core.speechrecognition.sapi;

import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.function.IntUnaryOperator;

import teaselib.core.speechrecognition.PreparedChoices;
import teaselib.core.speechrecognition.SpeechRecognitionEvents;
import teaselib.core.speechrecognition.SpeechRecognitionProvider;
import teaselib.core.speechrecognition.srgs.PhraseString;
import teaselib.core.ui.Choice;
import teaselib.core.ui.Choices;

public class TeaseLibSRSimple extends TeaseLibSR {

    public TeaseLibSRSimple(Locale locale, SpeechRecognitionEvents events) {
        super(locale, events);
        // TODO Auto-generated constructor stub
    }

    @Override
    public PreparedChoices prepare(Choices choices) {
        return new PreparedChoices() {
            @Override
            public void accept(SpeechRecognitionProvider sri) {
                if (sri != TeaseLibSRSimple.this)
                    throw new IllegalArgumentException();
                setChoices(firstPhraseOfEach(choices));
            }

            @Override
            public IntUnaryOperator mapper() {
                return IdentityMapping;
            }

        };
    }

    public static List<String> firstPhraseOfEach(Choices choices) {
        return choices.stream().map(TeaseLibSRSimple::firstPhrase).map(TeaseLibSRSimple::withoutPunctation)
                .collect(toList());
    }

    private static String firstPhrase(Choice choice) {
        return choice.phrases.get(0);
    }

    private static String withoutPunctation(String text) {
        return Arrays.stream(PhraseString.words(text)).collect(joining(" "));
    }

}
