package teaselib.core.ai.deepspeech;

import static java.util.Collections.*;
import static teaselib.core.speechrecognition.Confidence.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Function;

import teaselib.core.ai.deepspeech.DeepSpeechRecognizer.Result;
import teaselib.core.speechrecognition.Rule;

public class RuleBuilder {

    private RuleBuilder() {
    }

    static class Matcher {
        int wordIndex = 0;
        int resultIndex = 0;
        int childIndex = 0;
        int insertNullRules = 0;

        final List<Rule> children = new ArrayList<>();

        final List<Result> results;
        final String[] words;

        /**
         * @param words
         * @param phraseIndex
         */
        public Matcher(List<Result> results, String[] words, int phraseIndex) {
            this.results = results;
            this.words = words;
            match(phraseIndex);
        }

        class MatchStrategy {
            final Function<String, Float> probability;
            final BiConsumer<String, Float> builder;

            /**
             * @param probability
             * @param resultor
             */
            public MatchStrategy(Function<String, Float> probability, BiConsumer<String, Float> builder) {
                this.probability = probability;
                this.builder = builder;
            }

        }

        void match(int phraseIndex) {
            MatchStrategy thisWord = new MatchStrategy(word -> match(word, resultIndex), (word, probability) -> {
                addChild(word, phraseIndex, probability);
                resultIndex += 1;
            });

            // TODO resolve decision making code duplication
            // TODO replace the words in result(s) -> no special casing
            // TODO detect partial matches for both words
            // TODO rate probability of partial matches
            // TODO implicit alternate matching not covered because additional tokens are generated - don't use
            MatchStrategy splitWord = new MatchStrategy(word -> {
                List<String> result = results.get(0).words;
                if (resultIndex < result.size() - 1 && wordIndex < words.length - 1) {
                    String recognized = result.get(resultIndex);
                    String nextWord = words[wordIndex + 1];
                    if (recognized.startsWith(word)
                            && recognized.endsWith(nextWord.substring(nextWord.length() - 1, 1))) {
                        return 1.0f;
                    } else if (recognized.startsWith(word.substring(0, 1)) && recognized.endsWith(nextWord)) {
                        return 1.0f;
                    } else {
                        return 0.0f;
                    }
                } else {
                    return 0.0f;
                }
            }, (word, probability) -> {
                List<String> result = results.get(0).words;
                String recognized = result.get(resultIndex);
                String nextWord = words[wordIndex + 1];
                if (recognized.startsWith(word) && recognized.endsWith(nextWord.substring(nextWord.length() - 1, 1))) {
                    addChild(word, phraseIndex, 1.0f);
                    addChild(nextWord, phraseIndex, 1.0f / nextWord.length());
                } else if (recognized.startsWith(word.substring(0, 1)) && recognized.endsWith(nextWord)) {
                    addChild(word, phraseIndex, 1.0f / word.length());
                    addChild(nextWord, phraseIndex, 1.0f);
                } else {
                    throw new IllegalStateException();
                }

                resultIndex += 1;
                wordIndex += 1;
            });

            MatchStrategy nextWord = new MatchStrategy(word -> match(word, resultIndex + 1), (word, probability) -> {
                addChild(word, phraseIndex, probability);
                resultIndex += 2;
            });

            List<MatchStrategy> matchStrategies = Arrays.asList(thisWord, splitWord, nextWord);

            while (wordIndex < words.length && resultIndex < words.length) {
                String word = words[wordIndex];

                BiConsumer<String, Float> builder = null;
                float probability = 0.0f;
                for (MatchStrategy matchStrategy : matchStrategies) {
                    float p = matchStrategy.probability.apply(word);
                    if (p > probability) {
                        probability = p;
                        builder = matchStrategy.builder;
                        if (probability >= 1.0) {
                            break;
                        }
                    }
                }

                if (builder != null) {
                    builder.accept(word, probability);
                } else {
                    insertNullRules++;
                }
                wordIndex++;
            }

            if (insertNullRules > 0) {
                children.add(nullRule(null, childIndex, phraseIndex, 0.0f));
            }
        }

        private void addChild(String word, int phraseIndex, float probability) {
            if (insertNullRules > 0) {
                children.add(nullRule(null, childIndex, phraseIndex, 0.0f));
                insertNullRules = 0;
            }
            children.add(childRule(word, childIndex, childIndex + 1, phraseIndex, probability));
            childIndex++;
        }

        private float match(String word, int index) {
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
            return new Rule("r_" + index, word, index, singleton(choice), index, index, probability,
                    valueOf(probability));
        }

        Rule rule() {
            return Rule.mainRule(children);
        }
    }

    public static List<Rule> rules(List<String[]> phrases, List<Result> results) {
        Map<String, Rule> rules = new LinkedHashMap<>();
        for (int phraseIndex = 0; phraseIndex < phrases.size(); phraseIndex++) {
            String[] words = phrases.get(phraseIndex);
            Matcher matcher = new Matcher(results, words, phraseIndex);
            if (!matcher.children.isEmpty()) {
                Rule rule = matcher.rule();
                if (rule.probability > 0.0f && rules.computeIfPresent(rule.text,
                        (t, r) -> r.probability > rule.probability ? r : rule) == null) {
                    rules.put(rule.text, rule);
                }
            }
        }
        return new ArrayList<>(rules.values());
    }

}
