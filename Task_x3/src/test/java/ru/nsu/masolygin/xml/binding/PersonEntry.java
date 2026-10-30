package ru.nsu.masolygin.xml.binding;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlID;
import jakarta.xml.bind.annotation.XmlType;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(
    propOrder = {
        "firstName",
        "lastName",
        "gender",
        "parents",
        "marriage",
        "siblings",
        "children",
    }
)
public class PersonEntry {

    @XmlID
    @XmlAttribute(required = true)
    public String id;

    @XmlAttribute(name = "children-count", required = true)
    public int childrenCount;

    @XmlAttribute(name = "siblings-count", required = true)
    public int siblingsCount;

    @XmlElement(name = "first-name", required = true)
    public String firstName;

    @XmlElement(name = "last-name", required = true)
    public String lastName;

    public String gender;
    public Links parents = new Links();
    public Links marriage = new Links();
    public Links siblings = new Links();
    public Links children = new Links();

    public Links group(String kind) {
        return switch (kind) {
            case "parents" -> parents;
            case "marriage" -> marriage;
            case "siblings" -> siblings;
            case "children" -> children;
            default -> throw new IllegalArgumentException(
                "Unknown kind: " + kind
            );
        };
    }
}
