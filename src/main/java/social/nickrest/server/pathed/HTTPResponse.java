package social.nickrest.server.pathed;

import com.google.gson.JsonObject;
import lombok.Getter;
import lombok.Setter;

import java.io.File;
import java.io.FileReader;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

@Getter @Setter
public class HTTPResponse {
    private int statusCode;
    private byte[] returnBody;
    private boolean json;

    public static HTTPResponse create() {
        return new HTTPResponse();
    }

    public HTTPResponse status(int statusCode) {
        this.statusCode = statusCode;
        return this;
    }

    public HTTPResponse body(byte[] returnBody) {
        this.returnBody = returnBody;
        return this;
    }

    public HTTPResponse body(File file) {
        if(!file.exists()) {
            throw new RuntimeException("File does not exist");
        }

        List<Integer> buffer = new ArrayList<>();

        try(FileReader reader = new FileReader(file)) {
            buffer.add(reader.read());
        } catch (Exception e) {
            throw new RuntimeException("Failed to read file", e);
        }

        byte[] bytes = new byte[buffer.size()];
        for(int i = 0; i < buffer.size(); i++) {
            bytes[i] = buffer.get(i).byteValue();
        }

        this.returnBody = bytes;
        return this;
    }

    public HTTPResponse body(JsonObject returnBody) {
        this.returnBody = returnBody.toString().getBytes();
        return this;
    }

    public HTTPResponse body(InputStream stream) {
        try {
            this.returnBody = stream.readAllBytes();
            return this;
        } catch (Exception e) {
            throw new RuntimeException("Failed to read input stream", e);
        }
    }

    public HTTPResponse json(boolean json) {
        this.json = json;
        return this;
    }

    public HTTPResponse build() {
        return this;
    }

}
