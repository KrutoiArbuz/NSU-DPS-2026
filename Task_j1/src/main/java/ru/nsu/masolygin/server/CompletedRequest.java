package ru.nsu.masolygin.server;

import java.nio.channels.SelectionKey;

import ru.nsu.masolygin.crypto.KeyMaterial;

record CompletedRequest(
        SelectionKey connection,
        KeyMaterial material,
        Throwable error) {
}
