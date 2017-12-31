package com.github.mmauro.glhmg.datastruct;

public class Holder<T> {

	private final T value;

	public Holder(T value) {
		this.value = value;
	}

	public T getValue() {
		return value;
	}
}
