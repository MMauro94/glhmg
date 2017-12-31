package com.github.mmauro.glhmg.datastruct;

import com.github.mmauro.glhmg.Utils;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.time.Instant;
import java.util.Iterator;
import java.util.TreeSet;

/**
 * Created by Mauro on 28/08/2017.
 */
public final class Location implements Comparable<Location> {

	@Nullable
	private Location previous, next;
	@NotNull
	private final Instant timestamp;
	@NotNull
	private final LatLng latLng;
	private final int accuracy;
	@Nullable
	private final Integer altitude, heading;

	private Location(@NotNull Instant timestamp, @NotNull LatLng latLng, int accuracy, @Nullable Integer altitude, @Nullable Integer heading) {
		if (accuracy < 0) {
			throw new IllegalArgumentException("accuracy < 0: " + accuracy);
		} else if (heading != null && heading < 0) {
			throw new IllegalArgumentException("heading < 0: " + heading);
		} else if (heading != null && heading >= 360) {
			throw new IllegalArgumentException("heading >= 360: " + heading);
		}
		this.timestamp = timestamp;
		this.latLng = latLng;
		this.accuracy = accuracy;
		this.altitude = altitude;
		this.heading = heading;
	}

	@Contract(pure = true)
	@NotNull
	public Instant getTimestamp() {
		return timestamp;
	}

	@NotNull
	@Contract(pure = true)
	public LatLng getLatLng() {
		return latLng;
	}

	@Contract(pure = true)
	public int getAccuracy() {
		return accuracy;
	}

	@Contract(pure = true)
	public int getAltitude() {
		if (altitude == null) {
			throw new IllegalStateException("no altitude");
		}
		return altitude;
	}

	@Contract(pure = true)
	@Nullable
	public Integer optAltitude() {
		return altitude;
	}

	@Contract(pure = true)
	public boolean hasAltitude() {
		return altitude != null;
	}

	@Contract(pure = true)
	@Nullable
	public Integer optHeading() {
		return heading;
	}

	@Contract(pure = true)
	public boolean hasHeading() {
		return heading != null;
	}

	@Contract(pure = true)
	public int getHeading() {
		if (heading == null) {
			throw new IllegalStateException("no heading");
		}
		return heading;
	}

	@NotNull
	public String getGoogleApiLatLon() {
		return latLng.getLatitudeStr() + "," + latLng.getLongitudeStr();
	}

	public void setPrevious(@Nullable Location previous) {
		this.previous = previous;
	}

	public void setNext(@Nullable Location next) {
		this.next = next;
	}

	@Contract(pure = true)
	public boolean isOrphan() {
		return next == null && previous == null;
	}

	/**
	 * @return a new location where all its values are copied, except for <code>next</code> and <code>previous</code>, which are set to null
	 */
	@NotNull
	@Contract(pure = true)
	public Location asOrphan() {
		return new Location(timestamp, latLng, accuracy, altitude, heading);
	}

	public boolean isVisible(@NotNull MapParams mapParams) {
		return isVisible(mapParams, 1.01f);
	}

	public boolean isVisible(@NotNull MapParams mapParams, float tollerance) {
		final double scale = Math.pow(2, mapParams.getZoom());
		final MapSize mapSize = mapParams.getSize();

		final WorldCoordinate centerPx = mapParams.getLocation().getLatLng().toWorldCoordinate();
		final WorldCoordinate nePoint = new WorldCoordinate(centerPx.getX() + ((mapSize.width / 2d) / scale) * tollerance, centerPx.getY() - ((mapSize.height / 2d) / scale) * tollerance);
		final WorldCoordinate swPoint = new WorldCoordinate(centerPx.getX() - ((mapSize.width / 2d) / scale) * tollerance, centerPx.getY() + ((mapSize.height / 2d) / scale) * tollerance);

		return latLng.toWorldCoordinate().isInBounds(nePoint, swPoint);
	}

	@NotNull
	@Contract(pure = true)
	@Override
	public String toString() {
		return "Location{" +
				"timestamp=" + timestamp +
				", latLng=" + latLng +
				", accuracy=" + accuracy +
				", altitude=" + altitude +
				", heading=" + heading +
				'}';
	}

	/**
	 * Compares the timestamps
	 * {@inheritDoc}
	 */
	@Contract(pure = true)
	@Override
	public int compareTo(@NotNull Location o) {
		return timestamp.compareTo(o.timestamp);
	}

	@Contract(pure = true)
	@NotNull
	public Location getRoot() {
		if (previous == null) {
			return this;
		} else {
			return previous.getRoot();
		}
	}

