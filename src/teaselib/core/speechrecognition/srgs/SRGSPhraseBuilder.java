package teaselib.core.speechrecognition.srgs;

import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.function.Function;
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

    int guid = 0;

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
        private final Function<T, String> toString;

        public Indices(int size, T node) {
            this(size, node, T::toString);
        }

        public Indices(int size, T node, Function<T, String> toString) {
            this.previous = null;
            this.toString = toString;
            for (int i = 0; i < size; i++) {
                indexMap.put(Integer.valueOf(i), node);
            }
        }

        public Indices(Indices<T> current) {
            this.previous = current;
            this.toString = previous.toString;
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

        @Override
        public String toString() {
            return "["
                    + indexMap.entrySet().stream().map(entry -> entry.getKey() + "=" + toString.apply(entry.getValue()))
                            .collect(Collectors.joining(" "))
                    + "]";
        }

    }

    private void createNodes(Element grammar, Element main) {
        SlicedPhrases<PhraseString> slices = SlicedPhrases.of(PhraseStringSequences.of(phrases));

        Indices<Element> current = new Indices<>(phrases.size(), main);
        Indices<Element> next = new Indices<>(current);
        for (int i = 0; i < slices.size(); i++) {
            List<PhraseString> slice = slices.get(i).stream().map(e -> e.get(0)).collect(toList());

            Set<Integer> coverage = slice.stream().flatMap(phrase -> phrase.indices.stream()).collect(toSet());
            Set<Integer> missingIndices = allIndices();
            if (current.collect(coverage).size() == 1) {
                Element common = current.collect(coverage).iterator().next();
                List<Element> ruleRefs = new ArrayList<>();

                for (PhraseString phrase : slice) {
                    Set<Integer> indices = phrase.indices;
                    String ruleName = choiceName(i, indices);
                    Element ruleRef = ruleRef(ruleName);
                    ruleRefs.add(ruleRef);
                    missingIndices.removeAll(indices);
                    next.add(indices, common);
                    addRule(grammar, ruleName, phrase.phrase);
                }

                if (!missingIndices.isEmpty()) {
                    String ruleName = choiceName(i, missingIndices);
                    ruleRefs.add(ruleRef(ruleName));
                    specialRule(grammar, ruleName);
                    next.add(missingIndices, common);
                }

                common.appendChild(gather(ruleRefs));
            } else {
                Map<Element, List<Element>> ruleRefs = new HashMap<>();

                for (PhraseString phrase : slice) {
                    Set<Integer> indices = phrase.indices;
                    Set<Element> nodes = current.collect(indices);
                    for (Element element : nodes) {
                        String ruleName = choiceName(i, indices);
                        Element ruleRef = ruleRef(ruleName);
                        ruleRefs.computeIfAbsent(element, e -> new ArrayList<>()).add(ruleRef);
                        next.add(indices, element);
                        addRule(grammar, ruleName, phrase.phrase);
                    }
                    missingIndices.removeAll(indices);
                }

                if (!missingIndices.isEmpty()) {
                    String ruleName = choiceName(i, missingIndices);
                    Element ruleRef = ruleRef(ruleName);
                    Set<Element> nodes = current.collect(missingIndices);
                    for (Element element : nodes) {
                        ruleRefs.computeIfAbsent(element, e -> new ArrayList<>()).add(ruleRef);
                        specialRule(grammar, ruleName);
                        next.add(missingIndices, element);
                    }
                }

                for (Entry<Element, List<Element>> items : ruleRefs.entrySet()) {
                    Element item = gather(items.getValue());
                    items.getKey().appendChild(item);
                }

            }
            current = next;
        }
    }

    private Set<Integer> allIndices() {
        Set<Integer> indices = new HashSet<>();
        for (int i = 0; i < phrases.size(); i++) {
            indices.add(i);
        }
        return indices;
    }

    private Element item(Element ruleRef) {
        Element item = document.createElement("item");
        item.appendChild(ruleRef);
        return item;
    }

    private Element specialRule(Element grammar, String id) {
        Element element = document.createElement("rule");
        addAttribute(element, "id", id);
        addAttribute(element, "scope", "private");

        Element specialNull = document.createElement("ruleref");
        addAttribute(specialNull, "special", "NULL");
        element.appendChild(specialNull);
        grammar.appendChild(element);

        return element;
    }

    private void addRule(Element grammar, String id, String text) {
        Element element = document.createElement("rule");
        addAttribute(element, "id", id);
        addAttribute(element, "scope", "private");
        element.setTextContent(text);
        grammar.appendChild(element);
    }

    String choiceName(int n, Set<Integer> indices) {
        guid++;
        return CHOICE_NODE_PREFIX + n + "_" + indices.stream().map(Object::toString).collect(joining(",")) + "_" + guid;
    }

    public int map(int index) {
        return index2choices.get(index);
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
