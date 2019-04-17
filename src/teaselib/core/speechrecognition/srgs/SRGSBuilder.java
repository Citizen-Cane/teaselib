package teaselib.core.speechrecognition.srgs;

import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactoryConfigurationError;

import org.w3c.dom.Element;

import teaselib.core.speechrecognition.srgs.Phrases.OneOf;

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
        createRule(grammar, speechPart);
        addRuleToInventory(inventoryItems, speechPart);
    }

    private void createRule(Element grammar, Phrases.Rule rule) {
        for (Phrases.OneOf items : rule) {
            String id = items.choiceIndex != Phrases.COMMON_RULE ? choiceName(rule, items.choiceIndex) : ruleName(rule);
            Element ruleElement = createRule(id);
            grammar.appendChild(ruleElement);

            if (items.size() == 1) {
                String text = items.get(0);
                appendText(ruleElement, text);
            } else {
                // TODO each group must be sorted into a different one-of item inside the main rule
                // SpeechRecognitionTest.testSRGSBuilderMultiplePhrasesOfMultipleChoicesAreDistinctWithoutOptionalParts()
                // FIX all groups end up in the same </item> node -> should be another <one-of>
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
    }

    private void addRuleToInventory(List<Element> inventoryItems, Phrases.Rule rule) {
        for (int choiceIndex = 0; choiceIndex < rule.size(); choiceIndex++) {
            OneOf items = rule.get(choiceIndex);
            if (items.choiceIndex == Phrases.COMMON_RULE) {
                if (rule.size() > 1) {
                    throw new IllegalArgumentException("There may be only one entry per common rule");
                }
                appendRuleRefToAllChoices(inventoryItems, ruleName(rule));
            } else {
                inventoryItems.get(items.choiceIndex).appendChild(ruleRef(choiceName(rule, items.choiceIndex)));
            }
        }
    }

    private void appendRuleRefToAllChoices(List<Element> inventoryItems, String name) {
        for (Element element : inventoryItems) {
            element.appendChild(ruleRef(name));
        }
    }

}