	@Contract(pure = true)
	@NotNull
	public Location getPrevious(int steps) {
		if (steps < 0) {
			throw new IllegalArgumentException("steps < 0");
		} else if (steps == 0 || previous == null) {
			return this;
		} else {
			return previous.getPrevious(steps - 1);
		}
	}

	/**
	 * @return the path to pass to the Google Static Map APIs
	 */
	@NotNull
	public String getGoogleApiPath(@NotNull MapParams mapParams) {
		return getGoogleApiPath(mapParams, 200);
	}

	@NotNull
	public String getGoogleApiPath(@NotNull MapParams mapParams, int limit) {
		if (previous == null) {
			return getGoogleApiLatLon();
		} else {
			final TreeSet<Location> arr = new TreeSet<>();
			Location loc = getRoot();
			while (loc != previous && !loc.isVisible(mapParams)) {
				loc = loc.getNext();
			}

			//This while adds only visible points and entry and exit points to shadow zones
			boolean lastVisible = true, precAdded = true;
			Location prec = null;
			while (loc != this) {
				final boolean currentVisible = loc.isVisible(mapParams);
				if (currentVisible && !precAdded) {
					arr.add(prec);
				}
				if (lastVisible || currentVisible) {
					arr.add(loc);
					precAdded = true;
				} else {
					precAdded = false;
				}

				prec = loc;
				loc = loc.getNext();
				lastVisible = currentVisible;
			}
			arr.add(this);

			final StringBuilder sb = new StringBuilder();
			boolean first = true;
			for (Location l : Location.interpolateLocations(arr, limit)) {
				if (first) {
					first = false;
				} else {
					sb.append('|');
				}
				sb.append(l.getGoogleApiLatLon());
			}
			return sb.toString();
		}
	}


	@Contract(pure = true)
	public static double getMetersPerSecSpeed(@NotNull Location l1, @NotNull Location l2) {
		return LatLng.getMetersDistance(l1.latLng, l2.latLng) / (getTimeDifference(l1, l2).toMillis() / 1000d);
	}

	@Contract(pure = true)
	@NotNull
	public static Duration getTimeDifference(@NotNull Location l1, @NotNull Location l2) {
		return Duration.between(l1.timestamp, l2.timestamp);
	}

	@Contract(pure = true)
	public static double getKmPerHourSpeed(@NotNull Location l1, @NotNull Location l2) {
		return getMetersPerSecSpeed(l1, l2) * 3.6;
	}

	@Contract(pure = true)
	public boolean hasPrevious() {
		return previous != null;
	}

	@Contract(pure = true)
	@Nullable
	public Location optPrevious() {
		return previous;
	}

	@Contract(pure = true)
	@NotNull
	public Location getPrevious() {
		if (previous == null) {
			throw new IllegalStateException("no previous");
		}
		return previous;
	}

	@Contract(pure = true)
	@NotNull
	public Location getNext() {
		if (next == null) {
			throw new IllegalStateException("no next");
		}
		return next;
	}

	@Contract(pure = true)
	@Nullable
	public Location optNext() {
		return previous;
	}

	@Contract(pure = true)
	public boolean hasNext() {
		return next != null;
	}

	/**
	 * Interpolates adjusting the balancing the balancing to make the interpolation result int the given timesamp
	 *
	 * @param timestamp the desired timestamp. Must be after a.timestamp and before b.timestamp
	 * @see Location#interpolate(Location, Location, float)
	 */
	@NotNull
	public static Location interpolateWithTimestamp(@NotNull Location a, @NotNull Location b, @NotNull Instant timestamp) {
		if (timestamp.compareTo(a.timestamp) < 0) {
			throw new IllegalArgumentException("timestamp is not after a");
		} else if (timestamp.compareTo(b.timestamp) > 0) {
			throw new IllegalArgumentException("timestamp is not before b");
		}
		return interpolate(a, b, Duration.between(a.timestamp, timestamp).toNanos() / (float) Duration.between(a.timestamp, b.timestamp).toNanos());
	}

