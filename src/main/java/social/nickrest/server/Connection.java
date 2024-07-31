package social.nickrest.server;

import com.google.gson.JsonObject;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import social.nickrest.server.pathed.HTTPStatus;
import social.nickrest.server.pathed.HTTPRequest;
import social.nickrest.server.pathed.HTTPResponse;

import java.io.*;
import java.net.Socket;
import java.net.SocketException;
import java.util.*;
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
            if(connectedSocket.isClosed()) {
                return;
            }

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
                    boolean browserLikeRequest = false;
                    int read = inputStream.read(buffer);

                    outputStream.write("HTTP/1.1 200 OK\\nContent-Type: text/html; charset=UTF-8".getBytes());
                    outputStream.write("<html><body><h1>200 OK</h1></body></html>".getBytes());
                    String message = new String(buffer, 0, read);
                    String[] requestTypes = new String[]{
                            "GET", "POST",
                            "PUT", "DELETE",
                            "PATCH", "HEAD",
                            "OPTIONS", "CONNECT",
                            "TRACE"
                    };

                    if (Arrays.stream(requestTypes).anyMatch(message::startsWith)) {
                        browserLikeRequest = true;

                        String header = message.split("\n")[0];
                        boolean isRoot = header.split(" ").length == 2;

                        String path = isRoot ? "/" : header.split(" ")[1];
                        String type = header.split(" ")[0];

                        HTTPRequest request = getHttpRequest(message, path);

                        HTTPResponse response = switch (type) {
                            case "GET" -> SliceServer.getInstance().get(path);
                            case "POST" -> SliceServer.getInstance().post(path);
                            case "PUT" -> SliceServer.getInstance().put(path);
                            case "DELETE" -> SliceServer.getInstance().delete(path);
                            default -> null;
                        };

                        if(response == null) {
                            outputStream.write("Content-Type: text/html; charset=UTF-8\n".getBytes());
                            outputStream.write("\n".getBytes());
                            outputStream.write("<html><body><h1>404 Not Found</h1></body></html>".getBytes());
                            outputStream.flush();
                            outputStream.close();
                            return;
                        }

                        int statusCode = response.getStatusCode();
                        HTTPStatus statusType = HTTPStatus.fromCode(statusCode);

                        outputStream.write(("HTTP/1.1 " + statusCode + " " + statusType + "\n").getBytes());
                        if(response.isJson()) {
                            outputStream.write("Content-Type: application/json\n".getBytes());
                        } else if (path.endsWith(".css")) {
                            outputStream.write("Content-Type: text/css; charset=UTF-8\n".getBytes());
                        } else if(path.endsWith(".js")) {
                            outputStream.write("Content-Type: text/javascript; charset=UTF-8\n".getBytes());
                        } else if(path.endsWith(".png")) {
                            outputStream.write("Content-Type: image/png\n".getBytes());
                        } else if(path.endsWith(".jpg") || path.endsWith(".jpeg")) {
                            outputStream.write("Content-Type: image/jpeg\n".getBytes());
                        } else if(path.endsWith(".gif")) {
                            outputStream.write("Content-Type: image/gif\n".getBytes());
                        } else if(path.endsWith(".html") || !path.contains(".")) {
                            outputStream.write("Content-Type: text/html; charset=UTF-8\n".getBytes());
                        } else {
                            outputStream.write("Content-Type: application/octet-stream\n".getBytes());
                        }

                        outputStream.write("\n".getBytes());
                        outputStream.write(response.getReturnBody());
                        outputStream.flush();
                        outputStream.close(); // so the client knows we're done
                        continue;
                    }

                    if(browserLikeRequest) continue;

                    if (!message.startsWith("{") || !message.endsWith("}")) {
                        SliceServer.getLogger().warn("Received invalid message from client: {}", message);
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

    private HTTPRequest getHttpRequest(String message, String path) {
        Map<String, String> headers = new HashMap<>();
        for (String line : message.split("\n")) {
            if (line.contains(":")) {
                String key = line.split(":")[0];
                String value = line.split(":")[1];
                headers.put(key, value);
            }
        }
        return new HTTPRequest(path, message, headers, inputStream, outputStream);
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
