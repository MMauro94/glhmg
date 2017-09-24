package mmauro.glhmg.datastruct;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.time.Duration;
import java.time.Instant;
import java.util.Iterator;

public final class Locations implements Iterable<Location> {

    private final Location firstLocation;
    private final int size;

    /**
     * Constructs a new instance
     * @param firstLocation the first location
     */
    public Locations(@NotNull Location firstLocation) {
        this.firstLocation = firstLocation;
        this.size = firstLocation.size();
    }

    /**
     * @return the number of locations
     */
    @Contract(pure = true)
    public int size() {
        return size;
    }

    /**
     * Returns a new collection where the first location is the same, and than each subsequent one is always exactly after the specified duration
     *
     * @param duration the duration
     * @return a new {@link Locations}
     */
    @NotNull
    public Locations interpolateWithStaticDuration(@NotNull Duration duration) {
        final Location first = firstLocation.asOrphan();

        Location current = firstLocation;
        Instant newTimestamp = current.getTimestamp();
        Location currentNew = first;
        while (current.hasNext()) {
            newTimestamp = newTimestamp.plus(duration);

            while (current.hasNext() && !newTimestamp.isBefore(current.getNext().getTimestamp())) {
                current = current.getNext();
            }

            if (current.hasNext()) {
                final Location interpolated = Location.interpolateWithTimestamp(currentNew, current.getNext(), newTimestamp);
                currentNew.setNext(interpolated);
                interpolated.setPrevious(currentNew);
                currentNew = interpolated;
            }

        }
        return new Locations(first);
    }

    @NotNull
    @Override
    public Iterator<Location> iterator() {
        return new Iterator<Location>() {

            private Location current = firstLocation;

            @Override
            public boolean hasNext() {
                return current.hasNext();
            }

            @Override
            public Location next() {
                final Location ret = current;
                current = current.getNext();
                return ret;
            }
        };
    }
}
