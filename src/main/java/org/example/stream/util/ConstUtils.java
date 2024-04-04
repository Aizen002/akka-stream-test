package org.example.stream.util;

import java.util.ResourceBundle;

public class ConstUtils {

    public static final String REDIS_HOST;

    public static final String REDIS_PORT;

    public static final String REDIS_SECRET;

    static {
        ResourceBundle rb = ResourceBundle.getBundle("config");
        //redis config
        REDIS_HOST = rb.getString("cache.cloud.app.host");
        REDIS_PORT = rb.getString("cache.cloud.app.port");
        REDIS_SECRET = rb.getString("cache.cloud.app.secret");
    }
}
