package com.github.mmauro.glhmg.datastruct;

import org.apache.commons.cli.ParseException;
import org.jetbrains.annotations.NotNull;

import java.awt.Dimension;

public class MapSize extends Dimension {

	public MapSize(int width, int height) {
		super(width, height);
		if (width < 0) {
			throw new IllegalArgumentException("width < 0");
		} else if (height < 0) {
			throw new IllegalArgumentException("height < 0");
		}
	}

	@NotNull
	public static MapSize parse(@NotNull String str) throws ParseException {
		String[] split = str.split("x");
		if (split.length != 2) {
			throw new ParseException("Invalid dimension: " + str);
		} else {
			try {
				return new MapSize(Integer.parseInt(split[0]), Integer.parseInt(split[1]));
			} catch (NumberFormatException ex) {
				throw new ParseException("Invalid number in dimension: " + ex.getMessage());
			}
		}
	}

	@Override
	public String toString() {
		return (int) getWidth() + "x" + (int) getHeight();
	}
}
