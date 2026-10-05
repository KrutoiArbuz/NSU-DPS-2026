package ru.nsu.masolygin.cli;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import ru.nsu.masolygin.list.LinkedStringList;

class InputSessionTest {
    @Test
    void testBlankLinePrintsFragmentsAndLatestInputFirst() throws IOException {
        String longLine = "x".repeat(81);
        BufferedReader input = new BufferedReader(new StringReader(longLine + "\nlast\n\n"));
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (PrintStream output = new PrintStream(bytes, true, StandardCharsets.UTF_8)) {
            new InputSession().run(input, output, new LinkedStringList());
        }
        String expected = "last" + System.lineSeparator() + "x" + System.lineSeparator() + "x".repeat(80) + System.lineSeparator();
        assertEquals(expected, bytes.toString(StandardCharsets.UTF_8));
    }
}
