package ru.nsu.masolygin.crypto;

import java.io.IOException;
import java.math.BigInteger;
import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;

import javax.security.auth.x500.X500Principal;

import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x500.X500NameBuilder;
import org.bouncycastle.asn1.x500.style.BCStyle;
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
import org.bouncycastle.cert.X509v3CertificateBuilder;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;

public class RsaKeyGenerator implements KeyGenerator {

    private final PrivateKey signingKey;
    private final X500Name issuer;
    private final SecureRandom random = new SecureRandom();

    public RsaKeyGenerator(PrivateKey signingKey, String issuer) {
        this.signingKey = signingKey;
        this.issuer = X500Name.getInstance(new X500Principal(issuer).getEncoded());
    }

    @Override
    public KeyMaterial generate(String name) throws GeneralSecurityException, IOException {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(8192);
        KeyPair pair = generator.generateKeyPair();
        X500NameBuilder subject = new X500NameBuilder(BCStyle.INSTANCE);
        subject.addRDN(BCStyle.CN, name);
        Instant now = Instant.now();
        BigInteger serial = new BigInteger(159, random).add(BigInteger.ONE);
        X509v3CertificateBuilder certificate = new X509v3CertificateBuilder(
                issuer,
                serial,
                Date.from(now),
                Date.from(now.plus(365, ChronoUnit.DAYS)),
                subject.build(),
                SubjectPublicKeyInfo.getInstance(pair.getPublic().getEncoded()));

        try {
            byte[] encoded = certificate.build(
                    new JcaContentSignerBuilder("SHA256withRSA").build(signingKey)).getEncoded();
            return new KeyMaterial(pair.getPrivate().getEncoded(), pair.getPublic().getEncoded(), encoded);
        } catch (OperatorCreationException e) {
            throw new GeneralSecurityException("Cannot sign certificate", e);
        }
    }

}
