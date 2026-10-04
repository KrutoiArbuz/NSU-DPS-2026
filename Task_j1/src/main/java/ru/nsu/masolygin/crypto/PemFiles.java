package ru.nsu.masolygin.crypto;

import java.io.IOException;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.spec.PKCS8EncodedKeySpec;

import org.bouncycastle.util.io.pem.PemObject;
import org.bouncycastle.util.io.pem.PemReader;
import org.bouncycastle.util.io.pem.PemWriter;

public class PemFiles {
    public PrivateKey readSigningKey(Path path) throws IOException, GeneralSecurityException {
        try (PemReader reader = new PemReader(Files.newBufferedReader(path, StandardCharsets.US_ASCII))) {
            PemObject object = reader.readPemObject();
            if (object == null || !object.getType().equals("PRIVATE KEY")) {
                throw new IOException("Expected an unencrypted PKCS#8 PRIVATE KEY in " + path);
            }
            return KeyFactory.getInstance("RSA").generatePrivate(new PKCS8EncodedKeySpec(object.getContent()));
        }
    }

    public void write(Path path, String type, byte[] data) throws IOException {
        Path parent = path.toAbsolutePath().getParent();
        Files.createDirectories(parent);
        StringWriter text = new StringWriter();
        try (PemWriter writer = new PemWriter(text)) {
            writer.writeObject(new PemObject(type, data));
        }
        Files.writeString(path, text.toString(), StandardCharsets.US_ASCII, StandardOpenOption.CREATE_NEW);
    }
}
