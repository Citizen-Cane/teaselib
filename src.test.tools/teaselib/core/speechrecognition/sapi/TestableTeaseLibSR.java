package teaselib.core.speechrecognition.sapi;

import static java.util.stream.Collectors.toList;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

import teaselib.core.speechrecognition.PreparedChoices;
import teaselib.core.speechrecognition.Rule;
import teaselib.core.speechrecognition.SpeechRecognitionInputMethod;
import teaselib.core.speechrecognition.srgs.PhraseString;
import teaselib.core.ui.Choices;

public class TestableTeaseLibSR extends TeaseLibSRGS.Relaxed {

    private static final float PHRASE_PROBABILITY_REDUCED_FOR_FIRST_WORD = 0.18f;
    private static final float PHRASE_PROBABILITY = 0.49f;
    private static final float PHRASE_PROBABILITY_REDUCED_AFTER_SOME_WORDS = 0.25f;

    public TestableTeaseLibSR(Locale locale) {
        super(locale);
    }

    @Override
    public PreparedChoices prepare(Choices choices) {
        return super.prepare(choices);
    }

    @Override
    public List<Rule> repair(List<Rule> result) {
        return super.repair(result).stream().map(this::convertEmulatedToRealworldConfidence).collect(Collectors.toList());
    }

    private Rule convertEmulatedToRealworldConfidence(Rule rule) {
        float probabilityFirstWord;
        Set<Integer> result = SpeechRecognitionInputMethod.choices(
                rule.hasTrailingNullRule() ? rule.withoutDisjunctTrailingNullRules(preparedChoices.mapper) : rule,
                preparedChoices.mapper);
        if (result.size() == 1) {
            String[] detectedSpeech = PhraseString.words(rule.text);
            // Probability value rules derived from various session logs
            if (detectedSpeech.length == 1) {
                // Single word
                probabilityFirstWord = PHRASE_PROBABILITY_REDUCED_FOR_FIRST_WORD;
            } else if (rule.children.size() == 1) {
                // multiple words in a single child rule
                probabilityFirstWord = PHRASE_PROBABILITY;
            } else {
                int firstChildLength = rule.children.stream().map(child -> child.text).filter(text -> text != null)
                        .map(PhraseString::words).map(words -> words.length).findFirst().orElse(0);
                if (firstChildLength == 1) {
                    // multiple words, multiple child rules, first child rule contains a single word
                    probabilityFirstWord = PHRASE_PROBABILITY_REDUCED_FOR_FIRST_WORD;
                } else {
                    // multiple words in a single child rule
                    probabilityFirstWord = PHRASE_PROBABILITY;
                }
            }
        } else {
            // Multiple results
            probabilityFirstWord = PHRASE_PROBABILITY_REDUCED_FOR_FIRST_WORD;
        }

        float probabilityLastWord;
        if (rule.children.size() > 4) {
            probabilityLastWord = PHRASE_PROBABILITY_REDUCED_AFTER_SOME_WORDS;
        } else {
            probabilityLastWord = PHRASE_PROBABILITY;
        }

        return correctedTestRule(rule, probabilityFirstWord, PHRASE_PROBABILITY, probabilityLastWord);
    }

    Rule correctedTestRule(Rule rule, float probabilityFirstWord, float probability, float probabilityLastWord) {
        int size = rule.children.size();
        List<Rule> children = new ArrayList<>(size);

        // process leading NULL rules before applying probability to first word
        for (Rule child : rule.children) {
            if (child.text == null) {
                children.add(child);
            } else {
                break;
            }
        }

        children.add(new Rule(rule.children.get(children.size()), probabilityFirstWord));
        if (size > children.size()) {
            if (size > children.size() + 1) {
                children.addAll(rule.children.subList(children.size(), size - 1).stream()
                        .map(r -> new Rule(r, probability)).collect(toList()));
            }
            children.add(new Rule(rule.children.get(size - 1), probabilityLastWord));
        }
        return new Rule("Emulated", children);
    }
}