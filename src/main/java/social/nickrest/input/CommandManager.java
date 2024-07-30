package social.nickrest.input;

import social.nickrest.input.commands.IRCCommand;
import social.nickrest.input.commands.ListCommand;
import social.nickrest.server.SliceServer;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

public class CommandManager implements Runnable {

    public static List<Class<? extends Command>> commandsClasses = List.of(
            IRCCommand.class,
            ListCommand.class
    );

    private List<Command> commands = new ArrayList<>();

    private static boolean running = false;

    public CommandManager() {
        commandsClasses.forEach(commandClass -> {
            try {
                commands.add(commandClass.getConstructor().newInstance());
            } catch (InstantiationException | IllegalAccessException | NoSuchMethodException | InvocationTargetException e) {
                throw new RuntimeException("Failed to create command " + commandClass.getName(), e);
            }
        });

        running = true;
        new Thread(this).start();
    }

    @Override
    public void run() {
        while (running) {
            try {
                BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));

                String input = reader.readLine();

                String[] args = input.split(" ");
                String commandName = args[0];
                String[] commandArgs = new String[args.length - 1];
                System.arraycopy(args, 1, commandArgs, 0, commandArgs.length);

                boolean found = false;
                for (Command command : commands) {
                    if (command.check(commandName)) {
                        command.handle(commandName, commandArgs);
                        found = true;
                        break;
                    }
                }

                if(!found) {
                    SliceServer.getLogger().info("Unknown command: {}", commandName);
                }
            } catch (Exception e) {
                throw new RuntimeException("Failed to read console input", e);
            }
        }
    }

}
