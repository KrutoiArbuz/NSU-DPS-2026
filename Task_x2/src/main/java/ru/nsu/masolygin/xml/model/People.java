package ru.nsu.masolygin.xml.model;

import java.util.Map;

public record People(Map<String, Person> persons, int sourceFragments) {
    public People {
        persons = Map.copyOf(persons);
    }
}
