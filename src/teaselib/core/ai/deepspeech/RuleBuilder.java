package teaselib.core.ai.deepspeech;

import static java.util.Collections.singleton;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import teaselib.core.ai.deepspeech.DeepSpeechRecognizer.Result;
import teaselib.core.ai.deepspeech.RuleBuilder.Rating.Rated;
import teaselib.core.speechrecognition.Rule;

public class RuleBuilder {

    private RuleBuilder() {
    }

    interface Rating {
        Rated get(String word, Integer choice);

        class Rated {
            final float probability;
            final Runnable builder;

            public Rated(float probability, Runnable builder) {
                super();
                this.probability = probability;
                this.builder = builder;
            }

        }
    }

    static class Matcher {
        final List<String[]> phrases;
        final List<Result> results;
        final List<MatchStrategy> matchStrategies;

        String[] phrase;
        int wordIndex;
        int resultIndex;
        int childIndex;
        int insertNullRules;
        List<Rule> children;

        public Matcher(List<String[]> phrases, List<Result> results) {
            this.phrases = phrases;
            this.results = results;
            matchStrategies = createStrategies();
        }

        class MatchStrategy {
            final Rating rating;

            public MatchStrategy(Rating wordRater) {
                this.rating = wordRater;
            }
        }

        private List<MatchStrategy> createStrategies() {

            var thisWord = new MatchStrategy((word, choice) -> {
                float probability = match(word, resultIndex);
                return new Rated(probability, () -> {
                    addChild("thisWord", word, choice, probability);
                    resultIndex += 1;
                });
            });

            // recognized word -> two expected words
            var joinedWord = new MatchStrategy((word, choice) -> {
                List<String> result = results.get(0).words;
                if (resultIndex < result.size() - 1 && wordIndex < phrase.length - 1) {
                    String recognized = result.get(resultIndex);
                    String nextWord = phrase[wordIndex + 1];

                    float probability = partialMatch(word + nextWord, recognized);
                    if (probability > 0.0f) {
                        float a = recognized.startsWith(word.substring(0, 1)) //
                                ? partialMatch(word, recognized)
                                : 0.0f;
                        float b = recognized.endsWith(nextWord.substring(nextWord.length() - 1)) //
                                ? partialMatch(nextWord, recognized)
                                : 0.0f;
                        if (a > 0.0f && b > 0.0f) {
                            return new Rated(probability, () -> {
                                if (a > 0.0f)
                                    addChild("Join word " + "a", word, choice, a);
                                if (b > 0.0f)
                                    addChild("Join word " + "b", nextWord, choice, b);
                                resultIndex += 1;
                                wordIndex += 1;
                            });
                        } else {
                            return null;
                        }
                    } else {
                        return null;
                    }
                } else {
                    return null;
                }
            });

            // recognized words -> ome expected word
            var splitWord = new MatchStrategy((word, choice) -> {
                List<String> result = results.get(0).words;
                if (resultIndex < result.size() - 1) {
                    String recognized = result.get(resultIndex);
                    String nextRecognized = result.get(resultIndex + 1);

                    float probability = partialMatch(recognized + nextRecognized, word);
                    if (probability > 0.0f) {
                        return new Rated(probability, () -> {
                            addChild("splitWord", word, choice, probability);
                            resultIndex += 1;
                        });
                    } else {
                        return null;
                    }
                } else {
                    return null;
                }
            });

            var nextWord = new MatchStrategy((word, choice) -> {
                float probability = match(word, resultIndex + 1);
                return new Rated(probability, () -> {
                    addChild("nextWord", word, choice, probability);
                    resultIndex += 2;
                });
            });

            return Arrays.asList(thisWord, nextWord, joinedWord, splitWord);
        }

        void match(int phraseIndex) {
            phrase = phrases.get(phraseIndex);
            wordIndex = 0;
            resultIndex = 0;
            childIndex = 0;
            insertNullRules = 0;
            children = new ArrayList<>();

            while (wordIndex < phrase.length) {
                String word = phrase[wordIndex];
                var ratedWord = rate(word, phraseIndex);
                if (ratedWord != null) {
                    ratedWord.builder.run();
                } else {
                    insertNullRules++;
                }
                wordIndex++;
            }

            if (resultIndex < results.get(0).words.size() || insertNullRules > 0) {
                int n = Math.max(results.get(0).words.size() - wordIndex, insertNullRules);
                addPlaceholders(n, phraseIndex);
            }
        }

