package ru.nsu.masolygin.xml.binding;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlIDREF;

@XmlAccessorType(XmlAccessType.FIELD)
public class Link {

    @XmlIDREF
    @XmlAttribute(name = "ref", required = true)
    public PersonEntry person;

    public Link() {}

    public Link(PersonEntry person) {
        this.person = person;
    }
}
