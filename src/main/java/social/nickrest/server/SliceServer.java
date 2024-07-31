package social.nickrest.server;

import com.google.gson.Gson;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import social.nickrest.server.pathed.HTTPRequest;
import social.nickrest.server.pathed.HTTPResponse;
import social.nickrest.server.pathed.RequestType;
import social.nickrest.util.FileUtil;

import java.io.IOException;
import java.io.InputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;
import java.util.function.Function;

@Getter
public class SliceServer {

    @Getter
    private static SliceServer instance;

    @Getter
    private static final Logger logger = LogManager.getLogger(SliceServer.class);
    private final Gson gson = new Gson();

    private final List<Connection> connections = new ArrayList<>();
    private final List<Function<Connection, Boolean>> connectionListeners = new ArrayList<>();

    // there has to be a better way to do this instead of making 4 of these motherfuckers
    private final HashMap<String, Function<HTTPRequest, HTTPResponse>> get = new HashMap<>();
    private final HashMap<String, Function<HTTPRequest, HTTPResponse>> post = new HashMap<>();
    private final HashMap<String, Function<HTTPRequest, HTTPResponse>> put = new HashMap<>();
    private final HashMap<String, Function<HTTPRequest, HTTPResponse>> delete = new HashMap<>();

    private ServerSocket serverSocket;

    private final int port;

    public SliceServer(int port) {
        if(instance != null) {
            throw new IllegalStateException("Server already exists");
        }

        instance = this;
        this.port = port;
    }

    public void open() {
        try {
            serverSocket = new ServerSocket(port);

            SliceServer.getLogger().info("Server started on port {}", port);

            while (!serverSocket.isClosed()) {
                Socket socket = serverSocket.accept();
                Connection connection = getConnection(socket);

                connections.add(connection);
                new Thread(connection).start();
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void onConnection(Function<Connection, Boolean> function) {
        connectionListeners.add(function);
    }

    public void get(String path, Function<HTTPRequest, HTTPResponse> callback) { request(RequestType.GET, path, callback); }
    public void post(String path, Function<HTTPRequest, HTTPResponse> callback) { request(RequestType.POST, path, callback); }
    public void put(String path, Function<HTTPRequest, HTTPResponse> callback) { request(RequestType.PUT, path, callback); }
    public void delete(String path, Function<HTTPRequest, HTTPResponse> callback) { request(RequestType.DELETE, path, callback); }

    public HTTPResponse get(String path) { return respond("GET", new HTTPRequest(path, null, null, null, null)); }
    public HTTPResponse post(String path) { return respond("POST", new HTTPRequest(path, null, null, null, null)); }
    public HTTPResponse put(String path) { return respond("PUT", new HTTPRequest(path, null, null, null, null)); }
    public HTTPResponse delete(String path) { return respond("DELETE", new HTTPRequest(path, null, null, null, null)); }

    public void request(RequestType type, String path, Function<HTTPRequest, HTTPResponse> callback) {
        switch (type) {
            case GET:
                get.put(path, callback);
                break;
            case POST:
                post.put(path, callback);
                break;
            case PUT:
                put.put(path, callback);
                break;
            case DELETE:
                delete.put(path, callback);
                break;
        }
    }

    public HTTPResponse respond(String type, HTTPRequest request) {
        return switch (type) {
            case "GET" -> get.get(request.getPath()) != null ? get.get(request.getPath()).apply(request) : null;
            case "POST" -> post.get(request.getPath()) != null ? post.get(request.getPath()).apply(request) : null;
            case "PUT" -> put.get(request.getPath()) != null ? put.get(request.getPath()).apply(request) : null;
            case "DELETE" -> delete.get(request.getPath()) != null ? delete.get(request.getPath()).apply(request) : null;
            default -> null;
        };
    }

    private Connection getConnection(Socket socket) throws IOException {
        Connection connection = new Connection(this, socket, socket.getInputStream(), socket.getOutputStream(), connections.size() + 1);

        connection.on("disconnect", (json) -> {
            try {
                if (!socket.isClosed()) {
                    socket.close();
                }
            } catch (Exception e) {
                logger.error("Failed to close connection: {}", e.getMessage());
                logger.error(e);
            }

            connections.remove(connection);
            return false;
        });

        connectionListeners.forEach(listener -> listener.apply(connection));
        return connection;
    }

    public void emit(String message) {
        connections.stream().filter(Objects::nonNull).forEach(connection -> connection.emit(message));
    }

    public void emit(String message, Object... args) {
        connections.stream().filter(Objects::nonNull).forEach(connection -> connection.emit(message, args));
    }

}
