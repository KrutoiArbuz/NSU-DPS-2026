package ru.nsu.masolygin.server;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;

import ru.nsu.masolygin.protocol.ResponseCodec;

class KeyServer implements AutoCloseable {
    private final KeyService service;
    private final Selector selector;
    private final ServerSocketChannel listener;
    private final ResponseCodec codec = new ResponseCodec();
    private final Queue<CompletedRequest> completed = new ConcurrentLinkedQueue<>();
    private final Set<SelectionKey> connections = new HashSet<>();
    private volatile boolean running = true;

    KeyServer(InetSocketAddress address, KeyService service) throws IOException {
        this.service = service;
        this.selector = Selector.open();
        this.listener = ServerSocketChannel.open();
        try {
            listener.configureBlocking(false);
            listener.bind(address, 256);
            listener.register(selector, SelectionKey.OP_ACCEPT);
        } catch (IOException | RuntimeException e) {
            listener.close();
            selector.close();
            throw e;
        }
    }

    int port() throws IOException {
        return ((InetSocketAddress) listener.getLocalAddress()).getPort();
    }

    void run() throws IOException {
        try (selector; listener) {
            while (running) {
                transferCompleted();
                selector.select();
                transferCompleted();
                Iterator<SelectionKey> events = selector.selectedKeys().iterator();
                while (events.hasNext()) {
                    SelectionKey key = events.next();
                    events.remove();
                    if (!key.isValid()) {
                        continue;
                    }
                    if (key.isAcceptable()) {
                        acceptClients();
                    } else {
                        handleClient(key);
                    }
                }
            }
        } finally {
            closeClients();
        }
    }

    private void acceptClients() throws IOException {
        SocketChannel channel;
        while ((channel = listener.accept()) != null) {
            try {
                channel.configureBlocking(false);
                connections.add(channel.register(selector, SelectionKey.OP_READ, new ClientConnection(channel)));
            } catch (IOException | RuntimeException e) {
                channel.close();
            }
        }
    }

    private void handleClient(SelectionKey key) {
        ClientConnection connection = (ClientConnection) key.attachment();
        try {
            if (key.isReadable()) {
                String name = connection.readName();
                if (name != null) {
                    key.interestOps(0);
                    service.request(name).whenComplete((material, error) -> {
                        completed.add(new CompletedRequest(key, material, error));
                        selector.wakeup();
                    });
                }
            }
            if (key.isValid() && key.isWritable() && connection.writeResponse()) {
                closeConnection(key);
            }
        } catch (IOException | RuntimeException e) {
            closeConnection(key);
        }
    }

    private void transferCompleted() {
        CompletedRequest request;
        while ((request = completed.poll()) != null) {
            SelectionKey key = request.connection();
            if (!key.isValid()) {
                continue;
            }
            ClientConnection connection = (ClientConnection) key.attachment();
            if (request.error() == null) {
                connection.prepareResponse(codec.success(request.material()));
            } else {
                System.err.println("Key generation failed: " + request.error());
                connection.prepareResponse(codec.failure("Key generation failed"));
            }
            key.interestOps(SelectionKey.OP_WRITE);
        }
    }

    private void closeConnection(SelectionKey key) {
        key.cancel();
        connections.remove(key);
        try {
            key.channel().close();
        } catch (IOException e) {
            System.err.println("Cannot close connection: " + e.getMessage());
        }
    }

    private void closeClients() {
        for (SelectionKey key : Set.copyOf(connections)) {
            closeConnection(key);
        }
    }

    @Override
    public void close() {
        running = false;
        selector.wakeup();
    }

}
