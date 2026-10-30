package ru.nsu.masolygin.xml.binding;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlType;
import java.util.ArrayList;
import java.util.List;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(
    propOrder = {
        "father",
        "mother",
        "parent",
        "husband",
        "wife",
        "spouse",
        "brother",
        "sister",
        "sibling",
        "son",
        "daughter",
        "child",
    }
)
public class Links {

    public List<Link> father;
    public List<Link> mother;
    public List<Link> parent;
    public List<Link> husband;
    public List<Link> wife;
    public List<Link> spouse;
    public List<Link> brother;
    public List<Link> sister;
    public List<Link> sibling;
    public List<Link> son;
    public List<Link> daughter;
    public List<Link> child;

    public void add(String role, PersonEntry person) {
        switch (role) {
            case "father" -> {
                if (father == null) father = new ArrayList<>();
                father.add(new Link(person));
            }
            case "mother" -> {
                if (mother == null) mother = new ArrayList<>();
                mother.add(new Link(person));
            }
            case "parent" -> {
                if (parent == null) parent = new ArrayList<>();
                parent.add(new Link(person));
            }
            case "husband" -> {
                if (husband == null) husband = new ArrayList<>();
                husband.add(new Link(person));
            }
            case "wife" -> {
                if (wife == null) wife = new ArrayList<>();
                wife.add(new Link(person));
            }
            case "spouse" -> {
                if (spouse == null) spouse = new ArrayList<>();
                spouse.add(new Link(person));
            }
            case "brother" -> {
                if (brother == null) brother = new ArrayList<>();
                brother.add(new Link(person));
            }
            case "sister" -> {
                if (sister == null) sister = new ArrayList<>();
                sister.add(new Link(person));
            }
            case "sibling" -> {
                if (sibling == null) sibling = new ArrayList<>();
                sibling.add(new Link(person));
            }
            case "son" -> {
                if (son == null) son = new ArrayList<>();
                son.add(new Link(person));
            }
            case "daughter" -> {
                if (daughter == null) daughter = new ArrayList<>();
                daughter.add(new Link(person));
            }
            case "child" -> {
                if (child == null) child = new ArrayList<>();
                child.add(new Link(person));
            }
            default -> throw new IllegalArgumentException(
                "Unknown role: " + role
            );
        }
    }
}
