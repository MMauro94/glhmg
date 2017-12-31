package com.github.mmauro.glhmg.datastruct;

import com.github.mmauro.glhmg.Utils;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.text.DecimalFormat;

public final class LatLng {

	private static final DecimalFormat FORMATTER = new DecimalFormat("0.0000");

	private final double latitude, longitude;

	public LatLng(double latitude, double longitude) {
		this.latitude = latitude;
		this.longitude = longitude;
	}

	@Contract(pure = true)
	public double getLatitude() {
		return latitude;
	}

	@Contract(pure = true)
	@NotNull
	public String getLatitudeStr() {
		return FORMATTER.format(getLatitude());
	}

	@Contract(pure = true)
	public double getLongitude() {
		return longitude;
	}

	@Contract(pure = true)
	@NotNull
	public String getLongitudeStr() {
		return FORMATTER.format(getLongitude());
	}

	/**
	 * Calculates th distance, in meters, between two locations
	 *
	 * @param l1 the first location
	 * @param l2 the second location
	 * @return the number of meters separating the two locations
	 */
	@Contract(pure = true)
	public static double getMetersDistance(@NotNull LatLng l1, @NotNull LatLng l2) {
		final double lat1 = l1.getLatitude();
		final double lat2 = l2.getLatitude();
		final double lon1 = l1.getLongitude();
		final double lon2 = l2.getLongitude();

		double latDistance = Math.toRadians(lat2 - lat1);
		double lonDistance = Math.toRadians(lon2 - lon1);
		double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
				+ Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
				* Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);
		double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
		double distance = Utils.EARTH_RADIUS_KM * c * 1000; // convert to meters


		distance = Math.pow(distance, 2);

		return Math.sqrt(distance);
	}

	@NotNull
	@Contract(pure = true)
	public static LatLng interpolate(@NotNull LatLng a, @NotNull LatLng b, float balancing) {
		return new LatLng(
				a.getLatitude() + (b.getLatitude() - a.getLatitude()) * balancing,
				a.getLongitude() + (b.getLongitude() - a.getLongitude()) * balancing
		);
	}

	@NotNull
	@Contract(pure = true)
	public WorldCoordinate toWorldCoordinate() {
		double siny = Math.sin(getLatitude() * Math.PI / 180);

		// Truncating to 0.9999 effectively limits latitude to 89.189. This is
		// about a third of a tile past the edge of the world tile.
		siny = Math.min(Math.max(siny, -0.9999), 0.9999);

		return new WorldCoordinate(
				256 * (0.5 + getLongitude() / 360),
				256 * (0.5 - Math.log((1 + siny) / (1 - siny)) / (4 * Math.PI))
		);
	}
}