        private Rated rate(String word, int phraseIndex) {
            Rated ratedWord = null;
            for (MatchStrategy matchStrategy : matchStrategies) {
                Rated r = matchStrategy.rating.get(word, phraseIndex);
                if ((ratedWord == null && r != null && r.probability > 0.0f)
                        || (ratedWord != null && r != null && r.probability > ratedWord.probability)) {
                    ratedWord = r;
                    if (ratedWord != null && ratedWord.probability >= 1.0) {
                        break;
                    }
                }
            }
            return ratedWord;
        }

        private float match(String word, int index) {
            return match(word, index, 0, 0.0f);
        }

        private float match(String word, int index, int startResult, float currentConfidence) {
            for (int i = startResult; i < results.size(); i++) {
                var result = results.get(i);
                if (index < result.words.size()) {
                    String hypothesis = result.words.get(index);
                    if (word.equals(hypothesis) && result.confidence >= currentConfidence) {
                        return result.confidence;
                    } else {
                        float confidence = partialMatch(hypothesis, word) * result.confidence;
                        return Math.max(currentConfidence, match(word, index, startResult + 1, confidence));
                    }
                }
            }
            return currentConfidence;
        }

        private static float partialMatch(String hypothesis, String word) {
            // better (but also more expensive) implementations:
            // - string approximation (interesting but complex)
            // - pronunciation comparison - what we want, but locale-dependent
            // See https://en.wikipedia.org/wiki/Approximate_string_matching
            int hypothesis_length = hypothesis.length();
            int word_length = word.length();
            int matches = 0;
            int i = 0;
            int length = Math.min(hypothesis_length, word_length);
            while (i < length && hypothesis.charAt(i) == word.charAt(i)) {
                matches++;
                i++;
            }

            int j = word_length - 1;
            int k = hypothesis_length - 1;
            while (k >= i && j >= i && hypothesis.charAt(k) == word.charAt(j)) {
                matches++;
                j--;
                k--;
            }

            if (matches < hypothesis_length && word.contains(hypothesis)) {
                matches = hypothesis_length;
            }

            if (hypothesis_length < word_length) {
                return (float) matches / word_length;
            } else {
                return (float) matches / hypothesis_length;
            }
        }

        private void addChild(String name, String word, int phraseIndex, float probability) {
            if (insertNullRules > 0) {
                addPlaceholders(insertNullRules, phraseIndex);
                insertNullRules = 0;
            }
            children.add(childRule(name, word, phraseIndex, probability));
            childIndex++;
        }

        private Rule childRule(String name, String text, int choice, float probability) {
            return childRule(name, text, wordIndex, childIndex, childIndex + 1, choice, probability);
        }

        private static Rule childRule(String name, String text, int index, int from, int to, int choice,
                float probability) {
            Set<Integer> choices = singleton(choice);
            return new Rule(Rule.name(Rule.CHOICE_NODE_NAME, index, choices) + "_" + name, text, index, choices, from,
                    to, probability);
        }

        private void addPlaceholders(int n, int phraseIndex) {
            for (int i = 0; i < n; i++) {
                addPlaceholder(phraseIndex);
            }
        }

        private void addPlaceholder(int phraseIndex) {
            children.add(Rule.placeholder(wordIndex, childIndex, Collections.singleton(phraseIndex), 0.0f));
        }

        Rule rule() {
            return Rule.mainRule(children);
        }

    }

    public static List<Rule> rules(List<String[]> phrases, List<Result> results) {
        Map<String, Rule> rules = new LinkedHashMap<>();
        var matcher = new Matcher(phrases, results);
        for (int phraseIndex = 0; phraseIndex < phrases.size(); phraseIndex++) {
            matcher.match(phraseIndex);
            if (!matcher.children.isEmpty()) {
                var rule = matcher.rule();
                if (rule.probability > 0.0f && rules.computeIfPresent(rule.text,
                        (t, r) -> r.probability > rule.probability ? r : rule) == null) {
                    rules.put(rule.text, rule);
                }
            }
        }
        return new ArrayList<>(rules.values());
    }

}
