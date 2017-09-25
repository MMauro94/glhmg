package mmauro.glhmg.parse;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import mmauro.glhmg.OutUtils;
import mmauro.glhmg.datastruct.Location;
import mmauro.glhmg.datastruct.Locations;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Collection;
import java.util.Optional;
import java.util.TreeSet;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Parser for the Google locations JSON
 */
public class LocationsParser {

    /**
     * Divisor of latitude and longitude ints
     */
    private static final double LAT_LON_DIVISOR = 10000000d;

    @NotNull
    private final JsonParser jsonParser;
    private boolean setUp = false;
    private boolean end = false;
    private int builtLocations = 0;


    /**
     * Constructs the parser
     *
     * @param jsonParser an instance of {@link JsonParser} to use to parse the JSON
     */
    public LocationsParser(@NotNull JsonParser jsonParser) {
        this.jsonParser = jsonParser;
    }

    /**
     * @throws ParseException if <code>foundToken</code> is different from <code>expectedToken</code>
     */
    private void expect(@Nullable JsonToken foundToken, @NotNull JsonToken expectedToken) throws IOException, ParseException {
        if (foundToken != expectedToken) {
            throw new ParseException("Expected " + expectedToken + ", found " + foundToken);
        }
    }

    /**
     * @throws ParseException if the current token of the internal parser is not <code>expectedToken</code>
     */
    private void currentExpect(@NotNull JsonToken expectedToken) throws IOException, ParseException {
        expect(jsonParser.currentToken(), expectedToken);
    }

    /**
     * Advances the parser to the next token and tests it
     *
     * @throws ParseException if the next token of the internal parser is not <code>expectedToken</code>
     */
    private void nextExpect(@NotNull JsonToken expectedToken) throws IOException, ParseException {
        expect(jsonParser.nextToken(), expectedToken);
    }

    /**
     * Skips an entire value. The position of the parser at the end of the method is the struct end of the skipped struct value or the skipped scalar value
     *
     * @throws ParseException if the current token of the internal parser is not a struct start or a scalar value
     */
    private void skipChildrenOrValue() throws IOException, ParseException {
        final JsonToken currentToken = jsonParser.currentToken();
        if (currentToken.isStructStart()) {
            jsonParser.skipChildren();
        } else if (!currentToken.isScalarValue()) {
            throw new ParseException("Unexpected token " + currentToken);
        }
    }

    /**
     * Positions itself at the {@link JsonToken#START_ARRAY} of the <code>locations</code> value
     * To be only called when the parser is at the beginning of the JSON.
     */
    private void setUp() throws IOException, ParseException {
        nextExpect(JsonToken.START_OBJECT);

        while (true) {
            nextExpect(JsonToken.FIELD_NAME);

            switch (jsonParser.getText()) {
                case "locations":
                    nextExpect(JsonToken.START_ARRAY);
                    return;
                default:
                    jsonParser.nextToken();
                    skipChildrenOrValue();
            }
        }
    }

    /**
     * Obtains the next location from the JSON
     *
     * @return the next {@link Location} or <code>null</code> there are no more locations
     */
    @Nullable
    private Location next() throws IOException, ParseException {
        if (!setUp) {
            setUp = true;
            setUp();
        } else if (end) {
            return null;
        }

        while (true) {
            final Location.Builder builder = new Location.Builder();

            jsonParser.nextToken();
            if (jsonParser.currentToken() == JsonToken.END_ARRAY) {
                end = true;
                return null;
            } else {
                currentExpect(JsonToken.START_OBJECT);
            }
            while (jsonParser.nextToken() != JsonToken.END_OBJECT) {
                currentExpect(JsonToken.FIELD_NAME);

                final String fieldName = jsonParser.getText();
                jsonParser.nextToken();
                switch (fieldName) {
                    case "timestampMs":
                        if (jsonParser.currentToken() == JsonToken.VALUE_STRING) {
                            builder.setTimestamp(Instant.ofEpochMilli(Long.parseLong(jsonParser.getText())));
                        } else if (jsonParser.currentToken() == JsonToken.VALUE_NUMBER_INT) {
                            builder.setTimestamp(Instant.ofEpochMilli(jsonParser.getLongValue()));
                        } else {
                            throw new ParseException("Unexpected token " + jsonParser.currentToken() + " for timestamp value");
                        }
                        break;
                    case "latitudeE7":
                        if (jsonParser.currentToken() == JsonToken.VALUE_NUMBER_INT) {
                            builder.setLatitude(jsonParser.getLongValue() / LAT_LON_DIVISOR);
                        } else {
                            throw new ParseException("Unexpected token " + jsonParser.currentToken() + " for latitudeE7 value");
                        }
                        break;
                    case "longitudeE7":
                        if (jsonParser.currentToken() == JsonToken.VALUE_NUMBER_INT) {
                            builder.setLongitude(jsonParser.getLongValue() / LAT_LON_DIVISOR);
                        } else {
                            throw new ParseException("Unexpected token " + jsonParser.currentToken() + " for longitudeE7 value");
                        }
                        break;
                    case "accuracy":
                        if (jsonParser.currentToken() == JsonToken.VALUE_NUMBER_INT) {
                            builder.setAccuracy(jsonParser.getIntValue());
                        } else {
                            throw new ParseException("Unexpected token " + jsonParser.currentToken() + " for accuracy value");
                        }
                        break;
                    case "altitude":
                        if (jsonParser.currentToken() == JsonToken.VALUE_NUMBER_INT) {
                            builder.setAltitude(jsonParser.getIntValue());
                        } else {
                            throw new ParseException("Unexpected token " + jsonParser.currentToken() + " for altitude value");
                        }
                        break;
                    case "heading":
                        if (jsonParser.currentToken() == JsonToken.VALUE_NUMBER_INT) {
                            int heading = jsonParser.getIntValue();
                            if (heading >= 0)
                                builder.setHeading(heading);
                        } else {
                            throw new ParseException("Unexpected token " + jsonParser.currentToken() + " for heading value");
                        }
                        break;
                    default:
                        skipChildrenOrValue();
                }
            }
            if (builder.isBuildable()) {
                builtLocations++;
                if (builtLocations % 100000 == 0) {
                    OutUtils.verbose(builtLocations / 1000 + "K locations parsed");
                }
                return builder.build();
            }
        }
    }

    /**
     * Parses all the locations, orders them, filters them and returns them
     *
     * @return a new {@link Locations} instance, or <code>null</code> if the JSON doesn't contain locations
     * @throws IOException    if an error occurs reading the JSON
     * @throws ParseException if an error occurs parsing the JSON
     */
    @Nullable
    public Locations getLocations(@Nullable Predicate<Location> filter) throws IOException, ParseException {
        TreeSet<Location> treeSet = new TreeSet<>();
        Location next;
        while ((next = next()) != null) {
            treeSet.add(next);
        }
        Location previous = null;
        for (Location location : treeSet) {
            if (previous != null) {
                previous.setNext(location);
            }
            location.setPrevious(previous);
            previous = location;
        }
        if (filter != null) {
            treeSet = treeSet.stream().filter(filter).collect(Collectors.toCollection(TreeSet::new));
        }
        if (treeSet.isEmpty()) {
            return null;
        } else {
            Location first = treeSet.first();
            first.setPrevious(null);
            treeSet.last().setNext(null);
            return new Locations(first);
        }
    }
}
