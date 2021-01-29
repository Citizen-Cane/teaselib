package teaselib.core.speechrecognition.srgs;

import static java.util.stream.Collectors.*;

import java.util.ArrayList;
import java.util.Collections;
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

import teaselib.core.speechrecognition.Rule;
import teaselib.core.ui.Choices;

public class SRGSPhraseBuilder extends AbstractSRGSBuilder {

    public final Choices choices;
    public final PhraseMapping mapping;
    public final SlicedPhrases<PhraseString> slices;

    int guid = 0;

    public SRGSPhraseBuilder(Choices choices, String languageCode)
            throws ParserConfigurationException, TransformerFactoryConfigurationError, TransformerException {
        this(choices, languageCode, new PhraseMapping.Relaxed(choices));
    }

    public SRGSPhraseBuilder(Choices choices, String languageCode, PhraseMapping mapping)
            throws ParserConfigurationException, TransformerFactoryConfigurationError, TransformerException {
        super(languageCode);
        this.choices = choices;
        this.mapping = mapping;
        this.slices = SlicedPhrases.of(PhraseStringSequences.of(mapping.phrases), PhraseStringSequences::prettyPrint);
        buildXML();
    }

    @Override
    void buildXML() throws TransformerFactoryConfigurationError, TransformerException {
        Element grammar = createGrammar();
        Element main = createMainRule(Rule.MAIN_RULE_NAME);
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
        Indices<Element> current = new Indices<>(mapping.phrases.size(), main);
        Indices<Element> next = new Indices<>(current);
        Set<Integer> all = Collections.unmodifiableSet(allIndices());
        for (int i = 0; i < slices.size(); i++) {
            List<PhraseString> slice = slices.get(i).stream().map(Sequence::joined).collect(toList());

            Set<Integer> coverage = mapping
                    .srgs(slice.stream().flatMap(phrase -> phrase.indices.stream()).collect(toSet()));
            Set<Integer> remaining = new HashSet<>(all);
            Set<Element> currentCoverage = current.collect(coverage);
            Set<Integer> uncovered = new HashSet<>(all);
            uncovered.removeAll(coverage);
            if (currentCoverage.size() == 1) {
                Element common = currentCoverage.iterator().next();
                List<Element> ruleRefs = new ArrayList<>();

                for (PhraseString phrase : slice) {
                    Set<Integer> indices;
                    String ruleName;

                    boolean fullCoverage = phrase.indices.size() == mapping.phrases.size();
                    if (fullCoverage) {
                        indices = mapping.srgs(phrase.indices);
                        ruleName = choiceName(i, indices);
                        ruleRefs.add(ruleRef(ruleName));
                        remaining.clear();
                    } else {
                        if (mapping.isOptional(phrase, uncovered)) {
                            indices = new HashSet<>();
                            indices.addAll(mapping.srgs(phrase.indices));
                            indices.addAll(uncovered);
                            ruleName = choiceName(i, indices);
                            ruleRefs.add(optional(ruleRef(ruleName)));
                        } else {
                            indices = mapping.srgs(phrase.indices);
                            ruleName = choiceName(i, indices);
                            ruleRefs.add(ruleRef(ruleName));
                        }
                        remaining.removeAll(indices);
                    }

                    next.add(indices, common);
                    addRule(grammar, ruleName, phrase.phrase);
                }

                if (!remaining.isEmpty()) {
                    Set<Integer> missing = mapping.srgs(remaining);
                    String ruleName = choiceName(i, missing);
                    ruleRefs.add(ruleRef(ruleName));
                    addNullRule(grammar, ruleName);
                    next.add(missing, common);
                }

                common.appendChild(gather(ruleRefs));
            } else {
                Map<Element, List<Element>> ruleRefs = new HashMap<>();

                for (PhraseString phrase : slice) {
                    Set<Integer> indices = mapping.srgs(phrase.indices);
                    Set<Element> nodes = current.collect(indices);
                    for (Element element : nodes) {
                        String ruleName = choiceName(i, indices);
                        Element ruleRef = ruleRef(ruleName);
                        ruleRefs.computeIfAbsent(element, e -> new ArrayList<>()).add(ruleRef);
                        next.add(indices, element);
                        addRule(grammar, ruleName, phrase.phrase);
                    }
                    remaining.removeAll(phrase.indices);
                }

                if (!remaining.isEmpty()) {
                    Set<Integer> missingChoiceIndices = mapping.srgs(remaining);
                    String ruleName = choiceName(i, missingChoiceIndices);
                    Element ruleRef = ruleRef(ruleName);
                    Set<Element> nodes = current.collect(missingChoiceIndices);
                    for (Element element : nodes) {
                        ruleRefs.computeIfAbsent(element, e -> new ArrayList<>()).add(ruleRef);
                        addNullRule(grammar, ruleName);
                        next.add(missingChoiceIndices, element);
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
        int size = mapping.phrases.size();
        Set<Integer> indices = new HashSet<>(size);
        for (int i = 0; i < size; i++) {
            indices.add(i);
        }
        return indices;
    }

    private Element item(Element ruleRef) {
        Element item = document.createElement("item");
        item.appendChild(ruleRef);
        return item;
    }

    private Element addNullRule(Element grammar, String id) {
        Element element = document.createElement("rule");
        addAttribute(element, "id", id);
        addAttribute(element, "scope", "private");

        Element specialNull = document.createElement("ruleref");
        addAttribute(specialNull, "special", "NULL");
        element.appendChild(specialNull);
        grammar.appendChild(element);

        return element;
    }

    private Element optional(Element ruleRef) {
        Element optional = item(ruleRef);
        addAttribute(optional, "repeat", "0-1");
        optional.appendChild(ruleRef);
        return optional;
        // Element oneOf = document.createElement("one-of");
        // oneOf.appendChild(optional);
        // return oneOf;
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
        String encodedIndices = indices.stream().map(Object::toString).collect(joining(","));
        return Rule.CHOICE_NODE_PREFIX + n + "_" + encodedIndices + "_" + guid;
    }

    private Element gather(List<Element> elements) {
        if (elements.size() == 1) {
            return elements.get(0);
        } else {
            Element items = document.createElement("one-of");
            for (Element element : elements) {
                if (element.getNodeName().equals("item")) {
                    items.appendChild(element);
                } else {
                    items.appendChild(item(element));
                }
            }
            return items;
        }
    }

}
