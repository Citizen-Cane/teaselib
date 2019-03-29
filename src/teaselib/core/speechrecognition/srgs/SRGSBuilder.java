package teaselib.core.speechrecognition.srgs;

import java.util.ArrayList;
import java.util.List;

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

    private List<Element> createMainRule(Element grammar) {
        Element mainRule = createRule(MAIN_RULE_NAME);
        int max = SequenceUtil.max(choices);
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

    private void createRule(Element grammar, List<Element> inventoryItems, int index, List<? extends T> speechPart) {
        String name = createRule(grammar, index, speechPart);
        addRuleToInventory(inventoryItems, index, speechPart, name);
    }

    private String createRule(Element grammar, int index, List<? extends T> speechPart) {
        String name = ruleName(index);
        for (int i = 0; i < speechPart.size(); i++) {
            String id = speechPart.size() > 1 ? choiceName(index, i) : name;
            Element rule = createRule(id);
            grammar.appendChild(rule);

            // TODO Single text items can be added to the rule right away
            // - alternative phrases must be enumed via <one-of> <item> ... </one-of> </item>
            String text = speechPart.get(i).toString();
            appendText(rule, text);
        }
        return name;
    }

    private void addRuleToInventory(List<Element> inventoryItems, int index, List<? extends T> speechPart,
            String name) {
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
        return choiceNodePrefix + index + "_" + i;
    }

}
