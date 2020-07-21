package teaselib.core.speechrecognition.srgs;

import java.io.ByteArrayOutputStream;
import java.io.StringWriter;

import javax.xml.XMLConstants;
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

import teaselib.core.speechrecognition.Rule;

/**
 * @author Citizen-Cane
 * 
 */
abstract class AbstractSRGSBuilder {

    private final String languageCode;
    final Document document;

    AbstractSRGSBuilder(String languageCode) throws ParserConfigurationException, TransformerFactoryConfigurationError {
        this.languageCode = languageCode;
        this.document = createDocument();
    }

    private static Document createDocument() throws ParserConfigurationException {
        DocumentBuilderFactory documentFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder documentBuilder = documentFactory.newDocumentBuilder();
        return documentBuilder.newDocument();
    }

    abstract void buildXML() throws TransformerFactoryConfigurationError, TransformerException;

    Element createGrammar() {
        Element grammar = document.createElement("grammar");
        document.appendChild(grammar);
        addAttribute(grammar, "version", "1.0");
        addAttribute(grammar, "xml:lang", languageCode);
        addAttribute(grammar, "xmlns", "http://www.w3.org/2001/06/grammar");
        addAttribute(grammar, "tag-format", "semantics/1.0");
        addAttribute(grammar, "root", Rule.MAIN_RULE_NAME);
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
        transformerFactory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);

        Transformer transformer = transformerFactory.newTransformer();
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
        transformer.transform(domSource, streamResult);

        return result.toString();
    }

    public byte[] toBytes() throws TransformerFactoryConfigurationError, TransformerException {
        DOMSource domSource = new DOMSource(document);

        TransformerFactory factory = TransformerFactory.newInstance();
        factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
        Transformer transformer = factory.newTransformer();

        ByteArrayOutputStream srgs = new ByteArrayOutputStream();
        transformer.transform(domSource, new StreamResult(srgs));
        return srgs.toByteArray();
    }

}