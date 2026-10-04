package ru.nsu.masolygin.client;

import java.io.DataInputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

import ru.nsu.masolygin.crypto.KeyMaterial;
import ru.nsu.masolygin.protocol.ResponseCodec;

class KeyClient {
    KeyMaterial request(String host, int port, String name, Duration delay, boolean abort)
            throws IOException, InterruptedException {
        if (!StandardCharsets.US_ASCII.newEncoder().canEncode(name) || name.indexOf('\0') >= 0) {
            throw new IllegalArgumentException("Name must be ASCII without NUL");
        }
        if (delay.isNegative()) {
            throw new IllegalArgumentException("Delay must not be negative");
        }
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(host, port), 10000);
            socket.getOutputStream().write(name.getBytes(StandardCharsets.US_ASCII));
            socket.getOutputStream().write(0);
            socket.getOutputStream().flush();
            Thread.sleep(delay.toMillis());
            if (abort) {
                return null;
            }
            return new ResponseCodec().read(new DataInputStream(socket.getInputStream()));
        }
    }
}
