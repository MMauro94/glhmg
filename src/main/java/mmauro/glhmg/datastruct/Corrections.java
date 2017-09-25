package mmauro.glhmg.datastruct;

import mmauro.glhmg.Utils;
import org.apache.commons.cli.ParseException;
import org.jetbrains.annotations.NotNull;

import java.time.Instant;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class Corrections {

    @NotNull
    private final Set<Instant> instants;

    public Corrections(@NotNull Set<Instant> instants) {
        this.instants = Collections.unmodifiableSet(new HashSet<>(instants));
    }

    @NotNull
    public Set<Instant> getInstants() {
        return instants;
    }

    @NotNull
    public static Corrections parse(@NotNull String str) throws ParseException {
        String[] split = str.split(",");
        HashSet<Instant> instants = new HashSet<>(split.length);
        for (String s : split) {
            instants.add(Utils.parseDateTime(s.trim()));
        }
        return new Corrections(instants);
    }

    public boolean has(@NotNull Instant timestamp) {
        return instants.contains(timestamp);
    }
}
