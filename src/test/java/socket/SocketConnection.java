package socket;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import lombok.Getter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;

public class SocketConnection implements Runnable {

    public static final Gson GSON = new Gson();

    @Getter
    private static final Logger logger = LogManager.getLogger(SocketConnection.class);
    private final HashMap<String, List<Function<JsonObject, Boolean>>> params = new HashMap<>();

    private final InputStream inputStream;
    private final OutputStream outputStream;

    private final Socket socket;

    public SocketConnection(String address, int port) {
        try {
            socket = new Socket(address, port);
            inputStream = socket.getInputStream();
            outputStream = socket.getOutputStream();
            socket.setTcpNoDelay(true);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void start() {
        new Thread(this).start();
    }

    public void communicate() {
        logger.info("Connection established with server");

        call("connected", new JsonObject());
        while (!socket.isClosed()) {
            try {
                byte[] buffer = new byte[1024];
                int read = socket.getInputStream().read(buffer);

                if(read == -1) {
                    break;
                }

                String message = new String(buffer, 0, read);

                String jsonFormatted = new String(buffer, 0, read);
                if (!jsonFormatted.startsWith("{") || !jsonFormatted.endsWith("}")) {
                    logger.warn("Received invalid message from client: {}", jsonFormatted);
                    continue;
                }

                JsonObject jsonToSend = GSON.fromJson(new String(buffer, 0, read), JsonObject.class);

                call(jsonToSend.get("message").getAsString(), jsonToSend);
            } catch (Exception e) {
                logger.error("Failed to read message", e);
            }
        }

        call("disconnected", new JsonObject());
    }

    public void call(String path, JsonObject object) {
        try {
            if(!params.containsKey(path))
                return;

            params.get(path).forEach(callback -> callback.apply(object));
        } catch (Exception e) {
            logger.error("Failed to call event", e);
        }
    }

    public void emit(String message, Object... args) {
        JsonObject object = new JsonObject();
        object.addProperty("message", message);

        JsonObject data = new JsonObject();
        for(int i = 0; i < args.length; i++) {
            data.addProperty("arg" + i, args[i].toString());
        }

        object.add("data", data);
        send(object);
    }

    public void send(JsonObject object) {
        try {
            outputStream.write(object.toString().getBytes());
        } catch (Exception e) {
            logger.error("Failed to send message", e);
        }
    }

    @Override
    public void run() {
        communicate();
    }

    public void on(String path, Function<JsonObject, Boolean> callback) {
        if (!params.containsKey(path)) {
            params.put(path, new ArrayList<>());
        }
        params.get(path).add(callback);
    }

    public Object[] getArguments(JsonObject object) {
        if(!object.has("data"))
            return new Object[0];

        return object.get("data").getAsJsonObject().entrySet().stream().map(entry -> {
            if(entry.getValue().isJsonPrimitive()) {
                return entry.getValue().getAsString();
            } else if(entry.getValue().isJsonArray()) {
                List<Object> list = new ArrayList<>();
                for (int i = 0; i < entry.getValue().getAsJsonArray().size(); i++) {
                    list.add(entry.getValue().getAsJsonArray().get(i));
                }
                return list;
            } else if(entry.getValue().isJsonObject()) {
                return entry.getValue().getAsJsonObject();
            } else {
                return null;
            }
        }).filter(Objects::nonNull).toArray();
    }


}
