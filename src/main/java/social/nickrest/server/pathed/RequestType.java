package social.nickrest.server.pathed;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public enum RequestType {
    GET("GET"), POST("POST"),
    PUT("PUT"), DELETE("DELETE");

    private final String name;
}
