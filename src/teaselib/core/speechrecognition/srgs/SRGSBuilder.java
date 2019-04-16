package teaselib.core.speechrecognition.srgs;

import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactoryConfigurationError;

import org.w3c.dom.Element;

import teaselib.core.speechrecognition.srgs.Phrases.Rule;

public class SRGSBuilder extends AbstractSRGSBuilder {
    public SRGSBuilder(Phrases phrases) throws ParserConfigurationException, TransformerException {
        super(phrases);
    }

    @Override
    void buildXML() throws TransformerFactoryConfigurationError, TransformerException {
        Element grammar = createGrammar();
        List<Element> inventoryItems = createMainRule(grammar);

        for (Phrases.Rule rule : phrases) {
            if (!rule.isEmpty()) {
                createRule(grammar, inventoryItems, rule);
            }
        }
    }

    private List<Element> createMainRule(Element grammar) {
        Element mainRule = createRule(MAIN_RULE_NAME);
        int max = phrases.choices();
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

    private void createRule(Element grammar, List<Element> inventoryItems, Phrases.Rule speechPart) {
        String name = createRule(grammar, speechPart);
        addRuleToInventory(inventoryItems, speechPart, name);
    }

    private String createRule(Element grammar, Phrases.Rule rule) {
        String name = ruleName(rule);
        for (Phrases.OneOf items : rule) {

            String id = rule.size() > 1 ? choiceName(rule, items.choiceIndex) : name;
            Element ruleElement = createRule(id);
            grammar.appendChild(ruleElement);

            if (items.size() == 1) {
                String text = items.get(0);
                appendText(ruleElement, text);
            } else {
                Element oneOf = document.createElement("one-of");
                for (String text : items) {
                    // TODO starting or ending with the same words makes the distinct part optional ("No" vs "No Miss")
                    // -> must be a separate rule path
                    // TODO Empty items make the whole rule optional
                    // -> must be a separate rule path
                    Element item = document.createElement("item");
                    oneOf.appendChild(item);
                    appendText(item, text);
                }
                ruleElement.appendChild(oneOf);
            }
        }
        return name;
    }

    private void addRuleToInventory(List<Element> inventoryItems, Phrases.Rule rule, String name) {
        if (rule.size() == 1) {
            appendRuleRefToAllChoices(inventoryItems, name);
        } else if (rule.size() == inventoryItems.size()) {
            appendRuleRefToEachChoice(inventoryItems, rule);
        } else {
            throw new IllegalArgumentException("Choices must be all different or all the same");
        }
    }

    private void appendRuleRefToAllChoices(List<Element> inventoryItems, String name) {
        for (Element element : inventoryItems) {
            element.appendChild(ruleRef(name));
        }
    }

    private void appendRuleRefToEachChoice(List<Element> inventoryItems, Phrases.Rule rule) {
        for (int choiceIndex = 0; choiceIndex < rule.size(); choiceIndex++) {
            inventoryItems.get(choiceIndex).appendChild(ruleRef(choiceName(rule, choiceIndex)));
        }
    }

    private static String choiceName(Rule rule, int choiceIndex) {
        return CHOICE_NODE_PREFIX + rule.index + "_" + choiceIndex;
    }

}
