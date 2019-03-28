package teaselib.core.speechrecognition.srgs;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactoryConfigurationError;

import org.w3c.dom.Element;

public class SRGSBuilder<T> extends AbstractSRGSBuilder<T> {
    public SRGSBuilder(List<? extends List<T>> choices) throws ParserConfigurationException, TransformerException {
        super(choices);
    }

    @Override
    void buildXML() throws TransformerFactoryConfigurationError, TransformerException {
        Element grammar = createGrammar();
        List<Element> inventoryItems = createMainRule(grammar);

        int index = 0;
        for (List<? extends T> speechPart : choices) {
            if (!speechPart.isEmpty()) {
                createRule(grammar, inventoryItems, index, speechPart);
                index++;
            }
        }
    }

    private void createRule(Element grammar, List<Element> inventoryItems, int index, List<? extends T> speechPart) {
        String name = ruleName(index);
        for (int i = 0; i < speechPart.size(); i++) {
            String id = speechPart.size() > 1 ? choiceName(index, i) : name;
            String text = speechPart.get(i).toString();
            Element rule = createRule(id);
            appendText(rule, text);
            grammar.appendChild(rule);
        }

        if (speechPart.size() == 1) {
            for (Element element : inventoryItems) {
                element.appendChild(ruleRef(name));
            }
        } else if (speechPart.size() == inventoryItems.size()) {
            for (int i = 0; i < speechPart.size(); i++) {
                inventoryItems.get(i).appendChild(ruleRef(choiceName(index, i)));
            }
        } else {
            throw new IllegalArgumentException("Choices must be all different or all the same");
        }
    }

    private List<Element> createMainRule(Element grammar) {
        Element mainRule = createRule(MAIN_RULE_NAME);
        int max = maxSize(choices);
        Element inventoryNode = document.createElement("one-of");
        List<Element> inventoryItems = new ArrayList<>(max);
        for (int i = 0; i < max; i++) {
            Element inventoryItem = document.createElement("item");
            inventoryItems.add(inventoryItem);
            inventoryNode.appendChild(inventoryItem);
        }
        mainRule.appendChild(inventoryNode);
        grammar.appendChild(mainRule);
        return inventoryItems;
    }

    private static String choiceName(int index, int i) {
        return choiceNodePrefix + index + "_" + i;
    }

    private int maxSize(List<? extends List<? extends T>> choices) {
        Optional<? extends List<? extends T>> reduced = choices.stream().reduce((a, b) -> a.size() > b.size() ? a : b);
        return reduced.isPresent() ? reduced.get().size() : 0;
    }

}
