package teaselib.core.speechrecognition.sapi;

import static teaselib.core.util.ExceptionUtil.asRuntimeException;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.function.IntUnaryOperator;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import teaselib.core.speechrecognition.PreparedChoices;
import teaselib.core.speechrecognition.Rule;
import teaselib.core.speechrecognition.SpeechRecognitionEvents;
import teaselib.core.speechrecognition.SpeechRecognitionInputMethod;
import teaselib.core.speechrecognition.SpeechRecognitionProvider;
import teaselib.core.speechrecognition.srgs.PhraseString;
import teaselib.core.speechrecognition.srgs.SRGSPhraseBuilder;
import teaselib.core.speechrecognition.srgs.SlicedPhrases;
import teaselib.core.ui.Choices;

public class TeaseLibSRGS extends TeaseLibSR {

    private final class PreparedChoicesImplementation implements PreparedChoices {
        final Choices choices;
        final SlicedPhrases<PhraseString> slicedPhrases;
        final byte[] srgs;
        final IntUnaryOperator mapper;

        private PreparedChoicesImplementation(Choices choices, SlicedPhrases<PhraseString> slicedPhrases, byte[] srgs,
                IntUnaryOperator mapper) {
            this.choices = choices;
            this.slicedPhrases = slicedPhrases;
            this.srgs = srgs;
            this.mapper = mapper;
        }

        @Override
        public void accept(SpeechRecognitionProvider sri) {
            if (sri != TeaseLibSRGS.this)
                throw new IllegalArgumentException();

            setChoices(srgs);
        }

        @Override
        public Optional<Rule> hypothesis(List<Rule> rules, Rule currentHypothesis) {
            return distinctHypothesis(rules, choices, this, currentHypothesis);
        }

        @Override
        public IntUnaryOperator mapper() {
            return mapper;
        }
    }

    private static final Logger logger = LoggerFactory.getLogger(TeaseLibSRGS.class);

    public TeaseLibSRGS(Locale locale, SpeechRecognitionEvents events) {
        super(locale, events);
    }

    @Override
    public PreparedChoices prepare(Choices choices) {
        try {
            SRGSPhraseBuilder builder = new SRGSPhraseBuilder(choices, languageCode());
            if (logger.isInfoEnabled()) {
                logger.info("{}", builder.slices);
                logger.info("{}", builder.toXML());
            }

            byte[] bytes = builder.toBytes();
            // TODO create op in builder to be able to release the builder instance
            IntUnaryOperator mapper = builder::map;

            return new PreparedChoicesImplementation(choices, builder.slices, bytes, mapper);
        } catch (ParserConfigurationException | TransformerException e) {
            throw asRuntimeException(e);
        }
    }

