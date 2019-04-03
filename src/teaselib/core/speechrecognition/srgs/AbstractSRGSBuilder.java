package teaselib.core.speechrecognition.srgs;

import java.io.StringWriter;

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

/**
 * @author Citizen-Cane
 * 
 */
abstract class AbstractSRGSBuilder {

    static final String MAIN_RULE_NAME = "Main";
    private static final String ruleNodePrefix = "Rule_";
    static final String choiceNodePrefix = "Choice_";

    final Phrases phrases;
    final Document document;

    AbstractSRGSBuilder(Phrases phrases)
            throws ParserConfigurationException, TransformerFactoryConfigurationError, TransformerException {
        this.phrases = phrases;
        this.document = createDocument();
        buildXML();
    }

    private static Document createDocument() throws ParserConfigurationException {
        DocumentBuilderFactory documentFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder documentBuilder = documentFactory.newDocumentBuilder();
        return documentBuilder.newDocument();
    }

    abstract void buildXML() throws TransformerFactoryConfigurationError, TransformerException;

    static String ruleName(int index) {
        return ruleNodePrefix + index;
    }

    Element createGrammar() {
        Element grammar = document.createElement("grammar");
        document.appendChild(grammar);
        addAttribute(grammar, "version", "1.0");
        addAttribute(grammar, "xml:lang", "en-US");
        addAttribute(grammar, "xmlns", "http://www.w3.org/2001/06/grammar");
        addAttribute(grammar, "tag-format", "semantics/1.0");
        addAttribute(grammar, "root", MAIN_RULE_NAME);
        return grammar;
    }

    Element ruleRef(String choiceRef) {
        Element ruleRef = document.createElement("ruleref");
        addAttribute(ruleRef, "uri", "#" + choiceRef);
        return ruleRef;
    }

    Element createMainRule(String id) {
        return createRule(id, "public");
    }

    Element createRule(String id) {
        return createRule(id, "private");
    }

    private Element createRule(String id, String scope) {
        Element element = document.createElement("rule");
        addAttribute(element, "id", id);
        addAttribute(element, "scope", scope);
        return element;
    }

    void addAttribute(Element element, String name, String value) {
        Attr attr = document.createAttribute(name);
        attr.setValue(value);
        element.setAttributeNode(attr);
    }

    void appendText(Element element, String text) {
        element.appendChild(document.createTextNode(text));
    }

    public String toXML() throws TransformerFactoryConfigurationError, TransformerException {
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

}