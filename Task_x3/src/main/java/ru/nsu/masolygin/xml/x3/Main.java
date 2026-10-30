package ru.nsu.masolygin.xml.x3;

import java.io.IOException;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import org.xml.sax.SAXException;
import ru.nsu.masolygin.xml.cli.Arguments;
import ru.nsu.masolygin.xml.transform.FamilyReport;

public class Main {

    public static void main(String[] args)
        throws IOException, SAXException, ParserConfigurationException, TransformerException {
        Arguments arguments = new Arguments(args);
        var input = arguments.path("--input", "people.xml");
        var output = arguments.path("--output", "build/family.html");
        new FamilyReport().write(input, output);
        System.out.println("HTML: " + output);
    }
}
