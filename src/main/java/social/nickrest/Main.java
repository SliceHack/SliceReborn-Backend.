package social.nickrest;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import social.nickrest.server.SliceServer;

import java.util.HashMap;
import java.util.Map;

public class Main {

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

        SliceServer server = new SliceServer(argMap.containsKey("port") ? Integer.parseInt(argMap.get("port")) : 8080);

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
}
