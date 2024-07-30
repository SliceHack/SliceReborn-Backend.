package social.nickrest.server;

import com.google.gson.Gson;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

@RequiredArgsConstructor
@Getter
public class SliceServer {

    @Getter
    private static final Logger logger = LogManager.getLogger(SliceServer.class);
    private final Gson gson = new Gson();

    private final List<Connection> connections = new ArrayList<>();
    private final List<Function<Connection, Boolean>> connectionListeners = new ArrayList<>();

    private ServerSocket serverSocket;

    @NonNull
    private int port;

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

    private Connection getConnection(Socket socket) throws IOException {
        Connection connection = new Connection(this, socket, socket.getInputStream(), socket.getOutputStream(), connections.size() + 1);

        connection.on("connect", (json) -> {
            logger.info("Connection established with client #{}", connection.getId());
            return false;
        });

        connection.on("disconnect", (json) -> {
            logger.info("Connection closed with client #{}", connection.getId());

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
        connections.forEach(connection -> connection.emit(message));
    }

    public void emit(String message, Object... args) {
        connections.forEach(connection -> connection.emit(message, args));
    }
}
