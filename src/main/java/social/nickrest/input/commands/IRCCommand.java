package social.nickrest.input.commands;

import com.google.gson.JsonObject;
import social.nickrest.input.Command;
import social.nickrest.input.CommandInfo;
import social.nickrest.server.SliceServer;

@CommandInfo(name = "irc", aliases = {"chat", "say"})
public class IRCCommand extends Command {

    @Override
    public void handle(String name, String[] args) {
        String message = String.join(" ", args);
        JsonObject userObject = new JsonObject();

        userObject.addProperty("globalName", "§cServer Console");
        SliceServer.getInstance().emit("irc", userObject, message);

        SliceServer.getLogger().info("Server Console: {}", message);
    }

}
