package teaselib.core.speechrecognition.srgs;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactoryConfigurationError;

import org.w3c.dom.Element;

public class SRGSBuilder extends AbstractSRGSBuilder {
    public SRGSBuilder(Phrases phrases, String languageCode) throws ParserConfigurationException, TransformerException {
        super(phrases, languageCode);
    }

    @Override
    void buildXML() throws TransformerFactoryConfigurationError, TransformerException {
        Element grammar = createGrammar();
        Map<String, Element> inventoryItems = createMainRule(grammar);

        for (Rule rule : phrases) {
            if (!rule.isEmpty()) {
                createRuleElements(grammar, inventoryItems, rule);
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
        for (Rule rule : phrases) {
            if (rule.index < 0) {
                throw new IndexOutOfBoundsException("Rule index of " + rule);
            }
            for (OneOf item : rule) {
                if (!item.isCommon()) {
                    if (item.choices.size() > 1) {
                        throw new IllegalArgumentException("Inventory accepts only distinct choices: " + item);
                    }
                    // TODO Inventory key from all choices (or common) - doesn't apply since choice size == 1
                    String inventoryKey = inventoryKey(rule, item.choices.iterator().next());
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

    private static String inventoryKey(Rule rule, int choice) {
        return choice + "_" + rule.group;
    }

    private static int inventoryChoice(String inventoryKey) {
        return Integer.parseInt(inventoryKey.substring(0, inventoryKey.indexOf("_")));
    }

    private static int inventoryGroup(String inventoryKey) {
        return Integer.parseInt(inventoryKey.substring(inventoryKey.indexOf("_") + 1));
    }

    private void createRuleElements(Element grammar, Map<String, Element> inventoryItems, Rule rule) {
        createRuleElements(grammar, rule);
        addRuleToInventory(inventoryItems, rule);
    }

    private void createRuleElements(Element grammar, Rule rule) {
        for (OneOf items : rule) {
            if (items.isBlank()) {
                // Rule elements must not be empty -> handle in inventory
                continue;
            }

            String id;
            if (items.isCommon()) {
                id = choiceName(rule, items.choices);
            } else {
                if (items.choices.size() > 1) {
                    throw new UnsupportedOperationException("OneOf item with multiple choices");
                }
                // TODO choice key from all choices (or common) - doesn't apply since choice size == 1
                id = choiceName(rule, items.choices.iterator().next());
            }

            Element ruleElement = createRule(id);
            grammar.appendChild(ruleElement);

            if (items.size() == 1) {
                String text = items.iterator().next();
                appendText(ruleElement, text);
            } else {
                Element oneOf = document.createElement("one-of");
                for (String text : items) {
                    Element item = document.createElement("item");
                    oneOf.appendChild(item);
                    appendText(item, text);
                }
                ruleElement.appendChild(oneOf);
            }
        }
    }

    private void addRuleToInventory(Map<String, Element> inventoryItems, Rule rule) {
        Set<Integer> blank = rule.stream().filter(OneOf::isBlank).map(item -> item.choices).flatMap(Set::stream)
                .collect(Collectors.toSet());

        Map<String, List<Element>> addToInventory = new HashMap<>();

        for (OneOf items : rule) {
            if (!items.isBlank()) {
                if (items.isCommon() && rule.size() == 1) {
                    // TODO optimize by adding common start/end rules directly to main rule
                    appendRuleRefToAllChoices(inventoryItems, rule);
                } else {
                    for (int choice : items.choices) {
                        String inventoryKey = inventoryKey(rule, choice);
                        List<Element> elements = addToInventory.computeIfAbsent(inventoryKey, key -> new ArrayList<>());
                        Element ruleRef = ruleRef(choiceName(rule, items.choices));
                        boolean optionalRule = blank.contains(choice);
                        elements.add(optionalRule ? optionalRuleRefItem(ruleRef) : ruleRef);
                    }
                }
            }
        }

        for (Map.Entry<String, List<Element>> addElements : addToInventory.entrySet()) {
            Element inventoryElement = inventoryItems.get(addElements.getKey());
            List<Element> items = addElements.getValue();
            if (items.size() == 1) {
                inventoryElement.appendChild(items.get(0));
            } else {
                Element oneOf = document.createElement("one-of");
                inventoryElement.appendChild(oneOf);
                items.stream().forEach(ruleRef -> oneOf.appendChild(ruleRefItem(ruleRef)));
            }
        }
    }

    private Element optionalRuleRefItem(Element child) {
        Element item = document.createElement("item");
        addAttribute(item, "repeat", "0-1");
        item.appendChild(child);
        return item;
    }

    private Element ruleRefItem(Element child) {
        Element item = document.createElement("item");
        item.appendChild(child);
        return item;
    }

    private void appendRuleRefToAllChoices(Map<String, Element> inventoryItems, Rule rule) {
        for (Map.Entry<String, Element> entry : inventoryItems.entrySet()) {
            String key = entry.getKey();
            int group = inventoryGroup(key);
            if (group == rule.group) {
                for (OneOf items : rule) {
                    int choice = inventoryChoice(key);
                    if (items.choices.contains(choice)) {
                        entry.getValue().appendChild(ruleRef(choiceName(rule, items.choices)));
                    }
                }
            }
        }
    }

}
