package social.nickrest.util;

import lombok.experimental.UtilityClass;

import java.io.*;

@UtilityClass
public class FileUtil {

    public InputStream getResource(String path) {
        InputStream stream =getResourceAsStream(path);

        if(stream == null) {
            throw new IllegalArgumentException("Resource not found: " + path);
        }

        return stream;
    }

    private InputStream getResourceAsStream(String resource) {
        final InputStream in = getContextClassLoader().getResourceAsStream(resource);

        return in == null ? FileUtil.class.getResourceAsStream(resource) : in;
    }

    private ClassLoader getContextClassLoader() {
        return Thread.currentThread().getContextClassLoader();
    }
}
