package social.nickrest.server;

import com.google.gson.JsonObject;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.SocketException;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.function.Function;

@RequiredArgsConstructor
@Getter
public class Connection implements Runnable {

    private final HashMap<String, List<Function<JsonObject, Boolean>>> params = new HashMap<>();

    private final SliceServer parent;
    private final Socket connectedSocket;

    private final InputStream inputStream;
    private final OutputStream outputStream;

    private final int id;

    @Override
    public void run() {
        try {
            this.connectedSocket.setTcpNoDelay(true);
        } catch (SocketException e) {
            throw new RuntimeException(e);
        }

        on("disconnect", (json) -> {
            parent.getConnections().remove(this);
            return false;
        });

        communicate();
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

    public void call(String path, JsonObject object) {
        try {
            if(!params.containsKey(path))
                return;

            params.get(path).forEach(callback -> callback.apply(object));
        } catch (Exception e) {
            SliceServer.getLogger().error("Failed to call event", e);
        }
    }

    public void on(String path, Function<JsonObject, Boolean> callback) {
        if (!params.containsKey(path)) {
            params.put(path, new ArrayList<>());
        }
        params.get(path).add(callback);
    }

    public void send(JsonObject object) {
        try {
            outputStream.write(object.toString().getBytes());
        } catch (Exception e) {
            SliceServer.getLogger().error("Failed to send data to client", e);
        }
    }

    public void communicate() {
        JsonObject connect = new JsonObject();
        connect.addProperty("message", "connect");
        connect.add("data", new JsonObject());

        call("connect", connect);
        while (connectedSocket.isConnected()) {
            try {
                byte[] buffer = new byte[1024];
                try {
                    int read = inputStream.read(buffer);

                    if (read == -1) {
                        JsonObject object = new JsonObject();
                        object.addProperty("message", "Connection closed by peer");
                        object.add("data", new JsonObject());

                        call("disconnect", object);
                        break;
                    }

                    // check if it's a valid json object
                    String jsonFormatted = new String(buffer, 0, read);
                    if (!jsonFormatted.startsWith("{") || !jsonFormatted.endsWith("}")) {
                        SliceServer.getLogger().warn("Received invalid message from client: {}", jsonFormatted);
                        continue;
                    }

                    JsonObject jsonToSend = parent.getGson().fromJson(new String(buffer, 0, read), JsonObject.class);

                    call(jsonToSend.get("message").getAsString(), jsonToSend);
                } catch (SocketException e) {
                    if(e.getMessage().equalsIgnoreCase("connection reset")) {
                        JsonObject jsonToSend = new JsonObject();
                        jsonToSend.addProperty("message", "Connection reset by peer");
                        jsonToSend.add("data", new JsonObject());

                        call("disconnect", jsonToSend);
                        break;
                    }
                }
            } catch (Exception e) {
                SliceServer.getLogger().error("Failed to read/write from/to socket", e);
                break;
            }
        }

        call("disconnect", new JsonObject());

        try {
            connectedSocket.close();
        } catch (Exception e) {
            SliceServer.getLogger().error("Failed to close socket", e);
        }

        parent.getConnections().remove(this);
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
