package ru.nsu.masolygin.xml.x2;

import jakarta.xml.bind.JAXBException;
import java.io.IOException;
import javax.xml.parsers.ParserConfigurationException;
import org.xml.sax.SAXException;
import ru.nsu.masolygin.xml.cli.Arguments;
import ru.nsu.masolygin.xml.parse.PeopleReader;
import ru.nsu.masolygin.xml.write.JaxbPeopleWriter;

public class Main {

    public static void main(String[] args)
        throws IOException, SAXException, ParserConfigurationException, JAXBException {
        Arguments arguments = new Arguments(args);
        var people = new PeopleReader().read(
            arguments.path("--input", "task_X1.zip")
        );
        var output = arguments.path("--output", "build/people.xml");
        new JaxbPeopleWriter().write(people, output);
        System.out.println(
            "Schema valid; people: " +
                people.persons().size() +
                ", output: " +
                output
        );
    }
}
