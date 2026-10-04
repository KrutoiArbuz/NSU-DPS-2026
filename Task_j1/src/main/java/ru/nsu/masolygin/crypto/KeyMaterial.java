package ru.nsu.masolygin.crypto;

public record KeyMaterial(
        byte[] privateKey,
        byte[] publicKey,
        byte[] certificate) {
}
