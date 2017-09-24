package mmauro.glhmg.datastruct;

import mmauro.glhmg.Utils;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;

/**
 * Created by Mauro on 28/08/2017.
 */
public final class Location implements Comparable<Location> {

    @Nullable
    private Location previous, next;
    @NotNull
    private final Instant timestamp;
    @NotNull
    private final BigDecimal latitude, longitude;
    private final int accuracy;
    @Nullable
    private final Integer altitude, heading;

    private Location(@NotNull Instant timestamp, @NotNull BigDecimal latitude, @NotNull BigDecimal longitude, int accuracy, @Nullable Integer altitude, @Nullable Integer heading) {
        if (accuracy < 0) {
            throw new IllegalArgumentException("accuracy < 0: " + accuracy);
        } else if (heading != null && heading < 0) {
            throw new IllegalArgumentException("heading < 0: " + heading);
        } else if (heading != null && heading >= 360) {
            throw new IllegalArgumentException("heading >= 360: " + heading);
        }
        this.timestamp = timestamp;
        this.latitude = latitude;
        this.longitude = longitude;
        this.accuracy = accuracy;
        this.altitude = altitude;
        this.heading = heading;
    }

    @Contract(pure = true)
    @NotNull
    public Instant getTimestamp() {
        return timestamp;
    }

    @Contract(pure = true)
    @NotNull
    public BigDecimal getLatitude() {
        return latitude;
    }

    @Contract(pure = true)
    @NotNull
    public BigDecimal getLongitude() {
        return longitude;
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
        return latitude.toPlainString() + "," + longitude.toPlainString();
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
    public Location asOrphan() {
        return new Location(timestamp, latitude, longitude, accuracy, altitude, heading);
    }

    @NotNull
    @Contract(pure = true)
    @Override
    public String toString() {
        return "Location{" +
                "timestamp=" + timestamp +
                ", latitude=" + latitude +
                ", longitude=" + longitude +
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

    /**
     *
     * @param max the maximum number of nodes
     * @return the path to pass to the Google Static Map APIs
     */
    @NotNull
    public String getGoogleApiPath(int max) {
        if (previous != null && max > 1) {
            return previous.getGoogleApiPath(max - 1) + "|" + getGoogleApiLatLon();
        } else {
            return getGoogleApiLatLon();
        }
    }

    /**
     * Calculates th distance, in meters, between two locations
     * @param l1 the first location
     * @param l2 the second location
     * @return the number of meters separating the two locations
     */
    @Contract(pure = true)
    public static double getMetersDistance(@NotNull Location l1, @NotNull Location l2) {
        final double lat1 = l1.latitude.doubleValue();
        final double lat2 = l2.latitude.doubleValue();
        final double lon1 = l1.longitude.doubleValue();
        final double lon2 = l2.longitude.doubleValue();

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


    @Contract(pure = true)
    public static double getMetersPerSecSpeed(@NotNull Location l1, @NotNull Location l2) {
        return getMetersDistance(l1, l2) / (getTimeDifference(l1, l2).toMillis() / 1000d);
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
        if (!timestamp.isAfter(a.timestamp)) {
            throw new IllegalArgumentException("timestamp is not after a");
        } else if (!timestamp.isBefore(b.timestamp)) {
            throw new IllegalArgumentException("timestamp is not before b");
        }
        return interpolate(a, b, Duration.between(a.timestamp, timestamp).toNanos() / (float) Duration.between(a.timestamp, b.timestamp).toNanos());
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
        if (balancing <= 0 || balancing >= 1) {
            throw new IllegalArgumentException("Invalid balancing: " + balancing);
        } else if (a.compareTo(b) >= 0) {
            throw new IllegalArgumentException("a.timestamp is not before b.timestamp");
        }
        final Instant timestamp = a.timestamp.plus(Duration.ofNanos(Math.round(Duration.between(a.timestamp, b.timestamp).toNanos() * (double) balancing)));
        final BigDecimal latitude = a.latitude.add(b.latitude.subtract(a.latitude).multiply(BigDecimal.valueOf(balancing))).setScale(7, BigDecimal.ROUND_HALF_UP);
        final BigDecimal longitude = a.longitude.add(b.longitude.subtract(a.longitude).multiply(BigDecimal.valueOf(balancing))).setScale(7, BigDecimal.ROUND_HALF_UP);
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
        return new Location(timestamp, latitude, longitude, accuracy, altitude, heading);
    }

    /**
     * @return 1 + the size of next
     */
    @Contract(pure = true)
    public int size() {
        Location location = next;
        int size = 1;
        while(location != null) {
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
        private BigDecimal latitude, longitude;
        private Integer accuracy, altitude, heading;

        public void setTimestamp(Instant timestamp) {
            this.timestamp = timestamp;
        }

        public void setLatitude(BigDecimal latitude) {
            this.latitude = latitude;
        }

        public void setLongitude(BigDecimal longitude) {
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
                return new Location(timestamp, latitude, longitude, accuracy, altitude, heading);
            }
        }
    }
}
