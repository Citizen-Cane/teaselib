package teaselib.core.speechrecognition.srgs;

import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactoryConfigurationError;

import org.w3c.dom.Element;

public class SRGSBuilder extends AbstractSRGSBuilder {
    public SRGSBuilder(Phrases phrases) throws ParserConfigurationException, TransformerException {
        super(phrases);
    }

    @Override
    void buildXML() throws TransformerFactoryConfigurationError, TransformerException {
        Element grammar = createGrammar();
        List<Element> inventoryItems = createMainRule(grammar);

        int index = 0;
        for (Phrases.Rule speechPart : phrases) {
            if (!speechPart.isEmpty()) {
                createRule(grammar, inventoryItems, index, speechPart);
                index++;
            }
        }
    }

    private List<Element> createMainRule(Element grammar) {
        Element mainRule = createRule(MAIN_RULE_NAME);
        int max = phrases.maxLength();
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

    private void createRule(Element grammar, List<Element> inventoryItems, int index, Phrases.Rule speechPart) {
        String name = createRule(grammar, index, speechPart);
        addRuleToInventory(inventoryItems, index, speechPart, name);
    }

    private String createRule(Element grammar, int index, Phrases.Rule speechPart) {
        String name = ruleName(index);
        for (int i = 0; i < speechPart.size(); i++) {
            String id = speechPart.size() > 1 ? choiceName(index, i) : name;
            Element rule = createRule(id);
            grammar.appendChild(rule);

            Phrases.OneOf items = speechPart.get(i);
            if (items.size() == 1) {
                String text = items.get(0);
                appendText(rule, text);
            } else {
                Element oneOf = document.createElement("one-of");
                for (String text : items) {
                    // TODO starting or ending with the same words makes the distinct part optional ("No" vs "No Miss")
                    // -> must be a separate rule path
                    // TODO Empty items make the whole rule optional
                    // -> must be a separate rule path
                    Element item = document.createElement("item");
                    appendText(item, text);
                    oneOf.appendChild(item);
                }
                rule.appendChild(oneOf);
            }
        }
        return name;
    }

    private void addRuleToInventory(List<Element> inventoryItems, int index, Phrases.Rule speechPart, String name) {
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

    private static String choiceName(int index, int i) {
        return CHOICE_NODE_PREFIX + index + "_" + i;
    }

}
