package teaselib.core.ai.deepspeech;

import static java.util.Collections.singleton;
import static teaselib.core.speechrecognition.Confidence.valueOf;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import teaselib.core.ai.deepspeech.DeepSpeechRecognizer.Result;
import teaselib.core.speechrecognition.Rule;
import teaselib.core.speechrecognition.srgs.PhraseString;

public class RuleBuilder {

    public static List<Rule> rules(List<PhraseString> phrases, List<Result> results) {
        Map<String, Rule> rules = new LinkedHashMap<>();
        // try each alternate, without match search less probable alternates
        for (int i = 0; i < results.size(); i++) {
            Result result = results.get(i);
            for (int phrase_index = 0; phrase_index < phrases.size(); phrase_index++) {
                PhraseString phrase = phrases.get(phrase_index);
                List<String> words = phrase.split();
                int word_index = 0;
                int result_index = 0;
                int child_index = 0;
                List<Rule> children = new ArrayList<>(result.words.size());
                while (word_index < words.size()) {
                    int j = result_index - 1;
                    int result_words = result.words.size();
                    int null_rules = 0;
                    while (++j < result_words) {
                        String word = words.get(word_index);
                        float probability = match(word, result_index, results);
                        if (probability > 0.0f) {
                            children.add(childRule(word, child_index, child_index + 1, phrase_index, probability));
                            result_index = j + 1;
                            child_index++;
                            break;
                        } else {
                            null_rules++;
                        }
                    }

                    if (j == result_words) {
                        children.add(nullRule(null, child_index, phrase_index, 0.0f));
                    } else if (null_rules > 0) {
                        for (int k = 0; k < null_rules; k++) {
                            children.add(nullRule(null, child_index, phrase_index, 0.0f));
                        }
                    }

                    word_index++;
                }

                // TODO reduce object creation: defer rule building until all probabilities are complete

                Rule rule = Rule.mainRule(children);
                if (rule.probability > 0.0f && rules.computeIfPresent(rule.text, (t, r) -> {
                    return r.probability > rule.probability ? r : rule;
                }) == null) {
                    rules.put(rule.text, rule);
                }
            }
        }
        return new ArrayList<>(rules.values());

    }

    private static float match(String word, int index, List<Result> results) {
        Result result = results.get(0);
        // TODO find exact match with less confidence
        // TODO find partial match with matching start/end letters
        return word.equals(result.words.get(index)) ? result.confidence : 0.0f;
    }

    private static Rule childRule(String text, int from, int to, int choice, float probability) {
        int index = from;
        return new Rule("r_" + index, text, index, singleton(choice), from, to, probability, valueOf(probability));
    }

    private static Rule nullRule(String word, int from, int choice, float probability) {
        int index = from;
        return new Rule("r_" + index, word, index, singleton(choice), index, index, probability, valueOf(probability));
    }

}
