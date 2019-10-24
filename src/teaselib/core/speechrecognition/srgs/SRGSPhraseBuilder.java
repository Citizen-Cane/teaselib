package teaselib.core.speechrecognition.srgs;

import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactoryConfigurationError;

import org.w3c.dom.Element;

import teaselib.core.ui.Choices;

public class SRGSPhraseBuilder extends AbstractSRGSBuilder {

    final Choices choices;
    private IndexMap<Integer> index2choices;
    private List<PhraseString> phrases;

    public SRGSPhraseBuilder(Choices choices, String languageCode)
            throws ParserConfigurationException, TransformerFactoryConfigurationError, TransformerException {
        super(languageCode);
        this.choices = choices;
        this.index2choices = new IndexMap<>();
        this.phrases = choices.stream()
                .flatMap(choice -> choice.phrases.stream()
                        .map(phrase -> new PhraseString(phrase, index2choices.add(choices.indexOf(choice)))))
                .collect(Collectors.toList());

        buildXML();
    }

    @Override
    void buildXML() throws TransformerFactoryConfigurationError, TransformerException {
        Element grammar = createGrammar();
        Element main = createMainRule(MAIN_RULE_NAME);
        grammar.appendChild(main);

        createNodes(grammar, main);
    }

    static class Indices<T> {
        final Indices<T> previous;
        final Map<Integer, T> indexMap = new HashMap<>();

        public Indices(int size, T node) {
            this(null);
            for (int i = 0; i < size; i++) {
                indexMap.put(Integer.valueOf(i), node);
            }
        }

        public Indices(Indices<T> current) {
            this.previous = current;
        }

        void add(Set<Integer> indices, T element) {
            for (Integer index : indices) {
                indexMap.put(index, element);
            }
        }

        Set<T> collect(Set<Integer> indices) {
            return indexMap.entrySet().stream().filter(entry -> indices.contains(entry.getKey()))
                    .map(Map.Entry::getValue).collect(Collectors.toSet());
        }
    }

    private void createNodes(Element grammar, Element main) {
        List<Sequences<PhraseString>> slices = PhraseStringSequences.slice(phrases);

        // AB(0) ,BC(1) is sliced with choice 1 starting at 2nd slice
        // -> TODO gather B(1) with A(0) into one-of items
        // Generate lists first?

        // <one-of>
        // ..<item>
        // ....<ruleref uri="#Choice_0_0"/> A
        // ....<ruleref uri="#Choice_1_0,1"/> B
        // ..</item>
        // ..<item>
        // ....<ruleref uri="#Choice_1_0,1"/> B
        // ....<ruleref uri="#Choice_2_1"/> C
        // ..</item>
        // </one-of>

        // at slice 0, only A is known -> single ruleref
        // This might go on in the middle, so we need a generic solution
        // -> browse forward, but then we must remember handled slices (or parts) -> too complex
        // -> insert backwards, but then we must defer xml generation to the end -> recursion

        Indices<Element> current = new Indices<>(phrases.size(), main);
        Indices<Element> next = new Indices<>(current);
        int n = 0;
        while (!slices.isEmpty()) {
            List<PhraseString> slice = slices.remove(0).stream().map(e -> e.get(0)).collect(toList());

            Set<Integer> coverage = slice.stream().flatMap(phrase -> phrase.indices.stream()).collect(toSet());
            if (current.collect(coverage).size() == 1) {
                Element common = current.collect(coverage).iterator().next();
                List<Element> ruleRefs = new ArrayList<>();

                for (PhraseString phrase : slice) {
                    Set<Integer> indices = phrase.indices;
                    Element ruleRef = ruleRef(choiceName(n, indices));
                    ruleRefs.add(ruleRef);
                    next.add(indices, common);
                    addRule(grammar, phrase, n);
                }

                common.appendChild(gather(ruleRefs));
            } else {
                Map<Element, List<Element>> ruleRefs = new HashMap<>();

                for (PhraseString phrase : slice) {
                    Set<Integer> indices = phrase.indices;
                    Set<Element> nodes = current.collect(indices);
                    for (Element element : nodes) {
                        Element ruleRef = ruleRef(choiceName(n, indices));
                        ruleRefs.computeIfAbsent(element, e -> new ArrayList<>()).add(ruleRef);
                        next.add(indices, element);
                        addRule(grammar, phrase, n);
                    }
                }

                for (Entry<Element, List<Element>> items : ruleRefs.entrySet()) {
                    Element item = gather(items.getValue());
                    items.getKey().appendChild(item);
                }
            }

            // TODO Remember each set of indices and search back to find largest common chunk
            // append node to that chunk

            n++;
            current = next;
        }
    }

    private Element item(Element ruleRef) {
        Element item = document.createElement("item");
        item.appendChild(ruleRef);
        return item;
    }

    private void addRule(Element grammar, PhraseString phrase, int index) {
        Element element = document.createElement("rule");
        addAttribute(element, "id", choiceName(index, phrase.indices));
        addAttribute(element, "scope", "private");
        element.setTextContent(phrase.phrase);
        grammar.appendChild(element);
    }

    String choiceName(int n, Set<Integer> indices) {
        return CHOICE_NODE_PREFIX + n + "_"
                + index2choices.get(indices).stream().map(Object::toString).collect(joining(","));
    }

    private Element gather(List<Element> elements) {
        if (elements.size() == 1) {
            return elements.get(0);
        } else {
            Element items = document.createElement("one-of");
            for (Element element : elements) {
                items.appendChild(item(element));
            }
            return items;
        }
    }

}
