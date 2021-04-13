package teaselib.core.speechrecognition.sapi;

import static java.util.stream.Collectors.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.function.IntUnaryOperator;

import teaselib.core.speechrecognition.PreparedChoices;
import teaselib.core.speechrecognition.Rule;
import teaselib.core.speechrecognition.SpeechRecognitionImplementation;
import teaselib.core.speechrecognition.srgs.PhraseString;
import teaselib.core.ui.Choice;
import teaselib.core.ui.Choices;

public class TeaseLibSRSimple extends TeaseLibSR.SAPI {

    List<String> phrases = null;

    public TeaseLibSRSimple(Locale locale) {
        super(locale);
    }

    @Override
    public PreparedChoices prepare(Choices choices) {
        return new PreparedChoices() {
            @Override
            public void accept(SpeechRecognitionImplementation sri) {
                if (sri != TeaseLibSRSimple.this)
                    throw new IllegalArgumentException();
                phrases = firstPhraseOfEach(choices);
                setChoices(phrases);
            }

            @Override
            public float hypothesisWeight(Rule rule) {
                if (rule.indices.equals(Rule.NoIndices))
                    throw new IllegalArgumentException(
                            "Rule contains no indices - consider updating it after adding children");

                Integer index = rule.indices.iterator().next();
                PhraseString text = new PhraseString(rule.text, index);
                List<PhraseString> complete = new PhraseString(choices.get(index).phrases.get(0), index).words();
                return (float) text.words().size() / (float) complete.size();
            }

            @Override
            public IntUnaryOperator mapper() {
                return IdentityMapping;
            }

        };
    }

    @Override
    protected List<Rule> repair(List<Rule> result) {
        List<Rule> repaired = new ArrayList<>(result.size());
        for (Rule rule : result) {
            HashSet<Integer> indices = new HashSet<>();
            for (int i = 0; i < phrases.size(); ++i) {
                if (startsWith(rule, phrases.get(i))) {
                    indices.add(i);
                }
            }
            if (indices.isEmpty()) {
                repaired.add(rule);
            } else {
                repaired.add(new Rule(Rule.REPAIRED_MAIN_RULE_NAME, rule.text, rule.ruleIndex, indices,
                        rule.fromElement, rule.toElement, rule.probability, rule.confidence));
            }
        }
        return repaired;
    }

    private static boolean startsWith(Rule rule, String phrase) {
        return PhraseString.words(rule.text).length < PhraseString.words(phrase).length && phrase.startsWith(rule.text);
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
