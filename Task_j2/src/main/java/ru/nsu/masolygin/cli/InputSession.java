package ru.nsu.masolygin.cli;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintStream;
import ru.nsu.masolygin.list.StringList;

public class InputSession {
    public void run(BufferedReader input, PrintStream output, StringList list) throws IOException {
        String line;
        while ((line = input.readLine()) != null) {
            if (line.isEmpty()) {
                for (String value : list) {
                    output.println(value);
                }
            } else {
                for (int offset = 0; offset < line.length(); offset += 80) {
                    list.addFirst(line.substring(offset, Math.min(offset + 80, line.length())));
                }
            }
        }
    }
}
