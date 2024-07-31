package social.nickrest.server.pathed;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.Map;

@AllArgsConstructor
@Getter @Setter
public class HTTPRequest {
    private String path;
    private String body;
    private Map<String, String> headers;
    private InputStream inputStream;
    private OutputStream outputStream;

    public JsonObject getBodyAsJsonObject() {
        return new Gson().fromJson(body, JsonObject.class);
    }
}
