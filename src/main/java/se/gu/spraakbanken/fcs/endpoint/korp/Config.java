package se.gu.spraakbanken.fcs.endpoint.korp;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public final class Config {
    private static final Properties props = new Properties();

    static {
        // Allow selecting the file via -Dconfig.file=...; default to config.properties
        String configFile = System.getProperty("config.file", "config.properties");

        try (InputStream is = Config.class.getClassLoader().getResourceAsStream(configFile)) {
            if (is != null) {
                props.load(is);
                System.out.println("Loaded configuration from: " + configFile);
            } else {
                System.err.println(configFile + " not found on classpath");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static String get(String key) {
        return props.getProperty(key);
    }

    public static final String WEB_SERVICE_URL = get("web_service");
}
