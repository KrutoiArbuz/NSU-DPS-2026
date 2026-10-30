package ru.nsu.masolygin.xml.transform;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import javax.xml.XMLConstants;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import org.xml.sax.SAXException;
import ru.nsu.masolygin.xml.schema.PeopleSchema;

public class FamilyReport {

    public void write(Path input, Path output)
        throws SAXException, IOException, ParserConfigurationException, TransformerException {
        var document = new PeopleSchema().document(input);
        TransformerFactory factory = TransformerFactory.newInstance();
        factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
        factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
        factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_STYLESHEET, "");
        Files.createDirectories(output.toAbsolutePath().getParent());
        try (var stylesheet = getClass().getResourceAsStream("/family.xsl")) {
            var transformer = factory.newTransformer(
                new StreamSource(stylesheet)
            );
            transformer.transform(
                new DOMSource(document),
                new StreamResult(output.toFile())
            );
        }
    }
}
