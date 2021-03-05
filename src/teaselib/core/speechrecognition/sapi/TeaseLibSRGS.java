package teaselib.core.speechrecognition.sapi;

import static teaselib.core.util.ExceptionUtil.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.function.IntUnaryOperator;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import teaselib.core.speechrecognition.PreparedChoices;
import teaselib.core.speechrecognition.Rule;
import teaselib.core.speechrecognition.SpeechRecognitionProvider;
import teaselib.core.speechrecognition.srgs.PhraseMapping;
import teaselib.core.speechrecognition.srgs.PhraseString;
import teaselib.core.speechrecognition.srgs.SRGSPhraseBuilder;
import teaselib.core.speechrecognition.srgs.SlicedPhrases;
import teaselib.core.ui.Choices;
import teaselib.core.util.CodeDuration;

public abstract class TeaseLibSRGS extends TeaseLibSR.SAPI {
    private static final Logger logger = LoggerFactory.getLogger(TeaseLibSRGS.class);

    private PreparedChoicesImplementation preparedChoices = null;

    class PreparedChoicesImplementation implements PreparedChoices {
        final Choices choices;
        final SlicedPhrases<PhraseString> slicedPhrases;
        final byte[] srgs;
        final IntUnaryOperator mapper;

        PreparedChoicesImplementation(Choices choices, SlicedPhrases<PhraseString> slicedPhrases, byte[] srgs,
                IntUnaryOperator mapper) {
            this.choices = choices;
            this.slicedPhrases = slicedPhrases;
            this.srgs = srgs;
            this.mapper = mapper;
        }

        @Override
        public void accept(SpeechRecognitionProvider sri) {
            if (sri == TeaseLibSRGS.this) {
                setChoices(srgs);
                preparedChoices = this;
            } else {
                throw new IllegalArgumentException();
            }
        }

        @Override
        public float weightedProbability(Rule rule) {
            if (rule.indices.equals(Rule.NoIndices))
                throw new IllegalArgumentException(
                        "Rule contains no indices - consider updating it after adding children");

            PhraseString text = new PhraseString(rule.text, rule.indices);
            List<PhraseString> complete = slicedPhrases.complete(text);
            float weight = (float) text.words().size() / (float) complete.size();
            return rule.probability * weight;
        }

        @Override
        public IntUnaryOperator mapper() {
            return mapper;
        }
    }

    public TeaseLibSRGS(Locale locale) {
        super(locale);
    }

    @Override
    public PreparedChoices prepare(Choices choices) {
        SRGSPhraseBuilder builder = CodeDuration.executionTimeMillis(logger, "Sliced in {}ms", () -> {
            try {
                return new SRGSPhraseBuilder(choices, languageCode(), mapping(choices));
            } catch (ParserConfigurationException e) {
                throw asRuntimeException(e);
            }
        });

        byte[] bytes;
        try {
            if (logger.isInfoEnabled()) {
                logger.info("{}", builder.slices);
                logger.info("{}", builder.toXML());
            }

            bytes = builder.toBytes();
        } catch (TransformerException e) {
            throw asRuntimeException(e);
        }

        IntUnaryOperator mapper = builder.mapping::choice;
        return new PreparedChoicesImplementation(choices, builder.slices, bytes, mapper);
    }

    abstract PhraseMapping mapping(Choices choices);

    public static class Strict extends TeaseLibSRGS {

        public Strict(Locale locale) {
            super(locale);
        }

        @Override
        PhraseMapping mapping(Choices choices) {
            return new PhraseMapping.Strict(choices);
        }
    }

    public static class Relaxed extends TeaseLibSRGS {

        public Relaxed(Locale locale) {
            super(locale);
        }

        @Override
        PhraseMapping mapping(Choices choices) {
            return new PhraseMapping.Relaxed(choices);
        }
    }

    @Override
    public List<Rule> repair(List<Rule> result) {
        Objects.requireNonNull(result);
        Objects.requireNonNull(preparedChoices);
        return repair(result, preparedChoices.slicedPhrases);
    }

    private static List<Rule> repair(List<Rule> result, SlicedPhrases<PhraseString> slicedPhrases) {

        // Caveats:

        // DONE
        // - trailing null rules make rule undefined (distinct [1], common [0,1], distinct NULL [0]
        // -> remove trailing null rules because the available rules already define a distinct result
        // + trailing null rules are at the end of the text (fromElement = size)

        // DONE
        // words are omitted in the main rule, instead there are NULL child rules
        // Example: "Ja Mit <NULL> dicken Dildo Frau Streng" -> "Ja Mit "DEM" dicken Dildo Frau Streng" ->
        // matches
        // phrase
        // -> for each NULL rule try to reconstruct the phrase by replacing the NULL rule with a valid phrase
        // + if it matches a phrase, use that one (works for speechDetected as well as for
        // recognitionCompleted/Rejected
        // - the phrase should match the beginning of a single phrase - can rate incomplete phrases -> not needed

        // DONE: rules and their children may not be consistent as SAPI replaces text in the matched rules
        // + however the rule index of such child rules is the right one - can be used to rebuild rule indices
        // -> If the children of the rule aren't distinct, no match

        List<Rule> rules = new ArrayList<>(result.size());
        for (Rule rule : result) {
            String text = rule.text;
            if (text != null && !text.isBlank()) {
                if (rule.hasIgnoreableTrailingNullRule()) {
                    rule = rule.withoutIgnoreableTrailingNullRules();
                }
                // TODO matching rule indices during repair must map to choices
                // -> avoids wrong repair in
                // teaselib.core.speechrecognition.SpeechRecognitionComplexTest.testSRGSBuilderMultipleChoicesAlternativePhrases()
                List<Rule> repaired = rule.repair(slicedPhrases);
                if (repaired.isEmpty()) {
                    rules.add(rule);
                } else {
                    rules.addAll(repaired);
                }
            }
        }

        validate(rules);
        return rules;
    }

    private static void validate(List<Rule> candidates) {
        candidates.stream().forEach(Rule::isValid);
    }

}
