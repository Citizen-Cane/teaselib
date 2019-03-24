package teaselib.core.speechrecognition.srgs;

import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.TransformerFactoryConfigurationError;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

public class SRGSBuilder<T> {
    private static final String ruleNodePrefix = "Rule_";
    private static final String choiceNodePrefix = "Choice_";

    private final Document document;
    private final List<? extends List<T>> choices;
    private final String xml;

    public SRGSBuilder(List<? extends List<T>> choices) throws ParserConfigurationException, TransformerException {
        this.document = createDocument();
        this.choices = choices;
        this.xml = buildXML();
    }

    private String buildXML() throws TransformerFactoryConfigurationError, TransformerException {
        int index = 0;

        Element grammar = document.createElement("grammar");
        document.appendChild(grammar);
        addAttribute(grammar, "version", "1.0");
        addAttribute(grammar, "xml:lang", "en-US");
        addAttribute(grammar, "xmlns", "http://www.w3.org/2001/06/grammar");
        addAttribute(grammar, "tag-format", "semantics/1.0");
        String mainRuleId = "Main";
        addAttribute(grammar, "root", mainRuleId);

        // Main
        int max = maxSize(choices);
        Element mainRule = createRule(mainRuleId);
        grammar.appendChild(mainRule);

        Element inventoryNode = document.createElement("one-of");
        List<Element> inventoryItems = new ArrayList<>(max);
        for (int i = 0; i < max; i++) {
            Element inventoryItem = document.createElement("item");
            inventoryItems.add(inventoryItem);
            inventoryNode.appendChild(inventoryItem);
        }
        mainRule.appendChild(inventoryNode);

        for (List<? extends T> speechPart : choices) {
            if (!speechPart.isEmpty()) {
                String name = ruleName(index);
                for (int i = 0; i < speechPart.size(); i++) {
                    Element rule = document.createElement("rule");
                    String ruleName = speechPart.size() > 1 ? choiceName(index, i) : name;
                    addAttribute(rule, "id", ruleName);
                    addAttribute(rule, "scope", "private");
                    rule.appendChild(document.createTextNode(speechPart.get(i).toString()));
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
                    throw new IllegalArgumentException("Choices must be all different or all the sameh");
                }
                index++;
            }

        }
        return toXML();
    }

    private static String ruleName(int index) {
        return ruleNodePrefix + index;
    }

    private static String choiceName(int index, int i) {
        return choiceNodePrefix + index + "_" + i;
    }

    private Element ruleRef(String choiceRef) {
        Element ruleRef = document.createElement("ruleref");
        addAttribute(ruleRef, "uri", "#" + choiceRef);
        return ruleRef;
    }

    private int maxSize(List<? extends List<? extends T>> choices) {
        Optional<? extends List<? extends T>> reduced = choices.stream().reduce((a, b) -> a.size() > b.size() ? a : b);
        return reduced.isPresent() ? reduced.get().size() : 0;
    }

    private Element createRule(String mainRuleId) {
        Element element = document.createElement("rule");
        addAttribute(element, "id", mainRuleId);
        addAttribute(element, "scope", "public");
        return element;
    }

    private void addAttribute(Element element, String name, String value) {
        Attr attr = document.createAttribute(name);
        attr.setValue(value);
        element.setAttributeNode(attr);
    }

    private static Document createDocument() throws ParserConfigurationException {
        DocumentBuilderFactory documentFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder documentBuilder = documentFactory.newDocumentBuilder();
        return documentBuilder.newDocument();
    }

    private String toXML() throws TransformerFactoryConfigurationError, TransformerException {
        DOMSource domSource = new DOMSource(document);
        StringWriter result = new StringWriter();
        StreamResult streamResult = new StreamResult(result);

        TransformerFactory transformerFactory = TransformerFactory.newInstance();
        Transformer transformer = transformerFactory.newTransformer();

        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");

        transformer.transform(domSource, streamResult);

        return result.toString();
    }

    @Override
    public String toString() {
        return xml;
    }

}
