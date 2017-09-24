package mmauro.glhmg;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParseException;
import mmauro.glhmg.datastruct.Location;
import mmauro.glhmg.datastruct.Locations;
import mmauro.glhmg.parse.LocationsParser;
import mmauro.glhmg.parse.ParseException;

import java.io.File;
import java.io.IOException;
import java.time.Duration;
import java.time.Instant;

public class Executor {


    public final Param<File> locationHistoryJson = new Param<>(file -> {
        if (!file.exists()) {
            throw new IllegalArgumentException("The given file doesn't exists");
        } else if (!file.isFile()) {
            throw new IllegalArgumentException("The given path is not a file");
        } else if (!file.canRead()) {
            throw new IllegalArgumentException("Unable to read the given file");
        }
    });
    public final Param<File> outputDirectory = new Param<>(dir -> {
        if (dir.exists() && !dir.isDirectory()) {
            throw new IllegalArgumentException("The given path is not a directory");
        } else if (!dir.mkdirs()) {
            throw new IllegalArgumentException("Unable to create given directory");
        }
    });
    public final Param<String> googleStaticMapsApiKey = new Param<>(value -> {
        if (value == null) {
            throw new IllegalArgumentException("API key is required");
        } else if (value.trim().isEmpty()) {
            throw new IllegalArgumentException("API key cannot be empty");
        }
    });
    public final Param<Instant> startTime = new Param<>();
    public final Param<Instant> endTime = new Param<>();
    public final Param<Duration> interpolation = new Param<>();


    public void execute() {
        OutUtils.standard("Parsing location file...");
        final Locations locations;
        try {
            locations = new LocationsParser(new JsonFactory().createParser(locationHistoryJson.getValue())).getLocations(location ->
                    (location.getTimestamp().compareTo(startTime.getValue()) >= 0) && (location.getTimestamp().compareTo(endTime.getValue()) <= 0)
            );
        } catch (ParseException | JsonParseException e) {
            OutUtils.err("There has been an error parsing the provided JSON file: " + e.getMessage(), 1, e);
            return;
        } catch (IOException e) {
            OutUtils.err("There has been an error reading the provided JSON file: " + e.getMessage(), 2, e);
            return;
        }
        if (locations == null) {
            OutUtils.err("No locations found in given JSON file", 3);
            return;
        }
        OutUtils.standard("Locations after filtering: " + locations.size());
        final Locations withInterpolation;
        if (interpolation.getValue() == null) {
            withInterpolation = locations;
        } else {
            withInterpolation = locations.interpolateWithStaticDuration(interpolation.getValue());
            OutUtils.standard("Locations after interpolation: " + withInterpolation.size());
        }

        for (Location location : withInterpolation) {
            try {
                Utils.downloadImage(location);
            } catch (IOException e) {
                OutUtils.err("Error downloading image: " + e.getMessage(), 4, e);
                return;
            }
        }
    }

    public static class MissingParamException extends IllegalStateException {
        public MissingParamException(String value) {
            super("Missing " + value);
        }
    }
}
