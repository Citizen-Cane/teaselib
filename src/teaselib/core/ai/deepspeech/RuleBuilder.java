package teaselib.core.ai.deepspeech;

import static java.util.Collections.singleton;
import static teaselib.core.speechrecognition.Confidence.valueOf;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import teaselib.core.ai.deepspeech.DeepSpeechRecognizer.Result;
import teaselib.core.speechrecognition.Confidence;
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
                        } else if (result_index + 1 < words.size()) {
                            // next word?
                            probability = match(word, result_index + 1, results);
                            if (probability > 0.0f) {
                                children.add(childRule(word, child_index, child_index + 1, phrase_index, probability));
                                result_index = j + 2;
                                child_index++;
                                break;
                            } else {
                                // no match in next word - extra garbage word
                                null_rules++;
                            }
                        } else {
                            // no match - garbage word at the end of the phrase
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

                // TODO decide whether to include trailing null rules in speech detection hypothesis
                // - yes -> models teaselib srgs probability correction for small hypotheses
                // - no -> models sapi hypothesis probability

                Rule rule = Rule.mainRule(children);
                if (rule.probability > 0.0f && rules.computeIfPresent(rule.text,
                        (t, r) -> r.probability > rule.probability ? r : rule) == null) {
                    rules.put(rule.text, rule);
                }
            }
        }
        return new ArrayList<>(rules.values());

    }

    private static float match(String word, int index, List<Result> results) {
        Result result = results.get(0);
        if (index < result.words.size()) {
            String hypothesis = result.words.get(index);
            if (word.equals(hypothesis)) {
                return result.confidence;
            } else {
                float alternateMatch = alternateMatch(word, index, results);
                // TODO compare with choice intention derived probability
                if (alternateMatch >= Confidence.High.probability) {
                    return alternateMatch;
                } else {
                    float partialMatch = partialMatch(hypothesis, word);
                    return Math.max(alternateMatch, partialMatch);
                }
            }
        } else {
            return 0.0f;
        }
    }

    private static float alternateMatch(String word, int index, List<Result> results) {
        for (int i = 1; i < results.size(); i++) {
            Result result = results.get(i);
            List<String> words = result.words;
            if (index < words.size() && word.equals(words.get(index))) {
                return result.confidence;
            }
        }
        return 0.0f;
    }

    private static float partialMatch(String hypothesis, String word) {
        // better (but also more expensive) implementations:
        // - string approximation (interesting but complex)
        // - pronunciation comparison - what we want, but locale-dependent
        // See https://en.wikipedia.org/wiki/Approximate_string_matching
        int matches = 0;
        int i = 0;
        int length = Math.min(hypothesis.length(), word.length());
        while (i < length && hypothesis.charAt(i) == word.charAt(i)) {
            matches++;
            i++;
        }

        int j = word.length() - 1;
        int k = hypothesis.length() - 1;
        while (k >= i && j >= i && hypothesis.charAt(k) == word.charAt(j)) {
            matches++;
            j--;
            k--;
        }

        return (float) matches / word.length();
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
