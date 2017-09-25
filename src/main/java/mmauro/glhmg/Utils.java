package mmauro.glhmg;

import mmauro.glhmg.datastruct.MapParams;
import mmauro.glhmg.datastruct.PathParams;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.file.Files;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.temporal.ChronoField;
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

    private static final DateTimeFormatter OUTPUT_FORMATTER = new DateTimeFormatterBuilder()
            .appendValue(ChronoField.YEAR, 4)
            .appendLiteral('-')
            .appendValue(ChronoField.MONTH_OF_YEAR, 2)
            .appendLiteral('-')
            .appendValue(ChronoField.DAY_OF_MONTH, 2)
            .appendLiteral(' ')
            .appendValue(ChronoField.HOUR_OF_DAY, 2)
            .appendLiteral('.')
            .appendValue(ChronoField.MINUTE_OF_HOUR, 2)
            .appendLiteral('.')
            .appendValue(ChronoField.SECOND_OF_MINUTE, 2)
            .appendLiteral('.')
            .appendValue(ChronoField.MILLI_OF_SECOND, 3)
            .toFormatter();

    private Utils() {
    }

    private static HttpURLConnection request(@NotNull String apiKey, @NotNull HashMap<String, String> parameters) throws IOException {
        parameters.put("key", apiKey);
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

    @NotNull
    public static String colorToRGBAString(@NotNull Color color) {
        return String.format("%02X%02X%02X%02X", color.getRed(), color.getGreen(), color.getBlue(), color.getAlpha());
    }

    static int i=0;
    public static void downloadImage(@NotNull String apiKey, @NotNull File outDir, @NotNull MapParams mapParams, @NotNull PathParams pathParams) throws IOException {
        final HashMap<String, String> params = new HashMap<>();
        params.put("center", mapParams.getLocation().getGoogleApiLatLon());
        params.put("zoom", String.valueOf(mapParams.getZoom()));
        params.put("scale", String.valueOf(mapParams.getScale()));
        params.put("size", mapParams.getSize().toString());
        params.put("path", "color:0x" + colorToRGBAString(pathParams.getPathColor()) + "|weight:" + pathParams.getPathWeight() + "|" + mapParams.getLocation().getGoogleApiPath(mapParams));

        final HttpURLConnection request = request(apiKey, params);
        if (request.getResponseCode() != 200) {
            throw new IOException(request.getResponseMessage());
        }
        String contentType = request.getContentType();
        final String[] split = contentType.split("\\/");
        if (split.length != 2) {
            throw new IOException("Invalid content type: " + contentType);
        } else if (!split[0].equals("image")) {
            throw new IOException("Invalid content type: " + contentType);
        }
        final File outFile = new File(outDir, mapParams.getLocation().getTimestamp().atOffset(ZoneOffset.UTC).format(OUTPUT_FORMATTER) + "." + split[1]);
        try (final InputStream inputStream = request.getInputStream()) {
            Files.copy(inputStream, outFile.toPath());
        }
    }

    @Contract(pure = true)
    public static int degreesDistance(int a, int b) {
        int phi = Math.abs(b - a) % 360; // This is either the distance or 360 - distance
        return phi > 180 ? 360 - phi : phi;
    }
}