	@NotNull
	public static TreeSet<Location> interpolateLocations(@NotNull TreeSet<Location> locations, int limit) {
		if (locations.size() < 2 || locations.size() <= limit) {
			return locations;
		} else {
			final Duration span = Duration.between(locations.first().timestamp, locations.last().timestamp).dividedBy(limit);

			final TreeSet<Location> ret = new TreeSet<>();
			Iterator<Location> it = locations.iterator();
			Location loc = it.next();
			Location next = it.next();
			Instant timestamp = loc.timestamp.plus(span);
			ret.add(loc);
			while (it.hasNext()) {
				while (it.hasNext() && !next.timestamp.isAfter(timestamp)) {
					loc = next;
					next = it.next();
				}
				ret.add(Location.interpolateWithTimestamp(loc, next, timestamp));
				timestamp = timestamp.plus(span);
			}
			return ret;
		}
	}

	/**
	 * Defaults <code>balancing</code> to <code>0.5</code>
	 *
	 * @see Location#interpolate(Location, Location, float)
	 */
	@NotNull
	public static Location interpolate(@NotNull Location a, @NotNull Location b) {
		return interpolate(a, b, 0.5f);
	}

	/**
	 * Creates a new {@link Location} interpolating the two given values. Optional values are interpolated only if both {@link Location}s have the values.
	 *
	 * @param a         the first location. Timestamp must be before than <code>b</code>.
	 * @param b         the second location. Timestamp must be after <code>a</code>.
	 * @param balancing the balancing of the interpolation. Must be > 0 and < 1.
	 * @return a new {@link Location} that is the result of the interpolation. The returned location will be an orphan (<code>next=null && previous=null</code>)
	 * @throws IllegalArgumentException if balancing is <=0 || >=1
	 * @throws IllegalArgumentException if a.timestamp is not before b.timestamp
	 */
	@NotNull
	public static Location interpolate(@NotNull Location a, @NotNull Location b, float balancing) {
		if (balancing == 0) {
			return a;
		} else if (balancing == 1) {
			return b;
		} else if (balancing < 0 || balancing > 1) {
			throw new IllegalArgumentException("Invalid balancing: " + balancing);
		} else if (a.compareTo(b) >= 0) {
			throw new IllegalArgumentException("a.timestamp is not before b.timestamp");
		}
		final Instant timestamp = a.timestamp.plus(Duration.ofNanos(Math.round(Duration.between(a.timestamp, b.timestamp).toNanos() * (double) balancing)));
		final int accuracy = Math.round(a.accuracy + (b.accuracy - a.accuracy) * balancing);
		final Integer altitude;
		if (a.altitude != null && b.altitude != null) {
			altitude = Math.round(a.altitude + (b.altitude - a.altitude) * balancing);
		} else {
			altitude = null;
		}
		final Integer heading;
		if (a.heading != null && b.heading != null) {
			heading = Math.round(Math.min(a.heading, b.heading) + Utils.degreesDistance(a.heading, b.heading) * balancing);
		} else {
			heading = null;
		}
		return new Location(timestamp, LatLng.interpolate(a.latLng, b.latLng, balancing), accuracy, altitude, heading);
	}

	/**
	 * @return 1 + the size of next
	 */
	@Contract(pure = true)
	public int size() {
		Location location = next;
		int size = 1;
		while (location != null) {
			size++;
			location = location.next;
		}
		return size;
	}


	/**
	 * Class that helps building a location
	 */
	public static class Builder {
		private Instant timestamp;
		private Double latitude, longitude;
		private Integer accuracy, altitude, heading;

		public void setTimestamp(Instant timestamp) {
			this.timestamp = timestamp;
		}

		public void setLatitude(double latitude) {
			this.latitude = latitude;
		}

		public void setLongitude(double longitude) {
			this.longitude = longitude;
		}

		public void setAccuracy(Integer accuracy) {
			this.accuracy = accuracy;
		}

		public void setAltitude(Integer altitude) {
			this.altitude = altitude;
		}

		public void setHeading(Integer heading) {
			this.heading = heading;
		}

		/**
		 * @return <code>true</code> if all the attributes necessary to construct the {@link Location} have been set
		 */
		public boolean isBuildable() {
			return timestamp != null && latitude != null && longitude != null && accuracy != null;
		}

		/**
		 * Builds the {@link Location}
		 *
		 * @return a new {@link Location} instance
		 * @throws IllegalStateException if a necessary attribute hasn't been set
		 */
		public Location build() {
			if (timestamp == null) {
				throw new IllegalStateException("timestamp not specified");
			} else if (latitude == null) {
				throw new IllegalStateException("latitude not specified");
			} else if (longitude == null) {
				throw new IllegalStateException("longitude not specified");
			} else if (accuracy == null) {
				throw new IllegalStateException("accuracy not specified");
			} else {
				return new Location(timestamp, new LatLng(latitude, longitude), accuracy, altitude, heading);
			}
		}
	}
}
