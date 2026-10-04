package ru.nsu.masolygin.client;

import java.io.IOException;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Set;

import ru.nsu.masolygin.cli.Arguments;
import ru.nsu.masolygin.crypto.KeyMaterial;
import ru.nsu.masolygin.crypto.PemFiles;

public class ClientMain {
    public static void main(String[] args) {
        try {
            Arguments options = new Arguments(args, Set.of("host", "port", "name", "output", "delay", "abort"));
            String host = options.required("host");
            int port = Integer.parseInt(options.required("port"));
            String name = options.required("name");
            Duration delay = Duration.ofSeconds(Long.parseLong(options.optional("delay", "0")));
            KeyMaterial material = new KeyClient().request(host, port, name, delay, options.has("abort"));
            if (material == null) {
                System.out.println("Disconnected without reading the response");
                return;
            }

            String output = options.optional("output", "client");
            PemFiles files = new PemFiles();
            files.write(Path.of(output + ".key"), "PRIVATE KEY", material.privateKey());
            files.write(Path.of(output + ".crt"), "CERTIFICATE", material.certificate());
            System.out.println("Saved " + output + ".key and " + output + ".crt");

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.err.println("Client interrupted");
        } catch (IOException | IllegalArgumentException e) {
            System.err.println("Client error: " + e.getMessage());
        }
    }
}
