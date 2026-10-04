package ru.nsu.masolygin.server;

import java.io.ByteArrayOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;

class ClientConnection {
    private final SocketChannel channel;
    private final ByteArrayOutputStream name = new ByteArrayOutputStream();
    private final ByteBuffer input = ByteBuffer.allocate(1024);
    private ByteBuffer response;

    ClientConnection(SocketChannel channel) {
        this.channel = channel;
    }

    String readName() throws IOException {
        input.clear();
        int count = channel.read(input);
        if (count == -1) {
            throw new EOFException("Connection closed before name terminator");
        }
        input.flip();
        while (input.hasRemaining()) {
            byte value = input.get();
            if (value == 0) {
                return name.toString(StandardCharsets.US_ASCII);
            }
            if (value < 0) {
                throw new IOException("Name must contain ASCII bytes only");
            }
            name.write(value);
        }
        return null;
    }

    void prepareResponse(ByteBuffer response) {
        this.response = response;
    }

    boolean writeResponse() throws IOException {
        if (response == null) {
            throw new IllegalStateException("Response not prepared");
        }
        channel.write(response);
        return !response.hasRemaining();
    }
}
