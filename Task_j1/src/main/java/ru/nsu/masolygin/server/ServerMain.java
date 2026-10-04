package ru.nsu.masolygin.server;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.security.PrivateKey;
import java.util.Set;

import ru.nsu.masolygin.cli.Arguments;
import ru.nsu.masolygin.crypto.PemFiles;
import ru.nsu.masolygin.crypto.RsaKeyGenerator;

public class ServerMain {
    public static void main(String[] args) {
        try {
            Arguments options = new Arguments(args, Set.of("host", "port", "workers", "signing-key", "issuer"));

            String host = options.optional("host", "0.0.0.0");
            int port = Integer.parseInt(options.optional("port", "8080"));
            int workers = Integer.parseInt(options.required("workers"));
            Path signingKeyPath = Path.of(options.required("signing-key"));
            String issuer = options.required("issuer");

            PrivateKey signingKey = new PemFiles().readSigningKey(signingKeyPath);
            RsaKeyGenerator generator = new RsaKeyGenerator(signingKey, issuer);

            try (KeyService service = new KeyService(generator, workers);
                    KeyServer server = new KeyServer(new InetSocketAddress(host, port), service)) {
                Thread shutdown = new Thread(server::close, "server-shutdown");
                Runtime.getRuntime().addShutdownHook(shutdown);
                try {
                    System.out.println("Listening on " + host + ":" + server.port() + ", workers=" + workers);
                    server.run();
                } finally {
                    try {
                        Runtime.getRuntime().removeShutdownHook(shutdown);
                    } catch (IllegalStateException e) {
                        // Ignore, shutdown already in progress.
                    }
                }
            }
        } catch (IOException | GeneralSecurityException | IllegalArgumentException e) {
            System.err.println("Server error: " + e.getMessage());
        }
    }
}
