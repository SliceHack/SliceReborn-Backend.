package socket;

public class Main {

    public static void main(String[] args) {
        SocketConnection connection = new SocketConnection("localhost", 8080);

        connection.on("connected", (json) -> {
            connection.emit("hi", "Hello from client!");
            return false;
        });

        connection.on("hi", (json) -> {
            Object[] arguments = connection.getArguments(json);

            for (Object argument : arguments) {
                SocketConnection.getLogger().info("Server says: {}", argument);
            }

            return false;
        });

        connection.start();
    }

}