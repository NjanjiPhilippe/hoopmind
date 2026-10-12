package com.np3tech.hoopmind.ml.explanation;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CopyOnWriteArrayList;

/** Un faux fournisseur de LLM : rejoue les réponses prévues dans l'ordre et garde les requêtes reçues. */
final class FakeLlmServer implements AutoCloseable {

    record Received(String path, Headers headers, String body) {
    }

    private record Reply(int status, String body) {
    }

    private final HttpServer server;
    private final Queue<Reply> replies = new ConcurrentLinkedQueue<>();
    final List<Received> received = new CopyOnWriteArrayList<>();

    FakeLlmServer() {
        try {
            server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        server.createContext("/", exchange -> {
            received.add(new Received(exchange.getRequestURI().getPath(), exchange.getRequestHeaders(),
                    new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8)));
            Reply reply = replies.poll();
            if (reply == null) {
                reply = new Reply(500, "{}");
            }
            byte[] body = reply.body().getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(reply.status(), body.length);
            try (OutputStream out = exchange.getResponseBody()) {
                out.write(body);
            }
        });
        server.start();
    }

    FakeLlmServer reply(int status, String body) {
        replies.add(new Reply(status, body));
        return this;
    }

    String url(String path) {
        return "http://127.0.0.1:" + server.getAddress().getPort() + path;
    }

    @Override
    public void close() {
        server.stop(0);
    }
}
