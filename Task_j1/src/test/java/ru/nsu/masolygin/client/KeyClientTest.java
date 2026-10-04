package ru.nsu.masolygin.client;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import ru.nsu.masolygin.crypto.KeyMaterial;
import ru.nsu.masolygin.server.RunningServer;

@Timeout(30)
class KeyClientTest {
    @Test
    void testClientAbortReturnsWithoutReading() throws IOException, InterruptedException {
        try (RunningServer server = new RunningServer(this::material, 1)) {
            assertNull(new KeyClient().request("127.0.0.1", server.port(), "abort", Duration.ZERO, true));
            KeyMaterial result = new KeyClient().request("127.0.0.1", server.port(), "healthy", Duration.ZERO, false);
            assertArrayEquals(material("healthy").privateKey(), result.privateKey());
        }
    }

    private KeyMaterial material(String name) {
        byte[] data = name.getBytes(StandardCharsets.US_ASCII);
        return new KeyMaterial(data, data, data);
    }

}