    private Optional<Rule> distinctHypothesis(List<Rule> rules, Choices choices, PreparedChoices preparedChoices,
            Rule currentHypothesis) {
        SlicedPhrases<PhraseString> slicedPhrases = ((PreparedChoicesImplementation) preparedChoices).slicedPhrases;
        // Caveats:

        // DONE
        // - trailing null rules make rule undefined (distinct [1], common [0,1], distinct NULL [0]
        // -> remove trailing null rules because the available rules already define a distinct result
        // + trailing null rules are at the end of the text (fromElement = size)

        // DONE
        // For single rules or long rules, confidence decreases with length
        // -> forward earlier accepted hypothesis - this is already handled by the existing code
        // + for single confirmations, confidence is normal anyway
        // - longer rules within multiple choices may cause recognition problems
        // -> remember earlier hypothesis

        // DONE
        // words are omitted in the main rule, instead there are NULL child rules
        // Example: "Ja Mit <NULL> dicken Dildo Frau Streng" -> "Ja Mit "DEM" dicken Dildo Frau Streng" ->
        // matches
        // phrase
        // -> for each NULL rule try to reconstruct the phrase by replacing the NULL rule with a valid phrase
        // + if it matches a phrase, use that one (works for speechDetected as well as for
        // recognitionCompleted/Rejected
        // - the phrase should match the beginning of a single phrase - can rate incomplete phrases -> not needed

        // DONE
        // in the logs aborting recognition on audio problems looks suspicious in the log - multiple recognitions?
        // -> only check for audio problems when rejecting speech - should be okay if recognized
        // + make it as optional as possible

        // RESOLVED by confidence function
        // first child rule may have very low confidence value, second very high, how to measure minimal length
        // -> number of distinct rules, words, or vowels -> words or vowels

        // RESOLVED by confidence function
        // - confidence decreases at the end of the hypothesis (distinct [1] = 0.79, common [0,1] = 0.76,
        // distinct
        // NULL [0]= 0.58;
        // -> cut off hypothesis when probability falls below accepted threshold, or when average falls below
        // threshold
        //
        // unrealized NULL rules seem to have confidence == 1.0 -> all NULL rules have C=1.0

        // DONE
        // map phrase to choice indices before evaluating best phrase
        // -> better match on large phrase sets, supports hypothesis as well

        // DONE: rules and their children may not be consistent as SAPI replaces text in the matched rules
        // + however the rule index of such child rules is the right one - can be used to rebuild rule indices
        // -> If the children of the rule aren't distinct, no match

        // TODO: map rule indices back to choice indices when building srgs nodes
        // - much simpler then processing them here
        // - allows for optimizing rules, especially optional ones

        // TODO remember multiple hypotheses
        // + keep all hypothesis results
        // + keep those who continue in the next event
        // + copy over confidence from previous hypothesis

        List<Rule> candidates = new ArrayList<>(rules.size());
        for (Rule rule : rules) {
            if (!rule.text.isBlank()) {
                if (rule.hasTrailingNullRule()) {
                    rule = rule.withoutIgnoreableTrailingNullRules();
                }
                // TODO matching rule indices during repair must map to choices
                // -> avoids wrong repair in
                // teaselib.core.speechrecognition.SpeechRecognitionComplexTest.testSRGSBuilderMultipleChoicesAlternativePhrases()
                List<Rule> repaired = rule.repair(slicedPhrases);
                if (repaired.isEmpty()) {
                    candidates.add(rule);
                } else {
                    candidates.addAll(repaired);
                }
            }
        }

        validate(candidates);

        Optional<Rule> result = SpeechRecognitionInputMethod.bestSingleResult(candidates.stream(),
                preparedChoices.mapper());
        if (result.isPresent()) {
            Rule rule = result.get();
            if (rule.indices.equals(Rule.NoIndices)) {
                logger.info("Ignoring hypothesis {} since it contains results from multiple phrases", rule);
                return Optional.empty();
            } else {
                double expectedConfidence = SpeechRecognitionInputMethod.expectedHypothesisConfidence(choices, rule);
                // Weighted probability is too low in tests for short hypotheses even with optimal probability of 1.0
                // This is intended because probability/confidence values of short short hypotheses cannot be trusted
                // -> confidence moves towards ground truth when more speech is detected
                float weightedProbability = weightedProbability(choices, rule, slicedPhrases);
                if (weightedProbability >= expectedConfidence) {
                    if (currentHypothesis == null) {
                        logger.info("Considering as new hypothesis");
                        return Optional.of(newHypothesis(rule, weightedProbability));
                    } else if (currentHypothesis.indices.containsAll(rule.indices)) {
                        float hypothesisProbability = weightedProbability(choices, currentHypothesis, slicedPhrases);
                        float h = currentHypothesis.children.size();
                        float r = rule.children.size();
                        float average = (hypothesisProbability * h + rule.probability * r) / (h + r);
                        logger.info("Considering as hypothesis");
                        return Optional.of(newHypothesis(rule, Math.max(average, rule.probability)));
                    } else {
                        return Optional.empty();
                    }
                } else {
                    logger.info("Weighted confidence {} < expected hypothesis confidence {} - hypothesis ignored", //
                            weightedProbability, expectedConfidence);
                    return Optional.empty();
                }
            }
        } else {
            return Optional.empty();
        }
    }

    private float weightedProbability(Choices choices, Rule rule, SlicedPhrases<PhraseString> slicedPhrases) {
        if (rule.indices.equals(Rule.NoIndices))
            throw new IllegalArgumentException("Rule contains no indices - consider updating it after adding children");

        PhraseString text;
        List<PhraseString> complete;
        if (slicedPhrases != null) {
            text = new PhraseString(rule.text, rule.indices);
            complete = slicedPhrases.complete(text);
        } else {
            Integer index = rule.indices.iterator().next();
            text = new PhraseString(rule.text, index);
            complete = new PhraseString(choices.get(index).phrases.get(0), index).words();
        }

        float weight = (float) text.words().size() / (float) complete.size();
        return rule.probability * weight;
    }

    private Rule newHypothesis(Rule rule, float probability) {
        return new Rule(rule, Rule.HYPOTHESIS, probability, rule.confidence);
    }

    private static void validate(List<Rule> candidates) {
        candidates.stream().forEach(Rule::isValid);
    }

}
