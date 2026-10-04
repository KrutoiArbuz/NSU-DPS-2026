package ru.nsu.masolygin.protocol;

import java.io.DataInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

import ru.nsu.masolygin.crypto.KeyMaterial;

public class ResponseCodec {
    public ByteBuffer success(KeyMaterial material) {
        int size = 1 + 12 + material.privateKey().length + material.publicKey().length + material.certificate().length;
        ByteBuffer response = ByteBuffer.allocate(size);
        response.put((byte) 0);
        putBlock(response, material.privateKey());
        putBlock(response, material.publicKey());
        putBlock(response, material.certificate());
        return response.flip();
    }

    public ByteBuffer failure(String message) {
        byte[] text = message.getBytes(StandardCharsets.UTF_8);
        int size = 1 + 4 + text.length;
        ByteBuffer response = ByteBuffer.allocate(size);
        response.put((byte) 1);
        putBlock(response, text);
        return response.flip();
    }

    public KeyMaterial read(DataInputStream input) throws IOException {
        int status = input.readUnsignedByte();
        if (status == 1) {
            throw new IOException(new String(readBlock(input), StandardCharsets.UTF_8));
        }
        if (status != 0) {
            throw new IOException("Unknown response status: " + status);
        }
        return new KeyMaterial(readBlock(input), readBlock(input), readBlock(input));
    }

    private void putBlock(ByteBuffer buffer, byte[] block) {
        buffer.putInt(block.length);
        buffer.put(block);
    }

    private byte[] readBlock(DataInputStream input) throws IOException {
        int length = input.readInt();
        if (length < 0 || length > 1024 * 1024) {
            throw new IOException("Invalid response block length: " + length);
        }
        byte[] data = input.readNBytes(length);
        if (data.length != length) {
            throw new EOFException("Incomplete response block");
        }
        return data;
    }
}
