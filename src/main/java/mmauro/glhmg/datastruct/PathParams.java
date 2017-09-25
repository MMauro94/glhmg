package mmauro.glhmg.datastruct;

import org.jetbrains.annotations.NotNull;

import java.awt.*;

public class PathParams {

    @NotNull
    private final Color pathColor;
    private final int pathWeight;

    public PathParams(@NotNull Color pathColor, int pathWeight) {
        if (pathWeight <= 0) {
            throw new IllegalArgumentException("pathWeight <= 0");
        }
        this.pathColor = pathColor;
        this.pathWeight = pathWeight;
    }

    @NotNull
    public Color getPathColor() {
        return pathColor;
    }

    public int getPathWeight() {
        return pathWeight;
    }
}
