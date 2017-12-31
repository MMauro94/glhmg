package com.github.mmauro.glhmg.datastruct;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

public class WorldCoordinate {

	private final double x, y;

	public WorldCoordinate(double x, double y) {
		this.x = x;
		this.y = y;
	}

	public double getX() {
		return x;
	}

	public double getY() {
		return y;
	}

	@Contract(pure = true)
	public boolean isInBounds(@NotNull WorldCoordinate ne, @NotNull WorldCoordinate sw) {
		return x >= sw.x && x <= ne.x && y >= ne.y && y <= sw.y;
	}
}
