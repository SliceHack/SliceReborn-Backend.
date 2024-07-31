package social.nickrest.server.pathed.http;

import lombok.Getter;
import lombok.Setter;
import social.nickrest.server.pathed.http.builder.HTTPResponseBuilder;

@Getter @Setter
public class HTTPResponse {
    private int statusCode;
    private byte[] returnBody;
    private boolean json;
    private String type;

    public static HTTPResponseBuilder builder() {
        return new HTTPResponseBuilder();
    }


}
