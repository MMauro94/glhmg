package com.github.mmauro.glhmg;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParseException;
import com.github.mmauro.glhmg.datastruct.Corrections;
import com.github.mmauro.glhmg.datastruct.Location;
import com.github.mmauro.glhmg.datastruct.Locations;
import com.github.mmauro.glhmg.datastruct.MapParams;
import com.github.mmauro.glhmg.datastruct.MapSize;
import com.github.mmauro.glhmg.datastruct.PathParams;
import com.github.mmauro.glhmg.parse.LocationsParser;
import com.github.mmauro.glhmg.parse.ParseException;

import java.awt.Color;
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
		} else if (!dir.exists() && !dir.mkdirs()) {
			throw new IllegalArgumentException("Unable to create given directory");
		}
	});
	public final Param<String> googleStaticMapsApiKey = new Param<>(value -> {
		if (value == null) {
			throw new IllegalArgumentException("API key cannot be null");
		} else if (value.trim().isEmpty()) {
			throw new IllegalArgumentException("API key cannot be empty");
		}
	});
	public final Param<Instant> startTime = new Param<>();
	public final Param<Instant> endTime = new Param<>();
	public final Param<Duration> interpolation = new Param<>(value -> {
		if (value != null) {
			if (value.isNegative()) {
				throw new IllegalArgumentException("Interpolation cannot be negative");
			} else if (value.isZero()) {
				throw new IllegalArgumentException("Interpolation cannot be zero");
			}
		}
	});
	public final Param<Integer> mapZoom = new Param<>(value -> {
		if (value == null) {
			throw new IllegalArgumentException("Zoom cannot be null");
		} else if (value <= 0) {
			throw new IllegalArgumentException("Zoom must be greater than zero");
		}
	});
	public final Param<MapSize> mapSize = new Param<>(value -> {
		if (value == null) {
			throw new IllegalArgumentException("Size cannot be null");
		}
	});
	public final Param<Integer> mapScale = new Param<>(value -> {
		if (value == null) {
			throw new IllegalArgumentException("Scale cannot be null");
		} else if (value <= 0) {
			throw new IllegalArgumentException("Scale must be greater than zero");
		}
	});
	public final Param<Color> pathColor = new Param<>(value -> {
		if (value == null) {
			throw new IllegalArgumentException("PathColor cannot be null");
		}
	});
	public final Param<Integer> pathWeight = new Param<>(value -> {
		if (value == null) {
			throw new IllegalArgumentException("PathWeight cannot be null");
		} else if (value <= 0) {
			throw new IllegalArgumentException("PathWeight must be greater than zero");
		}
	});
	public Param<Corrections> coordinateCorrections = new Param<>();

	//@NotNull Location location, int zoom, int sizeWidth, int sizeHeight, int scale, @NotNull Color pathColor, int pathWeight

	public void execute() {
		OutUtils.standard("Parsing location file...");
		final Locations locations;
		try {
			locations = new LocationsParser(new JsonFactory().createParser(locationHistoryJson.getValue())).getLocations(location ->
					(startTime.isNull() || location.getTimestamp().compareTo(startTime.getValue()) >= 0) && (endTime.isNull() || location.getTimestamp().compareTo(endTime.getValue()) <= 0) && !coordinateCorrections.getValue().has(location.getTimestamp())
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

		final PathParams pathParams = new PathParams(pathColor.getValue(), pathWeight.getValue());
		System.out.println();
		int i = 1;
		for (Location location : withInterpolation) {
			final MapParams mapParams = new MapParams(location, mapSize.getValue(), mapZoom.getValue(), mapScale.getValue());
			OutUtils.standard("Downloading image " + i++ + "/" + withInterpolation.size() + "...");
			try {
				Utils.downloadImage(googleStaticMapsApiKey.getValue(), outputDirectory.getValue(), mapParams, pathParams);
			} catch (IOException e) {
				OutUtils.err("Error downloading image: " + e.getMessage(), 4, e);
				return;
			}
		}
	}
}
