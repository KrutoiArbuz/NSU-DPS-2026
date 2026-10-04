package ru.nsu.masolygin.crypto;

import java.io.IOException;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.security.KeyPairGenerator;
import java.util.Set;

import ru.nsu.masolygin.cli.Arguments;

public class SigningKeyMain {
    public static void main(String[] args) {
        try {
            Arguments options = new Arguments(args, Set.of("output"));
            Path output = Path.of(options.optional("output", "issuer.key"));

            KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
            generator.initialize(3072);

            byte[] key = generator.generateKeyPair().getPrivate().getEncoded();

            new PemFiles().write(output, "PRIVATE KEY", key);
            System.out.println("Created signing key: " + output);
        } catch (IOException | GeneralSecurityException | IllegalArgumentException e) {
            System.err.println("Signing key error: " + e.getMessage());
        }
    }

}
