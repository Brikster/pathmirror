package ru.brikster.pathmirror.util;

import ru.brikster.pathmirror.PathMirrorAgent;

import java.io.*;
import java.net.URL;
import java.net.URLConnection;

public class ConfigUtil {

    public static void saveDefaultConfig() {
        String resourcePath = "pathmirror.yml";

        File outFile = new File(resourcePath);
        InputStream in = getResource(resourcePath);

        try {
            if (!outFile.exists()) {
                OutputStream out = new FileOutputStream(outFile);
                byte[] buf = new byte[1024];
                int len;
                while ((len = in.read(buf)) > 0) {
                    out.write(buf, 0, len);
                }
                out.close();
                in.close();
            }
        } catch (IOException ignored) {}
    }

    private static InputStream getResource(String filename) {
        if (filename == null) {
            throw new IllegalArgumentException("Filename cannot be null");
        }

        try {
            URL url = PathMirrorAgent.class.getClassLoader().getResource(filename);

            if (url == null) {
                return null;
            }

            URLConnection connection = url.openConnection();
            connection.setUseCaches(false);
            return connection.getInputStream();
        } catch (IOException ex) {
            return null;
        }
    }

}
