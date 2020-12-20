package teaselib.core.ai.deepspeech;

import java.util.ArrayList;
import java.util.List;

import teaselib.core.ai.deepspeech.DeepSpeechRecognizer.Result;
import teaselib.core.speechrecognition.Confidence;
import teaselib.core.speechrecognition.Rule;
import teaselib.core.speechrecognition.srgs.PhraseString;

public class RuleBuilder {

    // recognition results - for each element:
    // - different word "expert" in "experience prooves this"
    // +> DONE iterate all results where size() == words(ground truth) to find correct match
    // +> DONE iterate all results but just look for the index
    // - for results with larger word size try to match variations with n-words less
    // +> rate first/last letters "exper" confidence = 5/10
    //
    // - missing word: " prooves" in "experience prooves this" / (no) of course (not)
    // -> NULL rule with confidence = 0.0f, empty index - the word should be there
    //
    // - extra word before "the experience" in "experience prooves this"
    // -> ignore

    // TODO works good on disjunct test data, but cannot construct multiple indices
    // -> integrate slices

    public static List<Rule> rules(List<PhraseString> phrases, List<Result> results) {
        List<Rule> rules = new ArrayList<>(phrases.size());
        for (PhraseString phrase : phrases) {
            int index = 0;
            List<PhraseString> words = phrase.words();
            List<Rule> children = new ArrayList<>(words.size());
            for (PhraseString word : words) {
                float probability = find(word, results, index);
                children.add(new Rule("TODO Childname", word.toString(), -2, word.indices, index, index + 1,
                        probability, Confidence.valueOf(probability)));
                index++;
            }
            float probability = Rule.probability(children);
            if (probability > 0) {
                Rule candidate = new Rule(Rule.MAIN_RULE_NAME, phrase.toString(), -1, children, 0, children.size(),
                        probability, Confidence.valueOf(probability));
                rules.add(candidate);
            }
        }
        return rules;
    }

    private static float find(PhraseString word, List<Result> results, int index) {
        for (Result result : results) {
            if (result.words.size() > index && result.words.get(index).equals(word.toString())) {
                return result.confidence;
            }
        }
        return 0.0f;
    }

}
