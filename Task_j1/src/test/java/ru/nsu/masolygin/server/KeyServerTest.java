package ru.nsu.masolygin.server;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.DataInputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import ru.nsu.masolygin.crypto.KeyMaterial;
import ru.nsu.masolygin.protocol.ResponseCodec;

@Timeout(30)
class KeyServerTest {
    @Test
    void testFragmentedNameDoesNotBlockAnotherClient() throws IOException {
        try (RunningServer server = new RunningServer(this::material, 1);
             Socket fragmented = connect(server.port())) {
            fragmented.getOutputStream().write("ali".getBytes(StandardCharsets.US_ASCII));
            fragmented.getOutputStream().flush();
            try (Socket ready = connect(server.port())) {
                sendName(ready, "bob");
                assertArrayEquals("bob".getBytes(StandardCharsets.US_ASCII), read(ready).privateKey());
            }
            fragmented.getOutputStream().write(new byte[]{'c', 'e', 0});
            assertArrayEquals("alice".getBytes(StandardCharsets.US_ASCII), read(fragmented).privateKey());
        }
    }

    @Test
    void testDisconnectDoesNotDiscardPendingResult() throws Exception {
        CountDownLatch started = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        AtomicInteger calls = new AtomicInteger();
        try (RunningServer server = new RunningServer(name -> {
            calls.incrementAndGet();
            started.countDown();
            await(release);
            return material(name);
        }, 1)) {
            try {
                try (Socket abandoned = connect(server.port())) {
                    sendName(abandoned, "alice");
                    assertTrue(started.await(5, TimeUnit.SECONDS));
                }
                try (Socket repeated = connect(server.port())) {
                    sendName(repeated, "alice");
                    release.countDown();
                    assertArrayEquals(material("alice").privateKey(), read(repeated).privateKey());
                    assertEquals(1, calls.get());
                }
            } finally {
                release.countDown();
            }
        }
    }

    @Test
    void testHundredClientsAndUnreadLargeReplyDoNotBlockNewRequest() throws IOException {
        AtomicInteger calls = new AtomicInteger();
        byte[] large = new byte[1024 * 1024];
        try (RunningServer server = new RunningServer(name -> {
            calls.incrementAndGet();
            return name.equals("slow") ? new KeyMaterial(large, large, large) : material(name);
        }, 1);
             Socket slow = new Socket()) {
            slow.setReceiveBufferSize(1024);
            slow.connect(new InetSocketAddress("127.0.0.1", server.port()), 5_000);
            sendName(slow, "slow");
            List<Socket> waiting = new ArrayList<>();
            try {
                for (int i = 0; i < 100; i++) {
                    Socket socket = connect(server.port());
                    waiting.add(socket);
                    socket.getOutputStream().write('a');
                }
                try (Socket ready = connect(server.port())) {
                    sendName(ready, "fast");
                    assertArrayEquals(material("fast").privateKey(), read(ready).privateKey());
                }
                assertEquals(2, calls.get());
            } finally {
                for (Socket socket : waiting) {
                    socket.close();
                }
            }
        }
    }

    @Test
    void testLargeResponseArrivesWithoutTruncation() throws IOException {
        byte[] large = new byte[1024 * 1024];
        large[large.length - 1] = 42;
        try (RunningServer server = new RunningServer(name -> new KeyMaterial(large, large, large), 1);
             Socket socket = connect(server.port())) {
            sendName(socket, "large");
            KeyMaterial actual = read(socket);
            assertArrayEquals(large, actual.privateKey());
            assertArrayEquals(large, actual.publicKey());
            assertArrayEquals(large, actual.certificate());
        }
    }

    @Test
    void testInvalidAsciiDoesNotStopServer() throws IOException {
        try (RunningServer server = new RunningServer(this::material, 1)) {
            try (Socket invalid = connect(server.port())) {
                invalid.getOutputStream().write(new byte[]{(byte) 255, 0});
                assertEquals(-1, invalid.getInputStream().read());
            }
            try (Socket valid = connect(server.port())) {
                sendName(valid, "valid");
                assertArrayEquals(material("valid").privateKey(), read(valid).privateKey());
            }
        }
    }

    @Test
    void testGenerationFailureReachesClient() throws IOException {
        try (RunningServer server = new RunningServer(name -> {
            throw new IOException("Simulated generation failure");
        }, 1);
             Socket socket = connect(server.port())) {
            sendName(socket, "failure");
            IOException error = assertThrows(IOException.class, () -> read(socket));
            assertEquals("Key generation failed", error.getMessage());
        }
    }

    private Socket connect(int port) throws IOException {
        Socket socket = new Socket();
        socket.connect(new InetSocketAddress("127.0.0.1", port), 5_000);
        socket.setSoTimeout(10_000);
        return socket;
    }

    private void sendName(Socket socket, String name) throws IOException {
        socket.getOutputStream().write(name.getBytes(StandardCharsets.US_ASCII));
        socket.getOutputStream().write(0);
        socket.getOutputStream().flush();
    }

    private KeyMaterial read(Socket socket) throws IOException {
        return new ResponseCodec().read(new DataInputStream(socket.getInputStream()));
    }

    private KeyMaterial material(String name) {
        byte[] data = name.getBytes(StandardCharsets.US_ASCII);
        return new KeyMaterial(data, data, data);
    }

    private void await(CountDownLatch latch) throws IOException {
        try {
            if (!latch.await(10, TimeUnit.SECONDS)) {
                throw new IOException("Test generator timed out");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Test generator interrupted", e);
        }
    }
}
