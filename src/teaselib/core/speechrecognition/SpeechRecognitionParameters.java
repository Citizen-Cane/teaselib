package teaselib.core.speechrecognition;

import java.util.function.IntUnaryOperator;

import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactoryConfigurationError;

import teaselib.core.speechrecognition.srgs.PhraseString;
import teaselib.core.speechrecognition.srgs.SRGSPhraseBuilder;
import teaselib.core.speechrecognition.srgs.SlicedPhrases;
import teaselib.core.ui.Choices;

public class SpeechRecognitionParameters {
    public final Choices choices;
    public final SlicedPhrases<PhraseString> slicedPhrases;
    final byte[] srgs;
    public final IntUnaryOperator mapper;

    public SpeechRecognitionParameters(Choices choices) {
        this(choices, null, null, index -> index);
    }

    public SpeechRecognitionParameters(SRGSPhraseBuilder phraseBuilder)
            throws TransformerFactoryConfigurationError, TransformerException {
        this(phraseBuilder.choices, phraseBuilder.slices, phraseBuilder.toBytes(), phraseBuilder::map);
    }

    public SpeechRecognitionParameters(Choices choices, SlicedPhrases<PhraseString> slicedPhrases, byte[] srgs,
            IntUnaryOperator mapper) {
        this.choices = choices;
        this.slicedPhrases = slicedPhrases;
        this.srgs = srgs;
        this.mapper = mapper;
    }

    public Integer choiceIndex(int phraseIndex) {
        return mapper.applyAsInt(phraseIndex);
    }

}