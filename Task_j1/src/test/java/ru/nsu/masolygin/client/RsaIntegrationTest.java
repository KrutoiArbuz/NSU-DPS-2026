package ru.nsu.masolygin.client;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.Signature;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.interfaces.RSAPublicKey;
import java.security.interfaces.RSAPrivateKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;
import javax.security.auth.x500.X500Principal;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x500.style.BCStyle;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.io.TempDir;
import ru.nsu.masolygin.crypto.KeyMaterial;
import ru.nsu.masolygin.crypto.PemFiles;
import ru.nsu.masolygin.crypto.RsaKeyGenerator;
import ru.nsu.masolygin.server.RunningServer;

class RsaIntegrationTest {
    @TempDir
    Path directory;

    @Test
    @Timeout(300)
    void testRsa8192CertificateCacheAndClientFiles() throws Exception {
        KeyPairGenerator issuerGenerator = KeyPairGenerator.getInstance("RSA");
        issuerGenerator.initialize(2048);
        KeyPair issuerPair = issuerGenerator.generateKeyPair();
        PemFiles files = new PemFiles();
        Path issuerFile = directory.resolve("issuer.key");
        files.write(issuerFile, "PRIVATE KEY", issuerPair.getPrivate().getEncoded());
        PrivateKey signingKey = files.readSigningKey(issuerFile);
        String issuer = "CN=Test Issuer,O=NSU";
        String name = "Alice, O=literal";
        RsaKeyGenerator generator = new RsaKeyGenerator(signingKey, issuer);
        AtomicInteger calls = new AtomicInteger();
        try (RunningServer server = new RunningServer(value -> {
            calls.incrementAndGet();
            return generator.generate(value);
        }, 2)) {
            KeyClient client = new KeyClient();
            KeyMaterial first = client.request("127.0.0.1", server.port(), name, Duration.ZERO, false);
            KeyMaterial second = client.request("localhost", server.port(), name, Duration.ZERO, false);
            assertArrayEquals(first.privateKey(), second.privateKey());
            assertArrayEquals(first.publicKey(), second.publicKey());
            assertArrayEquals(first.certificate(), second.certificate());
            X509Certificate certificate = (X509Certificate) CertificateFactory.getInstance("X.509")
                .generateCertificate(new ByteArrayInputStream(first.certificate()));
            certificate.verify(issuerPair.getPublic());
            certificate.checkValidity();
            assertEquals(new X500Principal(issuer), certificate.getIssuerX500Principal());
            String actualName = X500Name.getInstance(certificate.getSubjectX500Principal().getEncoded())
                .getRDNs(BCStyle.CN)[0].getFirst().getValue().toString();
            assertEquals(name, actualName);
            assertEquals(8192, ((RSAPublicKey) certificate.getPublicKey()).getModulus().bitLength());
            assertArrayEquals(first.publicKey(), certificate.getPublicKey().getEncoded());
            PrivateKey privateKey = KeyFactory.getInstance("RSA").generatePrivate(new PKCS8EncodedKeySpec(first.privateKey()));
            assertEquals(8192, ((RSAPrivateKey) privateKey).getModulus().bitLength());
            Signature signature = Signature.getInstance("SHA256withRSA");
            signature.initSign(privateKey);
            signature.update(new byte[]{1, 2, 3});
            byte[] signed = signature.sign();
            signature.initVerify(certificate.getPublicKey());
            signature.update(new byte[]{1, 2, 3});
            assertTrue(signature.verify(signed));
            String output = directory.resolve("alice").toString();
            ClientMain.main(new String[]{"--host", "127.0.0.1", "--port", Integer.toString(server.port()),
                "--name", name, "--output", output, "--delay", "1"});
            assertArrayEquals(first.privateKey(), files.readSigningKey(Path.of(output + ".key")).getEncoded());
            try (InputStream input = Files.newInputStream(Path.of(output + ".crt"))) {
                assertArrayEquals(first.certificate(), CertificateFactory.getInstance("X.509").generateCertificate(input).getEncoded());
            }
            assertEquals(1, calls.get());
            assertThrows(FileAlreadyExistsException.class, () -> files.write(issuerFile, "PRIVATE KEY", first.privateKey()));
        }
    }
}
