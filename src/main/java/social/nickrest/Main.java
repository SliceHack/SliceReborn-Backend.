package social.nickrest;

import social.nickrest.server.SliceServer;

public class Main {

    public static void main(String[] args) {
        new SliceServer(8080).open();
    }
}
