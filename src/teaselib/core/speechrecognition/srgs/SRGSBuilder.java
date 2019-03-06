package teaselib.core.speechrecognition.srgs;

import java.io.StringWriter;
import java.util.Arrays;
import java.util.List;

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

    private final Document document;
    private final List<? extends List<T>> choices;
    private final String xml;

    @SafeVarargs
    public SRGSBuilder(List<T>... choices) throws ParserConfigurationException, TransformerException {
        this(Arrays.asList(choices));
    }

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
        addAttribute(grammar, "root", ruleNodePrefix + index);

        for (List<T> list : choices) {
            if (!list.isEmpty()) {
                Element rule = document.createElement("rule");
                addAttribute(rule, "id", ruleNodePrefix + index++);
                addAttribute(rule, "scope", "public");
                grammar.appendChild(rule);

                Element container = list.size() > 1 ? document.createElement("one-of") : rule;
                for (T element : list) {
                    Element item = document.createElement("item");
                    item.appendChild(document.createTextNode(element.toString()));
                    container.appendChild(item);
                }

                if (container != rule) {
                    rule.appendChild(container);
                }

                if (list != choices.get(choices.size() - 1)) {
                    Element ruleRef = document.createElement("ruleref");
                    addAttribute(ruleRef, "uri", "#" + ruleNodePrefix + index);
                    rule.appendChild(ruleRef);
                }

                // TODO In cities_example, rule_ref is in node item node
                // TODO Rule_1 is inside Rule_0 item -> see cities_srg.xml

            }

        }
        return toXML();
    }

    private void addAttribute(Element element, String name, String value) {
        Attr attr = document.createAttribute(name);
        attr.setValue(value);
        element.setAttributeNode(attr);
    }

    private Document createDocument() throws ParserConfigurationException {
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
