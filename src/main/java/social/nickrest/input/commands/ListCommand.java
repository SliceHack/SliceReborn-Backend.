package social.nickrest.input.commands;

import com.google.gson.JsonObject;
import social.nickrest.Main;
import social.nickrest.input.Command;
import social.nickrest.input.CommandInfo;
import social.nickrest.server.SliceServer;

import java.util.List;

@CommandInfo(name = "list", aliases = {"ls"})
public class ListCommand extends Command {

    @Override
    public void handle(String name, String[] args) {
        StringBuilder builder = new StringBuilder();
        List<JsonObject> users = Main.getUsers();

        for(JsonObject user : users) {
            builder.append(user.get("globalName").getAsString()).append(", ");
        }

        SliceServer.getLogger().info("Users: {}", builder.toString());
    }

}
