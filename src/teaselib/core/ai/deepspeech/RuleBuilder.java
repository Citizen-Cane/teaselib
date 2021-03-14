package teaselib.core.ai.deepspeech;

import static java.util.Collections.*;
import static teaselib.core.speechrecognition.Confidence.*;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import teaselib.core.ai.deepspeech.DeepSpeechRecognizer.Result;
import teaselib.core.speechrecognition.Rule;

public class RuleBuilder {

    private RuleBuilder() {
    }

    public static List<Rule> rules(List<String[]> phrases, List<Result> results) {

        Map<String, Rule> rules = new LinkedHashMap<>();
        // try each alternate, without match search less probable alternates
        for (Result result : results) {
            for (int phraseIndex = 0; phraseIndex < phrases.size(); phraseIndex++) {
                String[] words = phrases.get(phraseIndex);
                int wordIndex = 0;
                int resultIndex = 0;
                int childIndex = 0;
                int insertNullRules = 0;
                List<Rule> children = new ArrayList<>(result.words.size());
                while (wordIndex < words.length) {
                    int j = resultIndex - 1;
                    while (++j < words.length) {
                        String word = words[wordIndex];
                        int k = 0;
                        float probability = 0.0f;
                        for (; k < 2 && resultIndex + k < words.length; k++) {
                            probability = match(word, resultIndex + k, results);
                            if (probability > 0.0f) {
                                break;
                            }
                        }

                        if (probability > 0.0f) {
                            if (insertNullRules > 0) {
                                children.add(nullRule(null, childIndex, phraseIndex, 0.0f));
                                insertNullRules = 0;
                            }
                            children.add(childRule(word, childIndex, childIndex + 1, phraseIndex, probability));
                            childIndex++;
                            resultIndex = j + k + 1;
                            break;
                        } else {
                            // no match in next word - extra garbage word
                            // - child index (fromElement, toElement) does not move
                            // because a null rule does not appear in the parent rule's text
                            insertNullRules++;
                            break;
                        }
                    }
                    wordIndex++;
                }

                if (insertNullRules > 0) {
                    children.add(nullRule(null, childIndex, phraseIndex, 0.0f));
                }

                if (!children.isEmpty()) {
                    Rule rule = Rule.mainRule(children);
                    if (rule.probability > 0.0f && rules.computeIfPresent(rule.text,
                            (t, r) -> r.probability > rule.probability ? r : rule) == null) {
                        rules.put(rule.text, rule);
                    }
                }
            }
        }
        return new ArrayList<>(rules.values());

    }

    private static float match(String word, int index, List<Result> results) {
        for (int i = 0; i < results.size(); i++) {
            Result result = results.get(i);
            if (index < result.words.size()) {
                String hypothesis = result.words.get(index);
                if (word.equals(hypothesis)) {
                    return result.confidence;
                } else {
                    float confidence = partialMatch(hypothesis, word);
                    if (confidence > 0.0f) {
                        return confidence;
                    } else {
                        return alternateMatch(word, index, results.subList(i + 1, results.size()));
                    }
                }
            }
        }
        return 0.0f;
    }

    private static float alternateMatch(String word, int index, List<Result> results) {
        for (Result result : results) {
            List<String> words = result.words;
            if (index < words.size()) {
                String hypothesis = words.get(index);
                if (word.equals(hypothesis)) {
                    return result.confidence;
                } else {
                    float confidence = partialMatch(hypothesis, word);
                    if (confidence > 0.0f) {
                        return confidence;
                    }
                }
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
