package teaselib.core.speechrecognition.srgs;

import java.util.LinkedHashMap;
import java.util.Map;

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
        Map<String, Element> inventoryItems = createMainRule(grammar);

        for (Rule rule : phrases) {
            if (!rule.isEmpty()) {
                createRule(grammar, inventoryItems, rule);
            }
        }
    }

    private Map<String, Element> createMainRule(Element grammar) {
        Element mainRule = createRule(MAIN_RULE_NAME);
        grammar.appendChild(mainRule);
        return createInventory(mainRule);
    }

    private Map<String, Element> createInventory(Element mainRule) {
        Element inventoryNode = document.createElement("one-of");
        Map<String, Element> inventoryItems = new LinkedHashMap<>();
        // TODO Phrases may contain multiple rules with the same items -> reuse them
        // SpeechRecognitionTest.testSRGSBuilderMultiplePhrasesOfMultipleChoicesAreDistinctWithoutOptionalParts()
        for (Rule rule : phrases) {
            if (rule.index < 0) {
                throw new IndexOutOfBoundsException("Rule index of " + rule);
            }
            for (OneOf item : rule) {
                if (!item.isCommon()) {
                    String inventoryKey = inventoryKey(rule, item);
                    if (!inventoryItems.containsKey(inventoryKey)) {
                        Element inventoryItem = document.createElement("item");
                        inventoryItems.put(inventoryKey, inventoryItem);
                        inventoryNode.appendChild(inventoryItem);
                    }
                }
            }
        }
        mainRule.appendChild(inventoryNode);
        return inventoryItems;
    }

    private static String inventoryKey(Rule rule, OneOf item) {
        if (item.choices.size() > 1)
            throw new IllegalArgumentException("SRGS inventory accepts only distinct items: " + item);
        return item.choices.get(0) + "_" + rule.group;
    }

    private static int inventoryChoice(String inventoryKey) {
        return Integer.parseInt(inventoryKey.substring(0, inventoryKey.indexOf("_")));
    }

    private static int inventoryGroup(String inventoryKey) {
        return Integer.parseInt(inventoryKey.substring(inventoryKey.indexOf("_") + 1));
    }

    private void createRule(Element grammar, Map<String, Element> inventoryItems, Rule rule) {
        createRule(grammar, rule);
        addRuleToInventory(inventoryItems, rule);
    }

    private void createRule(Element grammar, Rule rule) {
        for (OneOf items : rule) {
            if (!items.isBlank()) {
                String id = items.isCommon() ? ruleName(rule) : choiceName(rule, items);
                Element ruleElement = createRule(id);
                grammar.appendChild(ruleElement);

                if (items.size() == 1) {
                    String text = items.iterator().next();
                    appendText(ruleElement, text);
                } else {
                    // TODO each group must be sorted into a different one-of item inside the main rule
                    // SpeechRecognitionTest.testSRGSBuilderMultiplePhrasesOfMultipleChoicesAreDistinctWithoutOptionalParts()
                    // FIX all groups end up in the same </item> node -> should be another <one-of>
                    Element oneOf = document.createElement("one-of");
                    for (String text : items) {
                        // starting or ending with the same words makes the distinct part optional
                        // ("No" vs "No Miss")
                        // TODO for exact recognition generate separate rule path
                        Element item = document.createElement("item");
                        oneOf.appendChild(item);
                        appendText(item, text);
                    }
                    ruleElement.appendChild(oneOf);
                }
            }
        }
    }

    private void addRuleToInventory(Map<String, Element> inventoryItems, Rule rule) {
        for (OneOf items : rule) {
            if (!items.isBlank()) {
                if (items.isCommon()) {
                    if (rule.size() > 1) {
                        throw new IllegalArgumentException("There may be only one entry per common rule");
                    }
                    // TODO optimize by adding common start/end rules directly to main rule
                    appendRuleRefToAllChoices(inventoryItems, rule);
                } else {
                    Element element = inventoryItems.get(inventoryKey(rule, items));
                    element.appendChild(ruleRef(choiceName(rule, items)));
                }
            }
        }
    }

    private void appendRuleRefToAllChoices(Map<String, Element> inventoryItems, Rule rule) {
        for (Map.Entry<String, Element> entry : inventoryItems.entrySet()) {
            String key = entry.getKey();
            int group = inventoryGroup(key);
            if (group == rule.group) {
                for (OneOf items : rule) {
                    int choice = inventoryChoice(key);
                    if (items.choices.contains(choice)) {
                        entry.getValue().appendChild(ruleRef(ruleName(rule)));
                    }
                }
            }
        }
    }

}
