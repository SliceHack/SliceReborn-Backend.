package social.nickrest.server.pathed.http.builder;

import com.google.gson.JsonObject;
import social.nickrest.server.pathed.http.HTTPResponse;

import java.io.File;
import java.io.FileReader;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public  class HTTPResponseBuilder {

    private final HTTPResponse build = new HTTPResponse();

    public HTTPResponseBuilder type(String type) {
        build.setType(type);
        return this;
    }

    public HTTPResponseBuilder status(int statusCode) {
        build.setStatusCode(statusCode);
        return this;
    }

    public HTTPResponseBuilder body(byte[] returnBody) {
        build.setReturnBody(returnBody);
        return this;
    }

    public HTTPResponseBuilder body(File file) {
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

        build.setReturnBody(bytes);
        return this;
    }

    public HTTPResponseBuilder body(JsonObject returnBody) {
        build.setReturnBody(returnBody.toString().getBytes());
        return this;
    }

    public HTTPResponseBuilder body(InputStream stream) {
        try {
            build.setReturnBody(stream.readAllBytes());
            return this;
        } catch (Exception e) {
            throw new RuntimeException("Failed to read input stream", e);
        }
    }

    public HTTPResponseBuilder json(boolean json) {
        build.setJson(json);
        return this;
    }

    public HTTPResponse build() {
        return build;
    }
}
