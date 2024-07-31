package social.nickrest;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import lombok.Getter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import social.nickrest.input.CommandManager;
import social.nickrest.server.SliceServer;
import social.nickrest.server.pathed.http.HTTPResponse;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

public class Main {

    @Getter
    private static final List<JsonObject> users = new ArrayList<>();

    private static final Logger logger = LogManager.getLogger(Main.class);

    public static void main(String[] args) {
        Map<String, String> argMap = new Main().getArgumentMap(args);

        if(argMap.containsKey("port")) {
            try {
                Integer.parseInt(argMap.get("port"));
            } catch (NumberFormatException e) {
                logger.error("Invalid port number (FAILED TO START SERVER)");
                return;
            }
        }

        new CommandManager(); // this will register console input and all the commands
        SliceServer server = new SliceServer(argMap.containsKey("port") ? Integer.parseInt(argMap.get("port")) : 8080);

        server.get("/client.jar", (request) -> HTTPResponse.builder()
                .status(200)
                .type("application/java-archive")
                .body(new File("client.jar"))
                .build());

        server.get("/", (request) -> HTTPResponse.builder()
                .status(200)
                .type("text/html")
                .body("<h1>Yes, this is the web api now just move along and pretend like you didn't see anything</h1>".getBytes())
                .build());

        server.onConnection((connection) -> {
            AtomicReference<JsonObject> data = new AtomicReference<>(new JsonObject());

            connection.on("authenticate", (json) -> {
                Object[] arguments = connection.getArguments(json);

                if(arguments.length == 0) {
                    logger.info("Client tried to authenticate without any arguments");
                    return false;
                }

                JsonObject object = new Gson().fromJson(arguments[0].toString(), JsonObject.class);

                if(object.has("username") && object.has("globalName") && object.has("id")) {
                    String username = object.get("username").getAsString();
                    String globalName = object.get("globalName").getAsString();
                    String id = object.get("id").getAsString();

                    JsonObject session = object.has("session") ? object.get("session").getAsJsonObject() : new JsonObject();
                    if(!session.isEmpty()) {
                        object.add("session", session);
                    }

                    logger.info("User logged in as {} ({}) with ID {}", globalName, username, id);
                    data.set(object);
                    users.add(object);

                    JsonObject users = new JsonObject();
                    users.add("users", toJsonArray(Main.getUsers()));
                    server.emit("updateList", users);
                    return true;
                }

                logger.info("Client tried to authenticate without the correct arguments");
                return false;
            });

            connection.on("disconnect", (json) -> {
                if(!data.get().isEmpty()) {
                    logger.info("User {} ({}) disconnected", data.get().get("globalName"), data.get().get("username"));
                    users.remove(data.get());

                    JsonObject users = new JsonObject();
                    users.add("users", toJsonArray(Main.getUsers()));
                    server.emit("updateList", users);
                    return true;
                }

                logger.info("User disconnected without authenticating");
                return true;
            });

            connection.on("newSession", (json) -> {
                Object[] arguments = connection.getArguments(json);

                if(arguments.length == 0) {
                    logger.info("Client attempted to create a new session without any arguments");
                }

                String jsonString = arguments[0].toString();

                logger.info("new session created");
                if(!jsonString.startsWith("{") && !jsonString.endsWith("}")) {
                    connection.emit("error", "Invalid JSON object");
                    return false;
                }

                JsonObject object = new Gson().fromJson(jsonString, JsonObject.class);

                if(object.has("username") && object.has("uuid")) {
                    String username = object.get("username").getAsString();
                    String uuid = object.get("uuid").getAsString();

                    logger.info("User created a new session as {} with UUID {}", username, uuid);

                    if(data.get().has("session")) {
                        data.get().remove("session");
                    }

                    data.get().add("session", object);
                    return true;
                }

                return true;
            });

            connection.on("irc", (json) -> {
                Object[] arguments = connection.getArguments(json);
                JsonObject dataObject = data.get();

                if(dataObject.isEmpty()) {
                    connection.emit("error", "You must authenticate before sending messages");
                    return false;
                }

                if(arguments.length == 0) {
                    logger.info("Client tried to send an empty message");
                    return false;
                }

                String message = arguments[0].toString().trim();

                if(message.isEmpty()) {
                    connection.emit("error", "please just provide good message :(");
                    return false;
                }

                if(!dataObject.has("globalName") || !dataObject.has("username") || !dataObject.has("id")) {
                    connection.emit("error", "You must authenticate before sending messages");
                    return false;
                }

                logger.info("User {} ({}): {}", dataObject.get("globalName"), dataObject.get("username"), message);
                server.emit("irc", dataObject, message);
                return true;
            });

            return true;
        });

        server.open();
    }

    public Map<String, String> getArgumentMap(String[] args) {
        Map<String, String> map = new HashMap<>();
        for(int i = 0; i < args.length; i++) {
            String arg = args[i];
            if(arg.startsWith("--")) {
                String key = arg.substring(2);
                String value = args[i + 1];
                map.put(key, value);
                i++;
            }
        }
        return map;
    }

    public static JsonArray toJsonArray(List<JsonObject> list) {
        JsonArray array = new JsonArray();
        list.forEach(array::add);
        return array;
    }
}
