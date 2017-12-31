package com.github.mmauro.glhmg.datastruct;

import org.jetbrains.annotations.NotNull;

public class MapParams {

	@NotNull
	private final Location location;
	@NotNull
	private final MapSize size;
	private final int zoom;
	private final int scale;

	public MapParams(@NotNull Location location, @NotNull MapSize size, int zoom, int scale) {
		this.location = location;
		if (zoom < 0) {
			throw new IllegalArgumentException("zoom < 0");
		} else if (scale <= 0) {
			throw new IllegalArgumentException("scale <= 0");
		}
		this.size = size;
		this.zoom = zoom;
		this.scale = scale;
	}

	@NotNull
	public Location getLocation() {
		return location;
	}

	@NotNull
	public MapSize getSize() {
		return size;
	}

	public int getZoom() {
		return zoom;
	}

	public int getScale() {
		return scale;
	}
}
