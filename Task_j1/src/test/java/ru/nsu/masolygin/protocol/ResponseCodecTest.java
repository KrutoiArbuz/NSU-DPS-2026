package ru.nsu.masolygin.protocol;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.nio.ByteBuffer;
import org.junit.jupiter.api.Test;
import ru.nsu.masolygin.crypto.KeyMaterial;

public class ResponseCodecTest {
    @Test
    void testRoundTripPreservesAllThreeBlocks() throws IOException {
        KeyMaterial expected = new KeyMaterial(new byte[] { 1, 2 }, new byte[] { 3, 4 }, new byte[] { 5, 6 });
        ResponseCodec codec = new ResponseCodec();
        byte[] response = codec.success(expected).array();
        KeyMaterial actual = codec.read(new DataInputStream(new ByteArrayInputStream(response)));
        assertArrayEquals(expected.privateKey(), actual.privateKey());
        assertArrayEquals(expected.publicKey(), actual.publicKey());
        assertArrayEquals(expected.certificate(), actual.certificate());
    }

    @Test
    void testFailureCarriesMessage() {
        ResponseCodec codec = new ResponseCodec();
        byte[] response = codec.failure("Generation unavailable").array();
        IOException error = assertThrows(IOException.class,
                () -> codec.read(new DataInputStream(new ByteArrayInputStream(response))));
        assertEquals("Generation unavailable", error.getMessage());
    }

    @Test
    void testNegativeLengthRejected() {
        byte[] response = ByteBuffer.allocate(5).put((byte) 0).putInt(-1).array();
        assertThrows(IOException.class, () -> new ResponseCodec().read(
                new DataInputStream(new ByteArrayInputStream(response))));
    }

    @Test
    void testTruncatedBlockRejected() {
        byte[] response = ByteBuffer.allocate(6).put((byte) 0).putInt(2).put((byte) 1).array();
        assertThrows(EOFException.class, () -> new ResponseCodec().read(
                new DataInputStream(new ByteArrayInputStream(response))));
    }
}
