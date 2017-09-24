package mmauro.glhmg;

import mmauro.glhmg.datastruct.Location;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by Mauro on 29/08/2017.
 */
public class Utils {

    /**
     * The radius of the earth in kilometers
     */
    public static final int EARTH_RADIUS_KM = 6371;

    private Utils() {
    }

    private static HttpURLConnection request(@NotNull HashMap<String, String> parameters) throws IOException {
        parameters.put("key", Main.GOOGLE_MAPS_API_KEY);
        final StringBuilder urlStr = new StringBuilder();
        urlStr.append("https://maps.googleapis.com/maps/api/staticmap");
        boolean first = true;
        for (Map.Entry<String, String> entry : parameters.entrySet()) {
            if (first) {
                urlStr.append('?');
                first = false;
            } else {
                urlStr.append('&');
            }
            urlStr.append(URLEncoder.encode(entry.getKey(), "UTF-8"));
            urlStr.append('=');
            urlStr.append(URLEncoder.encode(entry.getValue(), "UTF-8"));
        }
        System.out.println(urlStr.toString());
        try {
            return (HttpURLConnection) new URL(urlStr.toString()).openConnection();
        } catch (MalformedURLException e) {
            throw new IllegalStateException(e);
        }
    }

    public static void downloadImage(@NotNull Location location) throws IOException {
        final HashMap<String, String> params = new HashMap<>();
        params.put("center", location.getGoogleApiLatLon());
        params.put("zoom", "10");
        params.put("size", "512x512");
        params.put("path", "color:0x0000ff80|weight:5|" + location.getGoogleApiPath(100));
        final HttpURLConnection request = request(params);
        String contentType = request.getContentType();
        final String[] split = contentType.split("\\/");
        if (split.length != 2) {
            throw new IOException("Invalid content type: " + contentType);
        } else if (!split[0].equals("image")) {
            throw new IOException("Invalid content type: " + contentType);
        }
        final File outFile = new File(Main.IMAGES_OUTPUT, location.getTimestamp().toEpochMilli() + "." + split[1]);
        try (final InputStream inputStream = request.getInputStream()) {
            Files.copy(inputStream, outFile.toPath());
        }
    }

    @Contract(pure = true)
    public static int degreesDistance(int a, int b) {
        int phi = Math.abs(b - a) % 360; // This is either the distance or 360 - distance
        return phi > 180 ? 360 - phi : phi;
    }

    public interface A {
        default int ciao() {
            return 1;
        }
    }

    public static void main(String[] args) {
        System.out.println(Modifier.toString(Modifier.methodModifiers()));


    }
}
