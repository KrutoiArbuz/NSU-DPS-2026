package ru.nsu.masolygin.crypto;

import java.io.IOException;
import java.security.GeneralSecurityException;

public interface KeyGenerator {
    KeyMaterial generate(String name) throws GeneralSecurityException, IOException;
}
