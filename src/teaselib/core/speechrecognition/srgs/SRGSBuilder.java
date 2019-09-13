package teaselib.core.speechrecognition.srgs;

import static java.util.stream.Collectors.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
            for (OneOf item : rule) {
                for (int choice : item.choices) {
                    String inventoryKey = inventoryKey(rule, choice);
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
        return inventoryKey(rule, Collections.singleton(choice));
    }

    private static String inventoryKey(Rule rule, Set<Integer> choices) {
        return choices.stream().map(Object::toString).collect(joining(",")) + "_" + rule.group;
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

            String id = choiceName(rule, items.choices);
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
                .collect(toSet());
        Map<String, List<Element>> addToInventory = new HashMap<>();

        // TODO optimize srgs by adding common start/end rules directly to main rule

        for (OneOf items : rule) {
            // TODO isBlank() resolves teaselib.core.jni.NativeException in testSimpleIrregularPhrases and other tests:
            // line 0 hr=0x80045003 Rule reference "Choice_2_2,3,4__group_0" is undefined
            if (!items.isBlank()) {
                collectRules(rule, blank, addToInventory, items);
            }
        }

        addRulesToInventory(inventoryItems, addToInventory);
    }

    private void collectRules(Rule rule, Set<Integer> blank, Map<String, List<Element>> addToInventory, OneOf items) {
        for (int choice : items.choices) {
            String inventoryKey = inventoryKey(rule, choice);
            List<Element> elements = addToInventory.computeIfAbsent(inventoryKey, k -> new ArrayList<>());
            boolean optionalRule = blank.contains(choice);
            String choiceName = choiceName(rule, items.choices);
            Element ruleRef = ruleRef(choiceName);
            elements.add(optionalRule ? optionalRuleRefItem(ruleRef) : ruleRef);
        }
    }

    private void addRulesToInventory(Map<String, Element> inventoryItems, Map<String, List<Element>> addToInventory) {
        for (Map.Entry<String, List<Element>> addElements : addToInventory.entrySet()) {
            Element inventoryElement = inventoryItems.get(addElements.getKey());
            List<Element> elements = addElements.getValue();
            if (elements.size() == 1) {
                inventoryElement.appendChild(elements.get(0));
            } else {
                Element oneOf = document.createElement("one-of");
                inventoryElement.appendChild(oneOf);
                elements.stream().forEach(ruleRef -> oneOf.appendChild(ruleRefItem(ruleRef)));
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

}
